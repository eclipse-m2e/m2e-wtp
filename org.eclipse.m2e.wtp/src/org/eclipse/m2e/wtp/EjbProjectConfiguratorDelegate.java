/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
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
import org.eclipse.jst.j2ee.ejb.project.operations.IEjbFacetInstallDataModelProperties;
import org.eclipse.jst.j2ee.internal.ejb.project.operations.EjbFacetInstallDataModelProvider;
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
 * EjbProjectConfiguratorDelegate
 * 
 * @author Fred Bricon
 */
@SuppressWarnings("restriction")
class EjbProjectConfiguratorDelegate extends AbstractProjectConfiguratorDelegate {

  protected void configure(IProject project, MavenProject mavenProject, IProgressMonitor monitor)
      throws CoreException {
    IFacetedProject facetedProject = ProjectFacetsManager.create(project, true, monitor);

    IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().create(project.getFile(IMavenConstants.POM_FILE_NAME), true, monitor);

    Set<Action> actions = new LinkedHashSet<Action>();
    installJavaFacet(actions, project, facetedProject);

    EjbPluginConfiguration config = new EjbPluginConfiguration(mavenProject);
    String contentDir = config.getEjbContentDirectory(project);
    IProjectFacetVersion ejbFv = config.getEjbFacetVersion();
    
    if(!facetedProject.hasProjectFacet(WTPProjectsUtil.EJB_FACET)) {
      removeConflictingFacets(facetedProject, ejbFv, actions);
      actions.add(new IFacetedProject.Action(IFacetedProject.Action.Type.INSTALL, ejbFv, getEjbDataModel(contentDir)));
    } else {
      IProjectFacetVersion projectFacetVersion = facetedProject.getProjectFacetVersion(WTPProjectsUtil.EJB_FACET);     
      if(ejbFv.getVersionString() != null && !ejbFv.getVersionString().equals(projectFacetVersion.getVersionString())){
          actions.add(new IFacetedProject.Action(IFacetedProject.Action.Type.VERSION_CHANGE, ejbFv, getEjbDataModel(contentDir)));
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

    //MECLIPSEWTP-41 Fix the missing moduleCoreNature
    fixMissingModuleCoreNature(project, monitor);
    
    removeTestFolderLinks(project, mavenProject, monitor, "/");

    IVirtualComponent ejbComponent = ComponentCore.createComponent(project);
    if (ejbComponent != null) {
      IPath contentDirPath = new Path("/").append(contentDir);
      WTPProjectsUtil.setDefaultDeploymentDescriptorFolder(ejbComponent.getRootFolder(), contentDirPath, monitor);
    }
    
    //Remove "library unavailable at runtime" warning.
    setNonDependencyAttributeToContainer(project, monitor);
    
    WTPProjectsUtil.removeWTPClasspathContainer(project);
  }

  private Object getEjbDataModel(String contentDir) {
    IDataModel ejbModelCfg = DataModelFactory.createDataModel(new EjbFacetInstallDataModelProvider());
    ejbModelCfg.setProperty(IEjbFacetInstallDataModelProperties.CONFIG_FOLDER, contentDir);
    return ejbModelCfg;
  }

  public void setModuleDependencies(IProject project, MavenProject mavenProject, IProgressMonitor monitor)
      throws CoreException {
    // TODO check if there's anything to do!
  }
}
