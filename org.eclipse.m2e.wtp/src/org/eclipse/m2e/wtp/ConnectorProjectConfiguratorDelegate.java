/*******************************************************************************
 * Copyright (c) 2008, 2014 Sonatype, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp;

import static org.eclipse.m2e.wtp.WTPProjectsUtil.removeConflictingFacets;

import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jst.j2ee.jca.project.facet.ConnectorFacetInstallDataModelProvider;
import org.eclipse.jst.j2ee.jca.project.facet.IConnectorFacetInstallDataModelProperties;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectUtils;
import org.eclipse.m2e.wtp.namemapping.FileNameMappingFactory;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.ModuleCoreNature;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.eclipse.wst.common.componentcore.resources.IVirtualReference;
import org.eclipse.wst.common.componentcore.resources.IVirtualResource;
import org.eclipse.wst.common.frameworks.datamodel.DataModelFactory;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IFacetedProject.Action;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

/**
 * Configures Connector (RAR) projects based on their maven-rar-plugin configuration.
 *
 * @author Fred Bricon
 */
class ConnectorProjectConfiguratorDelegate extends AbstractProjectConfiguratorDelegate{

  public static final ArtifactFilter SCOPE_FILTER_RUNTIME = new ScopeArtifactFilter(Artifact.SCOPE_RUNTIME);

  /* (non-Javadoc)
   * @see org.eclipse.m2e.wtp.AbstractProjectConfiguratorDelegate#configure(org.eclipse.core.resources.IProject, org.apache.maven.project.MavenProject, org.eclipse.core.runtime.IProgressMonitor)
   */
  @Override
protected void configure(IProject project, MavenProject mavenProject, IProgressMonitor monitor) throws CoreException {
    IFacetedProject facetedProject = ProjectFacetsManager.create(project, true, monitor);
    if (facetedProject == null) {
      return;
    }

    Set<Action> actions = new LinkedHashSet<Action>();
    installJavaFacet(actions, project, facetedProject);

    IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().create(project, monitor);
    RarPluginConfiguration config = new RarPluginConfiguration(facade);
    
    String contentDir = config.getRarContentDirectory();

    IProjectFacetVersion connectorFv = config.getConnectorFacetVersion();

    IDataModel rarModelCfg = DataModelFactory.createDataModel(new ConnectorFacetInstallDataModelProvider());

    IFolder contentFolder = project.getFolder(contentDir);
    if(!facetedProject.hasProjectFacet(WTPProjectsUtil.JCA_FACET)) {

      // Configuring content directory, used by WTP to create META-INF/manifest.mf, ra.xml
     
      rarModelCfg.setProperty(IConnectorFacetInstallDataModelProperties.CONFIG_FOLDER, contentDir);
      //Don't generate ra.xml by default - Setting will be ignored for JCA 1.6
      rarModelCfg.setProperty(IConnectorFacetInstallDataModelProperties.GENERATE_DD, false);
      rarModelCfg.setBooleanProperty(IConnectorFacetInstallDataModelProperties.ADD_TO_EAR, false);

      removeConflictingFacets(facetedProject, connectorFv, actions);

      actions.add(new IFacetedProject.Action(IFacetedProject.Action.Type.INSTALL, connectorFv, rarModelCfg));
    } else {
      IProjectFacetVersion projectFacetVersion = facetedProject.getProjectFacetVersion(WTPProjectsUtil.JCA_FACET);     
      
      if(projectFacetVersion.getVersionString() != null && !projectFacetVersion.getVersionString().equals(projectFacetVersion.getVersionString())){

        removeConflictingFacets(facetedProject, connectorFv, actions);

        actions.add(new IFacetedProject.Action(IFacetedProject.Action.Type.VERSION_CHANGE, connectorFv, rarModelCfg));
      } 
    }
    String customRaXml = config.getCustomRaXml();
    
    if(!actions.isEmpty()) {
      ResourceCleaner fileCleaner = new ResourceCleaner(project);
      try {
        addFoldersToClean(fileCleaner, facade);
        fileCleaner.addFiles(contentFolder.getFile("META-INF/MANIFEST.MF").getProjectRelativePath()); //$NON-NLS-1$
        if (customRaXml != null) {
          fileCleaner.addFiles(contentFolder.getFile("META-INF/ra.xml").getProjectRelativePath()); //$NON-NLS-1$
        }
        
        facetedProject.modify(actions, monitor);
      } finally {
        //Remove any unwanted MANIFEST.MF the Facet installation has created
        fileCleaner.cleanUp();
      }
    }

    //MECLIPSEWTP-41 Fix the missing moduleCoreNature
    fixMissingModuleCoreNature(project, monitor);
    
    IVirtualComponent component = ComponentCore.createComponent(project);
    if (component != null) {

      if (config.isJarIncluded()) {
        addSourceLinks(component, mavenProject, monitor);
      } else {
        //project classes won't be jar'ed in the resulting rar.
        removeSourceLinks(component, mavenProject, monitor);
      }
      
      removeTestFolderLinks(project, mavenProject, monitor, "/");  //$NON-NLS-1$
      
      linkFileFirst(project, customRaXml, "META-INF/ra.xml", monitor); //$NON-NLS-1$

      IPath contentDirPath = new Path("/").append(contentDir); //$NON-NLS-1$
      
      if (!WTPProjectsUtil.hasLink(project, ROOT_PATH, contentDirPath, monitor)) {
        component.getRootFolder().createLink(contentDirPath, IVirtualResource.NONE, monitor); 
      }
      
      WTPProjectsUtil.setDefaultDeploymentDescriptorFolder(component.getRootFolder(), contentDirPath, monitor);
    }

    setNonDependencyAttributeToContainer(project, monitor);

    //Remove "library unavailable at runtime" warning. TODO is it relevant for connector projects?
    WTPProjectsUtil.removeWTPClasspathContainer(project);
    
  }

  private void addSourceLinks(IVirtualComponent component, MavenProject mavenProject, IProgressMonitor monitor) throws CoreException {
    IProject project = component.getProject();
    IPath classesPath = MavenProjectUtils.getProjectRelativePath(project, mavenProject.getBuild().getOutputDirectory());
    if (classesPath != null) {
      for(IPath location : MavenProjectUtils.getSourceLocations(project, mavenProject.getCompileSourceRoots())) {
        addLinkIfNecessary(component, location, monitor);
      }
      for(IPath location : MavenProjectUtils.getResourceLocations(project, mavenProject.getResources())) {
        addLinkIfNecessary(component, location, monitor);
      }
    }
  }

  private void addLinkIfNecessary(IVirtualComponent component, IPath location, IProgressMonitor monitor) throws CoreException {
    IProject project = component.getProject();
    if (location!=null && !WTPProjectsUtil.hasLink(project, ROOT_PATH, location, monitor)) {
      if (project.getFolder(location).isAccessible()) {
        component.getRootFolder().createLink(location, IVirtualResource.NONE, monitor); 
      }
    }
  }

  
  
  private void removeSourceLinks(IVirtualComponent component, MavenProject mavenProject, IProgressMonitor monitor) throws CoreException {
      IVirtualFolder jsrc = component.getRootFolder();
      IProject project = component.getProject();
      for(IPath location : MavenProjectUtils.getSourceLocations(project, mavenProject.getCompileSourceRoots())) {
        jsrc.removeLink(location, 0, monitor);
      }
      for(IPath location : MavenProjectUtils.getResourceLocations(project, mavenProject.getResources())) {
        jsrc.removeLink(location, 0, monitor);
      }
  }
  

  
  /**
   * @see org.eclipse.m2e.wtp.IProjectConfiguratorDelegate#setModuleDependencies(org.eclipse.core.resources.IProject, org.apache.maven.project.MavenProject, org.eclipse.core.runtime.IProgressMonitor)
   */
  @Override
public void setModuleDependencies(IProject project, MavenProject mavenProject, IProgressMonitor monitor)
      throws CoreException {

    IVirtualComponent rarComponent = ComponentCore.createComponent(project);
    
    Set<IVirtualReference> newRefs = new LinkedHashSet<IVirtualReference>();
    
    Set<Artifact> artifacts =  mavenProject.getArtifacts();
    
    //Adding artifact references in .component. See https://bugs.eclipse.org/bugs/show_bug.cgi?id=297777#c1
    for(Artifact artifact : artifacts) {
    	ArtifactHelper.fixArtifactHandler(artifact.getArtifactHandler());
      //Don't deploy pom, non runtime or optional dependencies
      if("pom".equals(artifact.getType()) || !SCOPE_FILTER_RUNTIME.include(artifact) || artifact.isOptional()) { //$NON-NLS-1$
        continue;
      }
      
      IMavenProjectFacade workspaceDependency = projectManager.getMavenProject(artifact.getGroupId(), artifact
          .getArtifactId(), artifact.getVersion());

      if(workspaceDependency != null && !workspaceDependency.getProject().equals(project)
          && workspaceDependency.getFullPath(artifact.getFile()) != null) {
        //artifact dependency is a workspace project
        IProject depProject = preConfigureDependencyProject(workspaceDependency, monitor);
        if (ModuleCoreNature.isFlexibleProject(depProject)) {
          newRefs.add(createReference(rarComponent, depProject, artifact));
        }
      } else {
        //artifact dependency should be added as a JEE module, referenced with M2_REPO variable 
        newRefs.add(createReference(rarComponent, artifact));
      }
    }

    IVirtualReference[] newRefsArray = new IVirtualReference[newRefs.size()];
    newRefs.toArray(newRefsArray);
    
    //Only change the project references if they've changed
    IVirtualReference[] references = WTPProjectsUtil.extractHardReferences(rarComponent, false);
    if (WTPProjectsUtil.hasChanged(references, newRefsArray)) {
      rarComponent.setReferences(newRefsArray);
    }
  }

  private IVirtualReference createReference(IVirtualComponent rarComponent, IProject project, Artifact artifact) {
    IVirtualComponent depComponent = ComponentCore.createComponent(project);
    IVirtualReference depRef = ComponentCore.createReference(rarComponent, depComponent);
    String deployedFileName = FileNameMappingFactory.getDefaultFileNameMapping().mapFileName(artifact);
    depRef.setArchiveName(deployedFileName);
    return depRef;
  }
  
  private IVirtualReference createReference(IVirtualComponent rarComponent, Artifact artifact) {
      //Create dependency component, referenced from the local Repo.
      String artifactPath = ArtifactHelper.getM2REPOVarPath(artifact);
      IVirtualComponent depComponent = ComponentCore.createArchiveComponent(rarComponent.getProject(), artifactPath);
      return ComponentCore.createReference(rarComponent, depComponent);
  }

  
}
