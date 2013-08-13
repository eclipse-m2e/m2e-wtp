/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.m2e.wtp.overlay.internal.servers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.m2e.wtp.overlay.OverlayConstants;
import org.eclipse.m2e.wtp.overlay.internal.modulecore.OverlaySelfComponent;
import org.eclipse.m2e.wtp.overlay.modulecore.IOverlayVirtualComponent;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.ModuleCoreNature;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualReference;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.internal.Server;

/**
 * Listens to overlaid project changes to force server redeployment.
 * 
 * @author Fred Bricon
 *
 */
public class OverlayResourceChangeListener implements IResourceChangeListener {

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		IResourceDelta delta =  event.getDelta();
		if (delta == null) {
			return;
		}

		IServer[] servers = ServerCore.getServers();
		if (servers.length == 0) {
			//No servers defined, so nothing to do
			return;
		}
		
		IResourceDelta[] projectDeltas = delta.getAffectedChildren();
		if (projectDeltas == null || projectDeltas.length == 0) {
			return;
		}
	   
		boolean buildOccurred = hasBuildOccurred(event);
	   	
		Set<IProject> changedProjects  = getChangedProjects(projectDeltas);
		if (changedProjects.isEmpty()) {
			return;
		}
		
		Map<IServer, List<IModule>> republishableServers = new HashMap<IServer, List<IModule>>(servers.length);
		
		for (IServer server : servers) {
			modules : for (IModule module : server.getModules()) {
				IProject moduleProject = module.getProject();
				for (IProject changedProject : changedProjects) {
					if (hasOverlayChanged(changedProject, moduleProject, delta)) {
						List<IModule> republishableModules = republishableServers.get(server);
						if (republishableModules == null) {
							republishableModules = new ArrayList<IModule>(server.getModules().length);
							republishableServers.put(server, republishableModules);
						}
						republishableModules.add(module);
						break modules;
					}
				}
			}
		}
		
		if (republishableServers.isEmpty()) {
			return;
		}
		
		boolean isPublishOverlaysEnabled = isPublishOverlaysEnabled();
		for(Map.Entry<IServer, List<IModule>> entries : republishableServers.entrySet()) {
			IServer iserver = entries.getKey();
			boolean shouldPublish = isPublishOverlaysEnabled;
			if (iserver instanceof Server) {
				Server server = ((Server)iserver);
				List<IModule> modules = entries.getValue();
				IModule[] mod = new IModule[modules.size()];
				modules.toArray(mod);
				server.setModulePublishState(mod, IServer.PUBLISH_STATE_INCREMENTAL);
				int autoPublishSetting = server.getAutoPublishSetting(); 
				shouldPublish = shouldPublish && (autoPublishSetting == Server.AUTO_PUBLISH_RESOURCE || 
						                (autoPublishSetting == Server.AUTO_PUBLISH_BUILD && buildOccurred));
			}
			if (shouldPublish && iserver.getServerState() == IServer.STATE_STARTED) {
				iserver.publish(IServer.PUBLISH_INCREMENTAL, new NullProgressMonitor());
			} 
		}
	}

	private boolean isPublishOverlaysEnabled() {
	  boolean isEnabled = new InstanceScope().getNode(OverlayConstants.PLUGIN_ID)
	                      .getBoolean(OverlayConstants.P_REPUBLISH_ON_PROJECT_CHANGE, true);
	  return isEnabled;
	}

	private Set<IProject> getChangedProjects(IResourceDelta[] projectDeltas) {
		Set<IProject> projects = new HashSet<IProject>();
		if (projectDeltas != null) {
			for (IResourceDelta delta : projectDeltas) {
				IResource resource = delta.getResource();
				if (resource != null && resource instanceof IProject) {
					projects.add((IProject) resource);
				}
			}
		}
		return projects;
	}
	
	/**
	 * Return true if moduleProject references changedProject as an IOverlayComponent
	 * @param changedProject
	 * @param projectDeployedOnServer
	 * @param delta 
	 * @return true if moduleProject references changedProject as an IOverlayComponent
	 */
	private boolean hasOverlayChanged(IProject changedProject, IProject projectDeployedOnServer, IResourceDelta delta) {
		if (!ModuleCoreNature.isFlexibleProject(projectDeployedOnServer)) {
			return false; 
		}
		IVirtualComponent component = ComponentCore.createComponent(projectDeployedOnServer);
		if (component == null) {
			return false;
		}
		IVirtualReference[] references = component.getReferences();
		if (references == null || references.length == 0) {
			return false;
		}
		for (IVirtualReference reference : references) {
			IVirtualComponent vc = reference.getReferencedComponent();
			if (vc instanceof IOverlayVirtualComponent){
			  IProject overlaidProject = vc.getProject(); 
			  if (vc instanceof OverlaySelfComponent) {
			    IPath componentFilePath = overlaidProject.getFile(".settings/org.eclipse.wst.common.component").getFullPath(); //$NON-NLS-1$
			    if (delta.findMember(componentFilePath) != null) {
			      return true;
			    }
			  } else if (!vc.isBinary() && overlaidProject.equals(changedProject)){
			    return true;
			  }
			}
		}
		return false;
	}

     private boolean hasBuildOccurred(IResourceChangeEvent event) {
    	 if (event == null) {
    		 return false;
    	 }
         int kind = event.getBuildKind();
         return (kind == IncrementalProjectBuilder.INCREMENTAL_BUILD) ||
                (kind == IncrementalProjectBuilder.FULL_BUILD) ||
                ((kind == IncrementalProjectBuilder.AUTO_BUILD && ResourcesPlugin.getWorkspace().isAutoBuilding()));
     }

}
