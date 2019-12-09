/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.wtp.internal.filtering;


import java.util.Set;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.wtp.DeploymentDescriptorManagement;
import org.eclipse.m2e.wtp.EarPluginConfiguration;
import org.eclipse.m2e.wtp.MavenWtpConstants;
import org.eclipse.m2e.wtp.MavenWtpPlugin;
import org.eclipse.m2e.wtp.ProjectUtils;
import org.eclipse.m2e.wtp.preferences.IMavenWtpPreferencesManager;

/**
 * EAR Resource build participant, filters EAR resources and generates application.xml if it's missing.
 *
 * @provisional This class has been added as part of a work in progress. 
 * It is not guaranteed to work or remain the same in future releases. 
 * For more information contact <a href="mailto:m2e-wtp-dev@eclipse.org">m2e-wtp-dev@eclipse.org</a>.
 * 
 * @author Fred Bricon
 * 
 * @since 1.0.1
 */
public class EarResourceBuildParticipant extends ResourceFilteringBuildParticipant {
  
  @Override
  public Set<IProject> build(int kind, IProgressMonitor monitor) throws Exception {
	super.build(kind, monitor);
	IMavenProjectFacade facade = getMavenProjectFacade();
    
    IProject project = facade.getProject();
    MavenProject mavenProject = facade.getMavenProject(monitor);
    EarPluginConfiguration config = new EarPluginConfiguration(mavenProject);
    if (!config.isGenerateApplicationXml()) {
    	return null;
    }
    
    IMavenWtpPreferencesManager prefMgr = MavenWtpPlugin.getDefault().getMavenWtpPreferencesManager(); 
    boolean useBuildDirectory = prefMgr.getPreferences(project).isApplicationXmGeneratedInBuildDirectory();
    IFolder earResourcesFolder;
    if (useBuildDirectory) {
      String appResourcesDir = ProjectUtils.getM2eclipseWtpFolder(mavenProject, project).toPortableString()+Path.SEPARATOR+MavenWtpConstants.EAR_RESOURCES_FOLDER;
      earResourcesFolder = project.getFolder(appResourcesDir);
    } else {
      earResourcesFolder = project.getFolder(config.getEarContentDirectory(project));
    }

    if (!earResourcesFolder.getFile("META-INF/application.xml").exists()) { //$NON-NLS-1$
    	DeploymentDescriptorManagement.INSTANCE.updateConfiguration(project, mavenProject, config , useBuildDirectory, monitor);
    }
    return null;
  }
  
}
