/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.m2e.wtp.overlay.internal.servers;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
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

/**
 * Listens to overlaid project changes to force server redeployment.
 * 
 * @author Fred Bricon
 *
 */
public class OverlayResourceChangeListener implements IResourceChangeListener {

	public void resourceChanged(IResourceChangeEvent event) {
		
	  if (!isEnabled()) {
	    return;
	  }  
	  
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
		
		Set<IProject> changedProjects  = getChangedProjects(projectDeltas);
		if (changedProjects.isEmpty()) {
			return;
		}
		
		Set<IServer> republishableServers = new HashSet<IServer>(servers.length);
		
		for (IServer server : servers) {
			modules : for (IModule module : server.getModules()) {
				IProject moduleProject = module.getProject();
				for (IProject changedProject : changedProjects) {
					if (hasOverlayChanged(changedProject, moduleProject, delta)) {
						republishableServers.add(server);
						break modules;
					}
				}
			}
		}
		
		for(IServer server : republishableServers) {
			//TODO Publish more elegantly (check server status ...)
			server.publish(IServer.PUBLISH_INCREMENTAL, new NullProgressMonitor());
		}
	}

	private boolean isEnabled() {
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


}
