/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp;

import static org.eclipse.m2e.wtp.WTPProjectsUtil.hasWebFragmentFacet;
import static org.eclipse.m2e.wtp.WTPProjectsUtil.installJavaFacet;
import static org.eclipse.m2e.wtp.WTPProjectsUtil.isQualifiedAsWebFragment;
import static org.eclipse.m2e.wtp.WTPProjectsUtil.removeConflictingFacets;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jst.j2ee.web.project.facet.IWebFragmentFacetInstallDataModelProperties;
import org.eclipse.jst.j2ee.web.project.facet.WebFragmentFacetInstallDataModelProvider;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.markers.IMavenMarkerManager;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.eclipse.wst.common.frameworks.datamodel.DataModelFactory;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IFacetedProject.Action;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Project configurator for web-fragment projects. A web-fragment project is a java project having a
 * META-INF/web-fragment.xml file
 * 
 * @author Fred Bricon
 */
public class WebFragmentProjectConfigurator extends AbstractProjectConfigurator {

  private static final Logger LOG = LoggerFactory.getLogger(WebFragmentProjectConfigurator.class); 

  /**
   * Adds the Java and Web fragment facets to jar projects qualified as web-fragments, i.e, 
   * having a MET-INF/web-fragment.xml file in their output build directory (or any of their resource folders)
   * If a previous utility facet was installed, it's removed before setting the Web Fragment facet.
   */
  @Override
  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
    IMavenProjectFacade facade = request.getMavenProjectFacade();
    IProject project = facade.getProject();
    if(!isQualifiedAsWebFragment(facade)){
      return;
    }
    
    IMavenMarkerManager mavenMarkerManager = MavenPluginActivator.getDefault().getMavenMarkerManager();
    try {
      mavenMarkerManager.deleteMarkers(project,IMavenConstants.MARKER_CONFIGURATION_ID);//FIXME This is utterly wrong. Need to handle non core markers
      configureWebfragment(facade, monitor);
    } catch (CoreException cex) {
      mavenMarkerManager.addErrorMarkers(project, IMavenConstants.MARKER_CONFIGURATION_ID, cex);
    }
  }

  private void configureWebfragment(IMavenProjectFacade facade, IProgressMonitor monitor) throws CoreException {
    IProject project = facade.getProject();
    IFacetedProject facetedProject = ProjectFacetsManager.create(project, true, monitor);
    
    Set<Action> actions = new LinkedHashSet<Action>();
    
    ResourceCleaner fileCleaner = new ResourceCleaner(project);
    addFilesToClean(fileCleaner, facade.getResourceLocations());
    addFilesToClean(fileCleaner, facade.getCompileSourceLocations());
    
    removeConflictingFacets(facetedProject, WTPProjectsUtil.WEB_FRAGMENT_3_0, actions);
     
    try {
      //Install or update the java facet
      installJavaFacet(actions, project, facetedProject);
      
      //Only install the web facet fragment if necessary
      if(!hasWebFragmentFacet(project)) {
        IDataModel cfg = DataModelFactory.createDataModel(new WebFragmentFacetInstallDataModelProvider());
        //Don't create a associated war project
        cfg.setProperty(IWebFragmentFacetInstallDataModelProperties.ADD_TO_WAR, false);
 
        actions.add(new IFacetedProject.Action(
                            IFacetedProject.Action.Type.INSTALL, 
                            WTPProjectsUtil.WEB_FRAGMENT_3_0, cfg));
      }

      facetedProject.modify(actions, monitor);
      
      //remove test folder links
      WTPProjectsUtil.removeTestFolderLinks(project, facade.getMavenProject(), monitor, "/");
    } finally {
      try {
        //Remove any WTP created files (extras fragment descriptor and manifest) 
        fileCleaner.cleanUp();
      } catch (CoreException cex) {
        LOG.error("Error while cleaning up WTP's created files", cex);
      }
    }
    
  }

  private void addFilesToClean(ResourceCleaner cleaner, IPath[] paths) {
    for (IPath resourceFolderPath : paths) {
      cleaner.addFiles(resourceFolderPath.append("META-INF/web-fragment.xml"));
      cleaner.addFiles(resourceFolderPath.append("META-INF/MANIFEST.MF"));
    }
  }
  
}
