/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp;

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jst.j2ee.project.facet.IJ2EEFacetConstants;
import org.eclipse.m2e.core.project.MavenProjectUtils;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * See http://maven.apache.org/plugins/maven-ejb-plugin/ejb-mojo.html
 * 
 * @author Fred Bricon
 */
class EjbPluginConfiguration {
  
  private static final Logger LOG = LoggerFactory.getLogger(EjbPluginConfiguration.class); 

  /**
   * Maven defaults ejb version to 2.1
   */
  private static final IProjectFacetVersion DEFAULT_EJB_FACET_VERSION = IJ2EEFacetConstants.EJB_21;
  
  final Plugin plugin;
 
  final MavenProject ejbProject;
  
  public EjbPluginConfiguration(MavenProject mavenProject) {

    if (JEEPackaging.EJB != JEEPackaging.getValue(mavenProject.getPackaging()))
      throw new IllegalArgumentException("Maven project must have ejb packaging");
    
    this.ejbProject = mavenProject;
    this.plugin = mavenProject.getPlugin("org.apache.maven.plugins:maven-ejb-plugin");
  }

  /**
   * Gets EJB_FACET version of the project from pom.xml.<br/> 
   * @return  value of &lt;maven-ejb-plugin&gt;&lt;configuration&gt;&lt;ejbVersion&gt;. Default value is 2.1.
   */
  public IProjectFacetVersion getEjbFacetVersion() {
    if (plugin == null){
      return DEFAULT_EJB_FACET_VERSION; 
    }

    Xpp3Dom dom = (Xpp3Dom) plugin.getConfiguration();
    if (dom == null) {
      return DEFAULT_EJB_FACET_VERSION; 
    }
    
    String ejbVersion = DomUtils.getChildValue(dom, "ejbVersion");
    if (ejbVersion != null) {
      try {
        return WTPProjectsUtil.EJB_FACET.getVersion(ejbVersion);
      } catch (Exception e) {
        LOG.warn(e.getMessage());
        //If ejbVersion > 3.0 and WTP < 3.2, then downgrade to ejb facet 3.0
        if (ejbVersion.startsWith("3.")){
          return IJ2EEFacetConstants.EJB_30;
        }
      }
    }
    return DEFAULT_EJB_FACET_VERSION; 
  }
  
  /**
   * @return the first resource location directory declared in pom.xml
   */
  public String getEjbContentDirectory(IProject project) {
    IPath[] resources = MavenProjectUtils.getResourceLocations(project, ejbProject.getResources());
    return resources[0].toPortableString();
  }
  
}
