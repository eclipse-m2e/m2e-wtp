/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp.internal.mavenarchiver;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.wtp.MavenWtpConstants;
import org.eclipse.m2e.wtp.MavenWtpPlugin;
import org.eclipse.m2e.wtp.ProjectUtils;
import org.eclipse.m2e.wtp.WarPluginConfiguration;
import org.eclipse.m2e.wtp.mavenarchiver.AbstractWTPArchiverConfigurator;

/**
 * WarMavenArchiverConfigurator
 *
 * @author Fred Bricon
 */
public class WarMavenArchiverConfigurator extends AbstractWTPArchiverConfigurator {

  //private static final Logger log = LoggerFactory.getLogger(WarMavenArchiverConfigurator.class);

  @Override
  protected IPath getOutputDir(IMavenProjectFacade facade) {
    IProject project = facade.getProject();
    MavenProject mavenProject = facade.getMavenProject();
    
    WarPluginConfiguration warPluginConfiguration = new WarPluginConfiguration(mavenProject, project);
    
    if (MavenWtpPlugin.getDefault().getMavenWtpPreferencesManager().getPreferences(project).isWebMavenArchiverUsesBuildDirectory()
        || warPluginConfiguration.getWebResources() != null && warPluginConfiguration.getWebResources().length > 0 //Uses filtering
        || warPluginConfiguration.isFilteringDeploymentDescriptorsEnabled()) {

      IPath localResourceFolder =  ProjectUtils.getM2eclipseWtpFolder(mavenProject, project);
      return project.getFullPath().append(localResourceFolder)
                                  .append(MavenWtpConstants.WEB_RESOURCES_FOLDER);
    }
    
    return project.getFolder(warPluginConfiguration.getWarSourceDirectory()).getFullPath();
  }
  
  @Override
  protected String getArchiverFieldName() {
    return "warArchiver"; //$NON-NLS-1$
  }
  
  @Override
  protected MojoExecutionKey getExecutionKey() {
    MojoExecutionKey key = new MojoExecutionKey("org.apache.maven.plugins", "maven-war-plugin", "", "war", null, null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    return key;
  }
}
