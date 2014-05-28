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

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.wtp.AcrPluginConfiguration;
import org.eclipse.m2e.wtp.ProjectUtils;

/**
 * EarResourceFilteringConfiguration
 *
 * @author Fred Bricon
 */
public class AppClientResourceFilteringConfiguration extends AbstractResourceFilteringConfiguration {

  private AcrPluginConfiguration acrPluginConfiguration;
  
  public AppClientResourceFilteringConfiguration(IMavenProjectFacade mavenProjectFacade) throws CoreException {
    super(mavenProjectFacade);
    acrPluginConfiguration = new AcrPluginConfiguration(mavenProjectFacade);
  }

  @Override
public IPath getTargetFolder() {
    String buildOutputDir = mavenProjectFacade.getMavenProject().getBuild().getOutputDirectory();
    String relativeBuildOutputDir = ProjectUtils.getRelativePath(mavenProjectFacade.getProject(), buildOutputDir);
    return new Path(relativeBuildOutputDir);
  }

  @Override
public List<Xpp3Dom> getResources() {
    if (!acrPluginConfiguration.isFilteringDeploymentDescriptorsEnabled()) {
      return null;
    }
    IFile applicationClientXml = acrPluginConfiguration.getApplicationClientXml();
    if (applicationClientXml == null) {
      return null;
    }
    Xpp3Dom resource = new Xpp3Dom("resource"); //$NON-NLS-1$
    Xpp3Dom directory = new Xpp3Dom("directory"); //$NON-NLS-1$
    directory.setValue(applicationClientXml.getParent().getProjectRelativePath().toPortableString());
    resource.addChild(directory);
    Xpp3Dom filter = new Xpp3Dom("filtering"); //$NON-NLS-1$
    filter.setValue(Boolean.TRUE.toString());
    Xpp3Dom includes = new Xpp3Dom("includes"); //$NON-NLS-1$
    Xpp3Dom include = new Xpp3Dom("include"); //$NON-NLS-1$
    include.setValue(applicationClientXml.getName());
    includes.addChild(include);
    resource.addChild(includes);
    Xpp3Dom targetPath = new Xpp3Dom("targetPath"); //$NON-NLS-1$
    targetPath.setValue("META-INF/"); //$NON-NLS-1$
    resource.addChild(targetPath);
    resource.addChild(filter);
    
    return Arrays.asList(resource);
  }

}
