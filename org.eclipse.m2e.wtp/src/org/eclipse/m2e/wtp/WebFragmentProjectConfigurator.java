/*******************************************************************************
 * Copyright (c) 2008-2014 Sonatype, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp;

import static org.eclipse.m2e.wtp.WTPProjectsUtil.installJavaFacet;
import static org.eclipse.m2e.wtp.WTPProjectsUtil.removeConflictingFacets;
import static org.eclipse.m2e.wtp.internal.webfragment.WebFragmentUtil.getWebFragment;
import static org.eclipse.m2e.wtp.internal.webfragment.WebFragmentUtil.isQualifiedAsWebFragment;

import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.Set;

import org.codehaus.plexus.util.IOUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jst.j2ee.web.project.facet.IWebFragmentFacetInstallDataModelProperties;
import org.eclipse.jst.j2ee.web.project.facet.WebFragmentFacetInstallDataModelProvider;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.markers.IMavenMarkerManager;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ILifecycleMappingConfiguration;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.eclipse.m2e.wtp.internal.Messages;
import org.eclipse.m2e.wtp.internal.webfragment.WebFragmentQuickPeek;
import org.eclipse.wst.common.frameworks.datamodel.DataModelFactory;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IFacetedProject.Action;
import org.eclipse.wst.common.project.facet.core.IFacetedProject.Action.Type;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Project configurator for web-fragment projects. A web-fragment project is a java project having a
 * META-INF/web-fragment.xml file
 * 
 * @provisional This class has been added as part of a work in progress. 
 * It is not guaranteed to work or remain the same in future releases. 
 * For more information contact <a href="mailto:m2e-wtp-dev@eclipse.org">m2e-wtp-dev@eclipse.org</a>.
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

    if(WTPProjectsUtil.isM2eWtpDisabled(facade, monitor) || !project.isAccessible() || 
    		project.getResourceAttributes().isReadOnly() || !isQualifiedAsWebFragment(facade)){
      return;
    }
    
    IMavenMarkerManager mavenMarkerManager = MavenPluginActivator.getDefault().getMavenMarkerManager();
    try {
      mavenMarkerManager.deleteMarkers(project,MavenWtpConstants.WTP_MARKER_CONFIGURATION_ERROR_ID);
      configureWebfragment(facade, monitor);
    } catch (CoreException cex) {
      mavenMarkerManager.addErrorMarkers(project, MavenWtpConstants.WTP_MARKER_CONFIGURATION_ERROR_ID, cex);
    }
  }

  private void configureWebfragment(IMavenProjectFacade facade, IProgressMonitor monitor) throws CoreException {
	  
	IFile webFragment = getWebFragment(facade);
	if (webFragment == null) {
		return;
	}
	
	IProjectFacetVersion facetVersion = getVersion(webFragment);
	if (facetVersion == null) {
		return;
	}
	
    IProject project = facade.getProject();
    IFacetedProject facetedProject = ProjectFacetsManager.create(project, true, monitor);
    
    Set<Action> actions = new LinkedHashSet<Action>();
    
    ResourceCleaner fileCleaner = new ResourceCleaner(project);
    addFoldersToClean(fileCleaner, facade);
     
    try {
      IProjectFacetVersion currentFacetVersion = facetedProject.getProjectFacetVersion(WTPProjectsUtil.WEB_FRAGMENT_FACET);
      Type actionType = null;
      if(currentFacetVersion == null) {
    	  removeConflictingFacets(facetedProject, facetVersion, actions);
          installJavaFacet(actions, project, facetedProject);

    	  //Only install the web facet fragment if necessary
    	  actionType = IFacetedProject.Action.Type.INSTALL;
      } else if (facetVersion.compareTo(currentFacetVersion) > 0) {
    	  actionType = IFacetedProject.Action.Type.VERSION_CHANGE;  
      }

 	  if (actionType != null) {
          IDataModel cfg = DataModelFactory.createDataModel(new WebFragmentFacetInstallDataModelProvider());
          //Don't create an associated war project
          cfg.setProperty(IWebFragmentFacetInstallDataModelProperties.ADD_TO_WAR, false);
    	  actions.add(new IFacetedProject.Action(actionType,facetVersion, cfg)); 
      }
      
      if (!actions.isEmpty()) {
    	  facetedProject.modify(actions, monitor);
      }
      
    } finally {
      try {
        //Remove any WTP created files (extras fragment descriptor and manifest) 
        fileCleaner.cleanUp();
      } catch (CoreException cex) {
        LOG.error(Messages.Error_Cleaning_WTP_Files, cex);
      }
    }
    //remove test folder links
    WTPProjectsUtil.removeTestFolderLinks(project, facade.getMavenProject(), monitor, "/"); //$NON-NLS-1$
    
    WTPProjectsUtil.setNonDependencyAttributeToContainer(project, monitor);
  }

  private IProjectFacetVersion getVersion(IFile webFragment) {
	InputStream in = null;
	try {
		webFragment.refreshLocal(IResource.DEPTH_ZERO, new NullProgressMonitor());
		in = webFragment.getContents();
		WebFragmentQuickPeek peek = new WebFragmentQuickPeek(in);
		String version = peek.getVersion();
		if (version != null) {
			return WTPProjectsUtil.WEB_FRAGMENT_FACET.getVersion(version);
		}
	} catch (Exception e) {
		// ignore
		LOG.error("Error_Reading_WebFragment", e); //$NON-NLS-1$
		return WTPProjectsUtil.WEB_FRAGMENT_FACET.getDefaultVersion();
	} finally {
		IOUtil.close(in);
	}
	return null;
  }

  protected void addFoldersToClean(ResourceCleaner fileCleaner, IMavenProjectFacade facade) {
    for (IPath p : facade.getCompileSourceLocations()) {
      if (p != null) {
        fileCleaner.addFiles(p.append("META-INF/MANIFEST.MF")); //$NON-NLS-1$
        fileCleaner.addFiles(p.append("META-INF/web-fragment.xml")); //$NON-NLS-1$
        fileCleaner.addFolder(p);
      }
    }
    for (IPath p : facade.getResourceLocations()) {
      if (p != null) {
        fileCleaner.addFiles(p.append("META-INF/MANIFEST.MF")); //$NON-NLS-1$
        fileCleaner.addFiles(p.append("META-INF/web-fragment.xml")); //$NON-NLS-1$
        fileCleaner.addFolder(p);
      }
    }
    for (IPath p : facade.getTestCompileSourceLocations()) {
      if (p != null) fileCleaner.addFolder(p);
    }
    for (IPath p : facade.getTestResourceLocations()) {
      if (p != null) fileCleaner.addFolder(p);
    }
  }
  
  @Override
  public boolean hasConfigurationChanged(IMavenProjectFacade newFacade,
			ILifecycleMappingConfiguration oldProjectConfiguration,
			MojoExecutionKey key, IProgressMonitor monitor) {
    return false;
  }
}
