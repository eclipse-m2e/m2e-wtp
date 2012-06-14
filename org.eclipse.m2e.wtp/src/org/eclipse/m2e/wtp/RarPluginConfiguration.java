/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp;

import java.io.IOException;
import java.io.InputStream;

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jst.j2ee.internal.J2EEVersionConstants;
import org.eclipse.jst.j2ee.project.facet.IJ2EEFacetConstants;
import org.eclipse.jst.jee.util.internal.JavaEEQuickPeek;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;

/**
 * RarPluginConfiguration
 *
 * @author Fred Bricon
 */
public class RarPluginConfiguration {

  private static final String RAR_DEFAULT_CONTENT_DIR = "src/main/rar"; 

  private static final String RA_XML = "META-INF/ra.xml";

  private static final int JCA_1_6_ID = 16;//Exists in WTP >= 3.2 only

  final Plugin plugin;
  
  final MavenProject rarProject;
  
  
  public RarPluginConfiguration(MavenProject mavenProject) {

    if (JEEPackaging.RAR != JEEPackaging.getValue(mavenProject.getPackaging()))
      throw new IllegalArgumentException("Maven project must have rar packaging");
    
    this.rarProject = mavenProject;
    this.plugin = mavenProject.getPlugin("org.apache.maven.plugins:maven-rar-plugin");
  }

  /**
   * @return rar plugin configuration or null.
   */
  private Xpp3Dom getConfiguration() {
    if(plugin == null) {
      return null;
    }
    return (Xpp3Dom) plugin.getConfiguration();
  }

  /**
   * Should project classes be included in the resulting RAR?
   * @return the value of "includeJar". Default is true;
   */
  public boolean isJarIncluded() {
    Xpp3Dom config = getConfiguration();
    if(config != null) {
      Xpp3Dom includeJarDom = config.getChild("includeJar");
      if (includeJarDom != null) {
        return Boolean.parseBoolean(includeJarDom.getValue().trim());
      }
    }
    return true; 
  }
  
  
  /**
   * Gets the rar content directory of the project from pom.xml configuration.
   * 
   * @return the first resource directory found in pom.xml.
   */
  public String getRarContentDirectory(IProject project) {
    Xpp3Dom config = getConfiguration();
    if(config != null) {
      Xpp3Dom contentDirDom = config.getChild("rarSourceDirectory");
      if(contentDirDom != null && contentDirDom.getValue() != null) {
        String contentDir = contentDirDom.getValue().trim();
        contentDir = ProjectUtils.getRelativePath(project, contentDir);
        contentDir = (contentDir.length() == 0) ? RAR_DEFAULT_CONTENT_DIR : contentDir;
        return contentDir;
      }
    }

    return RAR_DEFAULT_CONTENT_DIR;
  }

  /**
   * @return
   */
  public IProjectFacetVersion getConnectorFacetVersion(IProject project) {

      IFile raXml = project.getFolder(getRarContentDirectory(project)).getFile(RA_XML);
      if(raXml.isAccessible()) {
        try {
          InputStream is = raXml.getContents();
          try {
            JavaEEQuickPeek jqp = new JavaEEQuickPeek(is);
            switch(jqp.getVersion()) {
              case J2EEVersionConstants.JCA_1_0_ID:
                return IJ2EEFacetConstants.JCA_10;
              case J2EEVersionConstants.JCA_1_5_ID:
                return IJ2EEFacetConstants.JCA_15;
              case JCA_1_6_ID:
                //Don't create a static 1.6 facet version, it'd blow up WTP < 3.2
                return IJ2EEFacetConstants.JCA_FACET.getVersion("1.6");//only exists in WTP version >= 3.2
            }
          } finally {
            is.close();
          }
        } catch(IOException ex) {
          // expected
        } catch(CoreException ex) {
          // expected
        }
      }
  

      //If no ra.xml found and the project depends and WTP >= 3.2, then set connector facet to 1.6
      //TODO see if other conditions might apply to differentiate JCA 1.6 from 1.5
      return IJ2EEFacetConstants.JCA_FACET.getVersion("1.6");
    }

  /**
   * Get the custom location of ra.xml, as set in &lt;raXmlFile&gt;.
   * @return the custom location of ra.xml or null if &lt;raXmlFile&gt; is not set
   */
  public String getCustomRaXml(IProject project) {
    Xpp3Dom config = getConfiguration();
    if(config != null) {
      Xpp3Dom raXmlFileDom = config.getChild("raXmlFile");
      if(raXmlFileDom != null && raXmlFileDom.getValue() != null) {
        String raXmlFile = raXmlFileDom.getValue().trim();
        raXmlFile = ProjectUtils.getRelativePath(project, raXmlFile);
        return raXmlFile;
      }
    }
    return null;
  }

}
