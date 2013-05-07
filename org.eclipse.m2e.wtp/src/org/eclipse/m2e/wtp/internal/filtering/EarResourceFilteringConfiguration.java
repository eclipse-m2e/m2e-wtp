/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp.internal.filtering;

import java.util.Arrays;
import java.util.List;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.wtp.EarPluginConfiguration;
import org.eclipse.m2e.wtp.MavenWtpConstants;
import org.eclipse.m2e.wtp.ProjectUtils;

/**
 * EarResourceFilteringConfiguration
 *
 * @author Fred Bricon
 */
public class EarResourceFilteringConfiguration extends AbstractResourceFilteringConfiguration {

  private EarPluginConfiguration earPluginConfiguration;
  
  public EarResourceFilteringConfiguration(IMavenProjectFacade mavenProjectFacade) {
    super(mavenProjectFacade);
    earPluginConfiguration = new EarPluginConfiguration(mavenProjectFacade.getMavenProject());
    pluginConfiguration = earPluginConfiguration;
  }

  @Override
public IPath getTargetFolder() {
    return getTargetFolder(mavenProjectFacade.getMavenProject(), mavenProjectFacade.getProject());
  }

  public static IPath getTargetFolder(MavenProject mavenProject, IProject project) {
    return ProjectUtils.getM2eclipseWtpFolder(mavenProject, project).append(MavenWtpConstants.EAR_RESOURCES_FOLDER);
  }

  @Override
public List<Xpp3Dom> getResources() {
    if (!earPluginConfiguration.isFilteringDeploymentDescriptorsEnabled()) {
      return null;
    }
    String earContentDir = earPluginConfiguration.getEarContentDirectory(mavenProjectFacade.getProject());
    Xpp3Dom resource = new Xpp3Dom("resource"); //$NON-NLS-1$
    Xpp3Dom directory = new Xpp3Dom("directory"); //$NON-NLS-1$
    directory.setValue(earContentDir);
    resource.addChild(directory);
    Xpp3Dom filter = new Xpp3Dom("filtering"); //$NON-NLS-1$
    filter.setValue(Boolean.TRUE.toString());
    resource.addChild(filter);
    
    return Arrays.asList(resource);
  }

}
