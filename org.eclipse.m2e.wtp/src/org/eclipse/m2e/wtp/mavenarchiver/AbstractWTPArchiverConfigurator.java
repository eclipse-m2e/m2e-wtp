/*******************************************************************************
 * Copyright (c) 2008-2014 Sonatype, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.wtp.mavenarchiver;

import org.apache.maven.plugin.MojoExecution;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.ILifecycleMappingConfiguration;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.eclipse.m2e.mavenarchiver.internal.JarArchiverConfigurator;
import org.eclipse.m2e.wtp.WTPProjectsUtil;
import org.eclipse.wst.common.componentcore.ModuleCoreNature;

/**
 * Base Maven Archiver configurator for WTP projects
 * 
 * @author Fred Bricon
 */
public class AbstractWTPArchiverConfigurator extends JarArchiverConfigurator {

	@Override
	public void configure(ProjectConfigurationRequest request,
			IProgressMonitor monitor) throws CoreException {
		if (WTPProjectsUtil.isM2eWtpDisabled(request.getMavenProjectFacade(), monitor)) {
			return;
		}
		super.configure(request, monitor);
	}

	@Override
	public void mavenProjectChanged(MavenProjectChangedEvent event,
			IProgressMonitor monitor) throws CoreException {
		if (WTPProjectsUtil.isM2eWtpDisabled(event.getMavenProject(), monitor)) {
			return;
		}
	    IMavenProjectFacade oldFacade = event.getOldMavenProject();
	    IMavenProjectFacade newFacade = event.getMavenProject();
	    if(oldFacade == null && newFacade == null) {
	      return;
	    }
	    mavenProjectChanged(newFacade, oldFacade, true, monitor);
	}
	
	@Override
	public AbstractBuildParticipant getBuildParticipant(
			IMavenProjectFacade projectFacade, MojoExecution execution,
			IPluginExecutionMetadata executionMetadata) {
		if (WTPProjectsUtil.isM2eWtpDisabled(projectFacade, new NullProgressMonitor())) {
			return null;
		}		
		return super.getBuildParticipant(projectFacade, execution,
				executionMetadata);
	}

	/**
	 * Ensures the project is WTP enabled before generating the manifest.
	 */
	@Override
	protected boolean needsNewManifest(IFile manifest,
			IMavenProjectFacade oldFacade, IMavenProjectFacade newFacade,
			IProgressMonitor monitor) {

		if (!ModuleCoreNature.isFlexibleProject(newFacade.getProject())) {
			return false;
		}
		return super.needsNewManifest(manifest, oldFacade, newFacade, monitor);
	}
	
	@Override
	public boolean hasConfigurationChanged(IMavenProjectFacade newFacade,
			ILifecycleMappingConfiguration oldProjectConfiguration,
			MojoExecutionKey key, IProgressMonitor monitor) {
		return false;
	}
	
}
