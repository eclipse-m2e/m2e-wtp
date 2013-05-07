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
import org.eclipse.m2e.wtp.internal.Messages;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  
  private static final int JEE_7_0_ID = 70;

  private static final Logger LOG = LoggerFactory.getLogger(AcrPluginConfiguration.class);
  
  final IMavenProjectFacade mavenProjectFacade;

  public AcrPluginConfiguration(IMavenProjectFacade facade) {

    MavenProject mavenProject = facade.getMavenProject();
    if (JEEPackaging.APP_CLIENT != JEEPackaging.getValue(mavenProject.getPackaging()))
      throw new IllegalArgumentException(Messages.AcrPluginConfiguration_Error_Project_Not_appclient);
    
    this.mavenProjectFacade = facade;
    Plugin plugin = mavenProject.getPlugin("org.apache.maven.plugins:maven-acr-plugin"); //$NON-NLS-1$
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
            case JEE_7_0_ID:
            	//This can only happen when run in WTP >= 3.5
            	//Don't use/create a static 1.7 facet version, it'd blow up WTP < 3.5
                return IJ2EEFacetConstants.APPLICATION_CLIENT_FACET.getVersion("7.0"); //$NON-NLS-1$
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
   
    IProject project = mavenProjectFacade.getProject();
    //If no application-client.xml found, don't change existing facet version
    try {
        IFacetedProject fProject = ProjectFacetsManager.create(project);
        if (fProject != null && fProject.hasProjectFacet(IJ2EEFacetConstants.APPLICATION_CLIENT_FACET)) {
          return fProject.getProjectFacetVersion(IJ2EEFacetConstants.APPLICATION_CLIENT_FACET);
        }
    } catch (Exception e) {
        LOG.warn(NLS.bind(Messages.Error_Reading_Project_Facet, project.getName()), e); 
    }      

    
    //If no application-client.xml found and the project depends on some java EE 6 jar then set application client facet to 6.0
    //FIXME this is totally arbitrary. Need to find a better solution.
    if (WTPProjectsUtil.hasInClassPath(mavenProjectFacade.getProject(), "javax.servlet.annotation.WebServlet")) { //$NON-NLS-1$
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
      contentDir = path.toPortableString()+"/META-INF"; //$NON-NLS-1$
      IFile applicationClientXml = project.getFolder(contentDir).getFile("application-client.xml"); //$NON-NLS-1$
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
  
  @Override
protected String getFilteringAttribute() {
    return "filterDeploymentDescriptor"; //$NON-NLS-1$
  }

}
