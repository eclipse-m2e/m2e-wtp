/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp.internal.build;

import static org.eclipse.m2e.wtp.MavenWtpConstants.WTP_MARKER_FAIL_ON_MISSING_WEBXML_ERROR;

import java.util.Set;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.markers.IMavenMarkerManager;
import org.eclipse.m2e.core.internal.markers.SourceLocation;
import org.eclipse.m2e.core.internal.markers.SourceLocationHelper;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.wtp.WTPProjectsUtil;
import org.eclipse.m2e.wtp.WarPluginConfiguration;
import org.eclipse.m2e.wtp.internal.Messages;
import org.eclipse.m2e.wtp.internal.filtering.ResourceFilteringBuildParticipant;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;

/**
 * WarProjectBuildParticipant
 *
 * @author Fred Bricon
 */
public class WarProjectBuildParticipant extends ResourceFilteringBuildParticipant {

	@SuppressWarnings("restriction")
	@Override
	public Set<IProject> build(int kind, IProgressMonitor monitor) throws Exception {
		super.build(kind, monitor);

		IMavenProjectFacade facade = getMavenProjectFacade();
		IProject project = facade.getProject();
		boolean iswtp = WTPProjectsUtil.isWTPProject(project);
		if (!iswtp) {
			return null;
		}

		final Boolean[] requiresWebXmlCheck = new Boolean[1]; 
		requiresWebXmlCheck[0]=Boolean.FALSE;
		boolean deleteMarkers = true;
		switch(kind) {
			case IncrementalProjectBuilder.CLEAN_BUILD:
				break;
			case IncrementalProjectBuilder.FULL_BUILD:
				requiresWebXmlCheck[0] = Boolean.TRUE;
				break;
			default :
			IResourceDelta delta = getDelta(project);
			if (delta != null) {
				delta.accept(new IResourceDeltaVisitor() {
					@Override
					public boolean visit(IResourceDelta delta) {
						boolean foundInterestingFile = false;
						IResource resource = delta.getResource();
						if (resource instanceof IFile) {
							String name = resource.getName(); 
							if ("web.xml".equals(name)//$NON-NLS-1$
									|| IMavenConstants.POM_FILE_NAME.equals(name)) {
								foundInterestingFile = true;
								requiresWebXmlCheck[0] = Boolean.TRUE;
							}
						}
						return !foundInterestingFile;
					}
				});
			}
			deleteMarkers = requiresWebXmlCheck[0];
		}

		IMavenMarkerManager markerManager  = MavenPluginActivator.getDefault().getMavenMarkerManager();
		IFile pom = facade.getPom();

		if (deleteMarkers && pom.findMarkers(WTP_MARKER_FAIL_ON_MISSING_WEBXML_ERROR, false, IResource.DEPTH_ZERO) != null) {
			markerManager.deleteMarkers(pom, WTP_MARKER_FAIL_ON_MISSING_WEBXML_ERROR);
		}

		if (requiresWebXmlCheck[0]) {
			MavenProject mavenProject = facade.getMavenProject(monitor);
			WarPluginConfiguration warConfig = new WarPluginConfiguration(mavenProject, project);
			if (warConfig.isFailOnMissingWebXml() 
					   && isWebXmlMissing(project)) {
				
				SourceLocation sourceLocation = SourceLocationHelper.findLocation(mavenProject, 
						new MojoExecutionKey("org.apache.maven.plugins", "maven-war-plugin", null, null, null, null)); //$NON-NLS-1$ //$NON-NLS-2$
				markerManager.addMarker(pom, WTP_MARKER_FAIL_ON_MISSING_WEBXML_ERROR, 
                        Messages.WarProjectBuildParticipant_Error_FailOnMissingWebXml, 
                        sourceLocation.getLineNumber(), IMarker.SEVERITY_ERROR);
			}
		}
		return null;
	}

	private boolean isWebXmlMissing(IProject project) {
		IVirtualComponent component = ComponentCore.createComponent(project, true);
	    if(component != null) {      
	      IVirtualFolder rootFolder = component.getRootFolder();
	      return rootFolder.findMember(new Path("WEB-INF/web.xml")) == null; //$NON-NLS-1$
	    }
		return false;
	}

}
