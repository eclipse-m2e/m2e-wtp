/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp;

import static org.eclipse.m2e.wtp.WTPProjectsUtil.removeConflictingFacets;

import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jst.j2ee.project.facet.AppClientFacetInstallDataModelProvider;
import org.eclipse.jst.j2ee.project.facet.IAppClientFacetInstallDataModelProperties;
import org.eclipse.jst.j2ee.project.facet.IJ2EEFacetInstallDataModelProperties;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.frameworks.datamodel.DataModelFactory;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IFacetedProject.Action;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;


/**
 *  Configures Application Client projects based on their maven-acr-plugin configuration.
 * 
 * @author Fred Bricon
 */
class AppClientProjectConfiguratorDelegate extends AbstractProjectConfiguratorDelegate {

  protected void configure(IProject project, MavenProject mavenProject, IProgressMonitor monitor)
      throws CoreException {
   
    IFacetedProject facetedProject = ProjectFacetsManager.create(project, true, monitor);

    Set<Action> actions = new LinkedHashSet<Action>();
    installJavaFacet(actions, project, facetedProject);

    IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().create(project.getFile(IMavenConstants.POM_FILE_NAME), true, monitor);
    //Reading content directory, used by WTP to create META-INF/manifest.mf, application-client.xml
    AcrPluginConfiguration config = new AcrPluginConfiguration(facade);
    String contentDir = config.getContentDirectory(project);
    IProjectFacetVersion fv = config.getFacetVersion();
    
    if(!facetedProject.hasProjectFacet(WTPProjectsUtil.APP_CLIENT_FACET)) {
      removeConflictingFacets(facetedProject, fv, actions);
      actions.add(new IFacetedProject.Action(IFacetedProject.Action.Type.INSTALL, fv, getAppClientDataModel(contentDir)));
    } else {
      IProjectFacetVersion projectFacetVersion = facetedProject.getProjectFacetVersion(WTPProjectsUtil.APP_CLIENT_FACET);     
      if(fv.getVersionString() != null && !fv.getVersionString().equals(projectFacetVersion.getVersionString())){
          actions.add(new IFacetedProject.Action(IFacetedProject.Action.Type.VERSION_CHANGE, fv, getAppClientDataModel(contentDir)));
      } 
    }

    if(!actions.isEmpty()) {
      ResourceCleaner fileCleaner = new ResourceCleaner(project);
      try {
        addFoldersToClean(fileCleaner, facade);
        
        facetedProject.modify(actions, monitor);
      } finally {
        //Remove any unwanted MANIFEST.MF the Facet installation has created
        fileCleaner.cleanUp();
      }
    }

    //Add the moduleCoreNature, in case it's missing 
    fixMissingModuleCoreNature(project, monitor);
    
    //Remove test folder links to prevent exporting them when packaging the project
    removeTestFolderLinks(project, mavenProject, monitor, "/");
    
    IVirtualComponent component = ComponentCore.createComponent(project);
    if (component != null) {
      IPath contentDirPath = new Path("/").append(contentDir);
      WTPProjectsUtil.setDefaultDeploymentDescriptorFolder(component.getRootFolder(), contentDirPath, monitor);
    }
    
    //Remove "library unavailable at runtime" warning.
    setNonDependencyAttributeToContainer(project, monitor);
    
    //Remove WTP classpath libraries conflicting with the Maven one
    WTPProjectsUtil.removeWTPClasspathContainer(project);
}

  private IDataModel getAppClientDataModel(String contentDir) {
    IDataModel appClientModelCfg = DataModelFactory.createDataModel(new AppClientFacetInstallDataModelProvider());
    appClientModelCfg.setProperty(IAppClientFacetInstallDataModelProperties.CONFIG_FOLDER, contentDir);
    appClientModelCfg.setProperty(IAppClientFacetInstallDataModelProperties.CREATE_DEFAULT_MAIN_CLASS, false);
    appClientModelCfg.setProperty(IJ2EEFacetInstallDataModelProperties.GENERATE_DD, false);
    return appClientModelCfg;
  }

}
