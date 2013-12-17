/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp.internal.conversion;

import static org.eclipse.m2e.wtp.internal.conversion.MavenPluginUtils.configure;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jst.j2ee.project.EarUtilities;
import org.eclipse.jst.j2ee.project.facet.IJ2EEFacetConstants;
import org.eclipse.m2e.wtp.WTPProjectsUtil;
import org.eclipse.m2e.wtp.internal.Messages;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts Eclipse WTP EAR project settings into maven-ear-plugin configuration 
 *
 * @author Fred Bricon
 */
public class EarProjectConverter extends AbstractWtpProjectConversionParticipant {

  private static final String DEFAULT_APPLICATION_FOLDER = "src/main/application"; //$NON-NLS-1$
  
  private static final String EAR_SOURCE_DIRECTORY_KEY = "earSourceDirectory"; //$NON-NLS-1$

  private static final String EAR_VERSION = "version"; //$NON-NLS-1$

  private static final String GENERATE_APPLICATION_XML = "generateApplicationXml"; //$NON-NLS-1$
  
  private static final Logger LOG = LoggerFactory.getLogger(EarProjectConverter.class);

  @Override
public void convert(IProject project, Model model, IProgressMonitor monitor) throws CoreException {
    if (!accept(project) || !"ear".equals(model.getPackaging())) { //$NON-NLS-1$
      return;
    }
    IVirtualComponent component = ComponentCore.createComponent(project);
    if (component == null) {
      return;
    }

    setEarPlugin(component, model);
  }

  private void setEarPlugin(IVirtualComponent component, Model model) throws CoreException {
    Build build = getCloneOrCreateBuild(model);
    String pluginVersion = MavenPluginUtils.getMostRecentPluginVersion("org.apache.maven.plugins", "maven-ear-plugin", "2.9"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    Plugin earPlugin = setPlugin(build, "org.apache.maven.plugins", "maven-ear-plugin", pluginVersion); //$NON-NLS-1$ //$NON-NLS-2$
  
    // Set  <earSourceDirectory>EarContent</earSourceDirectory>
    IFolder applicationContentFolder = findEarContentFolder(component);
    boolean hasApplicationXml=true;
    boolean customized = false;
    if (applicationContentFolder != null) {
      String applicationContent = applicationContentFolder.getProjectRelativePath().toPortableString();
      if (!DEFAULT_APPLICATION_FOLDER.equals(applicationContent)) {
        configure(earPlugin, EAR_SOURCE_DIRECTORY_KEY, applicationContent);
        customized = true;
      }
      hasApplicationXml = applicationContentFolder.getFile("META-INF/application.xml").exists(); //$NON-NLS-1$
    }
    else{
      IProject project = component.getProject();
      String msg =  NLS.bind(Messages.EarProjectConverter_Error_EAR_Root_Content_Folder, (project!= null?project.getName():component.getName())); 
      LOG.warn(msg);
    }
    
    IFacetedProject fProject = ProjectFacetsManager.create(component.getProject());
    if (fProject != null) {
      IProjectFacetVersion earVersion = fProject.getProjectFacetVersion(IJ2EEFacetConstants.ENTERPRISE_APPLICATION_FACET);
      if (!IJ2EEFacetConstants.ENTERPRISE_APPLICATION_13.equals(earVersion)) {
        String version;
        boolean isJavaEE = earVersion.compareTo(IJ2EEFacetConstants.ENTERPRISE_APPLICATION_50) >= 0;
        if (isJavaEE) {
          // maven-ear-plugin needs version 5 instead of 5.0
          version = earVersion.getVersionString().substring(0, 1);//Yuck!
          if (!hasApplicationXml){
            configure(earPlugin, GENERATE_APPLICATION_XML, "false"); //$NON-NLS-1$
          }
        } else {
          version = earVersion.getVersionString();
        }
        configure(earPlugin, EAR_VERSION, version);
        customized = true;
      }
      
      String libDir = EarUtilities.getEARLibDir(component);
      if (libDir == null) {
        libDir = inspectDefaultLibDirs(applicationContentFolder);
      }
      if (libDir != null) {
        configure(earPlugin, "defaultLibBundleDir", libDir); //$NON-NLS-1$
        customized = true;
      }
      
    }

    if (customized) {
      model.setBuild(build);
    }
  }

  /**
   * Checks if a lib directory exists in the application content folder
   * @return the relative path of the lib directory, if it exists, null otherwise
   */
  private String inspectDefaultLibDirs(IFolder applicationContentFolder) {
    
    for (String candidate : new String[] {"lib", "APP-INF/lib"}) { //$NON-NLS-1$ //$NON-NLS-2$
      if  (applicationContentFolder.getFolder(candidate).exists()) {
        return candidate;
      }
    }
    return null;
  }

  private IFolder findEarContentFolder(IVirtualComponent component) {
    return WTPProjectsUtil.getDefaultDeploymentDescriptorFolder(component.getRootFolder());
  }

  @Override
  protected IProjectFacet getRequiredFaced() {
    return WTPProjectsUtil.EAR_FACET;
  }

}
