/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp.filtering;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.wtp.MavenWtpConstants;
import org.eclipse.m2e.wtp.ProjectUtils;
import org.eclipse.m2e.wtp.WarPluginConfiguration;

/**
 * WebResourceFilteringConfiguration
 *
 * @author Fred Bricon
 */
public class WebResourceFilteringConfiguration extends AbstractResourceFilteringConfiguration {

  private static final String WEB_INF = "WEB-INF/";

  private WarPluginConfiguration warPluginConfiguration;
  
  public WebResourceFilteringConfiguration(IMavenProjectFacade mavenProjectFacade) {
    super(mavenProjectFacade);
    warPluginConfiguration = new WarPluginConfiguration(mavenProjectFacade.getMavenProject(), mavenProjectFacade.getProject());
    pluginConfiguration = warPluginConfiguration;
  }

  public IPath getTargetFolder() {
    return getTargetFolder(mavenProjectFacade.getMavenProject(), mavenProjectFacade.getProject());
  }

  public static IPath getTargetFolder(MavenProject mavenProject, IProject project) {
    return ProjectUtils.getM2eclipseWtpFolder(mavenProject, project).append(MavenWtpConstants.WEB_RESOURCES_FOLDER);
  }

  public List<Xpp3Dom> getResources() {
    Xpp3Dom[] domResources = warPluginConfiguration.getWebResources();
    List<Xpp3Dom> resources = new ArrayList<Xpp3Dom>();
    
    if(domResources != null && domResources.length > 0){
      resources.addAll(Arrays.asList(domResources));
    }    

    Xpp3Dom webXmlResource = getWebXmlResource();
    if (webXmlResource != null) {
      resources.add(webXmlResource);
    }

    return resources;
  }

  //MECLIPSEWTP-159 : Handle web.xml filtering with <filteringDeploymentDescriptors> 
  private Xpp3Dom getWebXmlResource() {
    if (!pluginConfiguration.isFilteringDeploymentDescriptorsEnabled()) {
      return null;
    }
    String warSourceDirectory = warPluginConfiguration.getWarSourceDirectory();
    if (warSourceDirectory.startsWith("/")) {
      warSourceDirectory = warSourceDirectory.substring(1);
    }
    if (!warSourceDirectory.endsWith("/")) {
      warSourceDirectory = warSourceDirectory + "/";
    }
    Xpp3Dom resource = new Xpp3Dom("resource");
    Xpp3Dom directory = new Xpp3Dom("directory");
    directory.setValue(warSourceDirectory+WEB_INF);
    resource.addChild(directory);
    Xpp3Dom includes = new Xpp3Dom("includes");
    Xpp3Dom include = new Xpp3Dom("include");
    //TODO handle custom web.xml
    include.setValue("web.xml");
    includes.addChild(include);
    resource.addChild(includes);
    Xpp3Dom filter = new Xpp3Dom("filtering");
    filter.setValue(Boolean.TRUE.toString());
    Xpp3Dom targetPath = new Xpp3Dom("targetPath");
    targetPath.setValue(WEB_INF);
    resource.addChild(targetPath);
    resource.addChild(filter);
    return resource;
  }

  
}
