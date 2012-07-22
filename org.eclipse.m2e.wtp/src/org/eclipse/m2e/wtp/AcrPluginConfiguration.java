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
import org.eclipse.core.runtime.IPath;
import org.eclipse.jst.j2ee.internal.J2EEVersionConstants;
import org.eclipse.jst.j2ee.project.facet.IJ2EEFacetConstants;
import org.eclipse.jst.jee.util.internal.JavaEEQuickPeek;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectUtils;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;

/**
 * maven-acr-plugin (application client maven plugin) configuration model.
 *
 * @see <a href="http://maven.apache.org/plugins/maven-acr-plugin/acr-mojo.html">http://maven.apache.org/plugins/maven-acr-plugin/acr-mojo.html</a>
 *
 * @provisional This class has been added as part of a work in progress. 
 * It is not guaranteed to work or remain the same in future releases. 
 * For more information contact <a href="mailto:m2e-wtp-dev@eclipse.org">m2e-wtp-dev@eclipse.org</a>.
 * 
 * @author Fred Bricon
 */
public class AcrPluginConfiguration extends AbstractFilteringSupportMavenPlugin {
  
  private static final IProjectFacetVersion DEFAULT_APPCLIENT_FACET_VERSION = IJ2EEFacetConstants.APPLICATION_CLIENT_50;
  
  final IMavenProjectFacade mavenProjectFacade;

  public AcrPluginConfiguration(IMavenProjectFacade facade) {

    MavenProject mavenProject = facade.getMavenProject();
    if (JEEPackaging.APP_CLIENT != JEEPackaging.getValue(mavenProject.getPackaging()))
      throw new IllegalArgumentException("Maven project must have app-client packaging");
    
    this.mavenProjectFacade = facade;
    Plugin plugin = mavenProject.getPlugin("org.apache.maven.plugins:maven-acr-plugin");
    if (plugin != null) {
      setConfiguration((Xpp3Dom) plugin.getConfiguration()); 
    }
  }

  public IProjectFacetVersion getFacetVersion() {
    IFile applicationClientXml = getApplicationClientXml();

    if(applicationClientXml != null && applicationClientXml.isAccessible()) {
      try {
        InputStream is = applicationClientXml.getContents();
        try {
          JavaEEQuickPeek jqp = new JavaEEQuickPeek(is);
          switch(jqp.getVersion()) {
            case J2EEVersionConstants.J2EE_1_2_ID:
              return IJ2EEFacetConstants.APPLICATION_CLIENT_12;
            case J2EEVersionConstants.J2EE_1_3_ID:
              return IJ2EEFacetConstants.APPLICATION_CLIENT_13;
            case J2EEVersionConstants.J2EE_1_4_ID:
              return IJ2EEFacetConstants.APPLICATION_CLIENT_14;
            case J2EEVersionConstants.JEE_5_0_ID:
              return IJ2EEFacetConstants.APPLICATION_CLIENT_50;
            case J2EEVersionConstants.JEE_6_0_ID:
              return IJ2EEFacetConstants.APPLICATION_CLIENT_60;
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
   
    //If no application-client.xml found and the project depends on some java EE 6 jar then set application client facet to 6.0
    if (WTPProjectsUtil.hasInClassPath(mavenProjectFacade.getProject(), "javax.servlet.annotation.WebServlet")) {
      return IJ2EEFacetConstants.APPLICATION_CLIENT_60;
    }
    
    return DEFAULT_APPCLIENT_FACET_VERSION; 
  }
  
  /**
   * @return the first application-client.xml file found under META-INF, in all the resource folders.
   */
  public IFile getApplicationClientXml() {
    IProject project = mavenProjectFacade.getProject();
    String contentDir = null;
    for (IPath path : mavenProjectFacade.getResourceLocations()) {
      contentDir = path.toPortableString()+"/META-INF";
      IFile applicationClientXml = project.getFolder(contentDir).getFile("application-client.xml");
      if (applicationClientXml.exists()) {
        return applicationClientXml;
      }
    }
    return null;
  }
  
  /**
   * @return the first resource location directory declared in pom.xml
   */
  public String getContentDirectory(IProject project) {
    IPath[] resources = MavenProjectUtils.getResourceLocations(project, mavenProjectFacade.getMavenProject().getResources());
    return resources[0].toPortableString();
  }
  
  protected String getFilteringAttribute() {
    return "filterDeploymentDescriptor";
  }

}
