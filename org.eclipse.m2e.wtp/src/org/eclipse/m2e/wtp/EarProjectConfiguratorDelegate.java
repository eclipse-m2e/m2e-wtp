
/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp;

import static org.eclipse.m2e.wtp.WTPProjectsUtil.removeConflictingFacets;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jst.j2ee.earcreation.IEarFacetInstallDataModelProperties;
import org.eclipse.jst.j2ee.internal.earcreation.EarFacetInstallDataModelProvider;
import org.eclipse.jst.j2ee.internal.project.J2EEProjectUtilities;
import org.eclipse.jst.j2ee.model.IEARModelProvider;
import org.eclipse.jst.j2ee.model.ModelProviderManager;
import org.eclipse.jst.javaee.application.Application;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.wtp.earmodules.EarModule;
import org.eclipse.m2e.wtp.internal.Messages;
import org.eclipse.m2e.wtp.internal.utilities.PathUtil;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.ModuleCoreNature;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualReference;
import org.eclipse.wst.common.componentcore.resources.IVirtualResource;
import org.eclipse.wst.common.frameworks.datamodel.DataModelFactory;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IFacetedProject.Action;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Configures EAR projects based on their maven-ear-plugin configuration.
 * 
 * @see org.eclipse.jst.j2ee.ui.AddModulestoEARPropertiesPage
 * @author Fred Bricon
 */
@SuppressWarnings("restriction")
class EarProjectConfiguratorDelegate extends AbstractProjectConfiguratorDelegate {

  private static final Logger LOG = LoggerFactory.getLogger(EarProjectConfiguratorDelegate.class); 

  @Override
protected void configure(IProject project, MavenProject mavenProject, IProgressMonitor monitor)
      throws CoreException {
    
    monitor.setTaskName(NLS.bind(Messages.EarProjectConfiguratorDelegate_Configuring_EAR_Project,project.getName()));
    
    IFacetedProject facetedProject = ProjectFacetsManager.create(project, true, monitor);
    IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().create(project.getFile(IMavenConstants.POM_FILE_NAME), true, monitor);
    
    EarPluginConfiguration config = new EarPluginConfiguration(mavenProject);
    Set<Action> actions = new LinkedHashSet<Action>();

    String contentDir = config.getEarContentDirectory(project);
    contentDir = PathUtil.toPortablePath(contentDir);
    IFolder contentFolder = project.getFolder(contentDir);

    ResourceCleaner fileCleaner = new ResourceCleaner(project);
    addFoldersToClean(fileCleaner, facade);
    fileCleaner.addFiles(contentFolder.getFile("META-INF/application.xml").getProjectRelativePath()); //$NON-NLS-1$

    IProjectFacetVersion earFv = config.getEarFacetVersion();
    if(!facetedProject.hasProjectFacet(WTPProjectsUtil.EAR_FACET)) {
      removeConflictingFacets(facetedProject, earFv, actions);
      actions.add(new IFacetedProject.Action(IFacetedProject.Action.Type.INSTALL, earFv, getEarModel(contentDir)));
    } else {
      //MECLIPSEWTP-37 : don't uninstall the EAR Facet, as it causes constraint failures when used with RAD
      IProjectFacetVersion projectFacetVersion = facetedProject.getProjectFacetVersion(WTPProjectsUtil.EAR_FACET);     
      if(earFv.getVersionString() != null && !earFv.getVersionString().equals(projectFacetVersion.getVersionString())){
          actions.add(new IFacetedProject.Action(IFacetedProject.Action.Type.VERSION_CHANGE, earFv, getEarModel(contentDir)));
      } 
    }
    
    try {
      if(!actions.isEmpty()) {
        facetedProject.modify(actions, monitor);
      }
    }
    finally {
      try {
        //Remove any WTP created files (extra application.xml and manifest) 
        fileCleaner.cleanUp();
      } catch (CoreException cex) {
        LOG.error(Messages.Error_Cleaning_WTP_Files, cex);
      }
    }
    //MECLIPSEWTP-41 Fix the missing moduleCoreNature
    fixMissingModuleCoreNature(project, monitor);
    
    IVirtualComponent earComponent = ComponentCore.createComponent(project);
    IPath contentDirPath = new Path((contentDir.startsWith("/"))?contentDir:"/"+contentDir); //$NON-NLS-1$ //$NON-NLS-2$
    //Ensure the EarContent link has been created
    if (!WTPProjectsUtil.hasLink(project, ROOT_PATH, contentDirPath, monitor)) {
      earComponent.getRootFolder().createLink(contentDirPath, IVirtualResource.NONE, monitor);
    }
    WTPProjectsUtil.setDefaultDeploymentDescriptorFolder(earComponent.getRootFolder(), contentDirPath, monitor);

    //MECLIPSEWTP-56 : application.xml should not be generated in the source directory
    boolean useBuildDirectory = MavenWtpPlugin.getDefault().getMavenWtpPreferencesManager().getPreferences(project).isApplicationXmGeneratedInBuildDirectory();
    boolean useResourcefiltering = config.isFilteringDeploymentDescriptorsEnabled();
    
    List<IPath> sourcePaths = new ArrayList<IPath>();
    sourcePaths.add(contentDirPath);
    
    if (!useBuildDirectory && useResourcefiltering) {
        mavenMarkerManager.addMarker(project, MavenWtpConstants.WTP_MARKER_CONFIGURATION_ERROR_ID, 
                                    Messages.markers_mavenarchiver_output_settings_ignored_warning, -1, IMarker.SEVERITY_WARNING);
    }
    if (useBuildDirectory || useResourcefiltering) {
      IPath m2eclipseWtpFolderPath = new Path("/").append(ProjectUtils.getM2eclipseWtpFolder(mavenProject, project)); //$NON-NLS-1$
      ProjectUtils.hideM2eclipseWtpFolder(mavenProject, project);
      IPath generatedResourcesPath = m2eclipseWtpFolderPath.append(Path.SEPARATOR+MavenWtpConstants.EAR_RESOURCES_FOLDER);
      sourcePaths.add(generatedResourcesPath);
      if (!WTPProjectsUtil.hasLink(project, ROOT_PATH, generatedResourcesPath, monitor)) {
        WTPProjectsUtil.insertLinkBefore(project, generatedResourcesPath, contentDirPath, ROOT_PATH, monitor);      
      }
     }

    //MECLIPSEWTP-161 remove stale source paths
    WTPProjectsUtil.deleteLinks(project, ROOT_PATH, sourcePaths, monitor);
    
    removeTestFolderLinks(project, mavenProject, monitor, "/"); //$NON-NLS-1$
    
    ProjectUtils.removeNature(project, JavaCore.NATURE_ID, monitor);

    String finalName = config.getFinalName();
    if (!finalName.endsWith(".ear")) { //$NON-NLS-1$
      finalName += ".ear"; //$NON-NLS-1$
    }
    configureDeployedName(project, finalName);
    project.refreshLocal(IResource.DEPTH_INFINITE, monitor);

    
    //MECLIPSEWTP-221 : add (in|ex)clusion patterns as .component metadata
    addComponentExclusionPatterns(earComponent, config);
    
    setModuleDependencies(project, mavenProject, monitor);
  }

  private IDataModel getEarModel(String contentDir) {
    IDataModel earModelCfg = DataModelFactory.createDataModel(new EarFacetInstallDataModelProvider());
    earModelCfg.setProperty(IEarFacetInstallDataModelProperties.CONTENT_DIR, contentDir);
    earModelCfg.setProperty(IEarFacetInstallDataModelProperties.GENERATE_DD, false);
    return earModelCfg;
  }

  @Override
public void setModuleDependencies(IProject project, MavenProject mavenProject, IProgressMonitor monitor)
      throws CoreException {
    IFacetedProject facetedProject = ProjectFacetsManager.create(project, true, monitor);
    if(!facetedProject.hasProjectFacet(WTPProjectsUtil.EAR_FACET)) {
      return;
    }

    IVirtualComponent earComponent = ComponentCore.createComponent(project);
    
    Set<IVirtualReference> newRefs = new LinkedHashSet<IVirtualReference>();
    
    EarPluginConfiguration config = new EarPluginConfiguration(mavenProject);
    // Retrieving all ear module configuration from maven-ear-plugin : User defined modules + artifacts dependencies.
    Set<EarModule> earModules = config.getEarModules();

    String libBundleDir = config.getDefaultBundleDirectory();

    updateLibDir(project, libBundleDir, monitor);
    
    IPackagingConfiguration packagingConfig = new PackagingConfiguration(config.getPackagingIncludes(), config.getPackagingExcludes());
    
    for(EarModule earModule : earModules) {

      Artifact artifact = earModule.getArtifact();
      IVirtualComponent depComponent = null;
      IMavenProjectFacade workspaceDependency = projectManager.getMavenProject(artifact.getGroupId(), artifact
          .getArtifactId(), artifact.getVersion());

      if(workspaceDependency != null && !workspaceDependency.getProject().equals(project)
          && workspaceDependency.getFullPath(artifact.getFile()) != null) {
        //artifact dependency is a workspace project
        IProject depProject = preConfigureDependencyProject(workspaceDependency, monitor);
        if (ModuleCoreNature.isFlexibleProject(depProject)) {
          depComponent = createDependencyComponent(earComponent, depProject);
        }
      } else {
        //artifact dependency should be added as a JEE module, referenced with M2_REPO variable 
        depComponent = createDependencyComponent(earComponent, earModule.getArtifact());
      }
      
      if (depComponent != null && packagingConfig.isPackaged(earModule.getUri())) {
        IVirtualReference depRef = ComponentCore.createReference(earComponent, depComponent);
        String bundleDir = (StringUtils.isBlank(earModule.getBundleDir()))?"/":earModule.getBundleDir(); //$NON-NLS-1$
        depRef.setRuntimePath(new Path(bundleDir));
        depRef.setArchiveName(earModule.getBundleFileName());
        newRefs.add(depRef);
      }
    }
    
    IVirtualReference[] newRefsArray = new IVirtualReference[newRefs.size()];
    newRefs.toArray(newRefsArray);
    
    //Only change the project references if they've changed
    if (hasChanged(earComponent.getReferences(), newRefsArray)) {
      earComponent.setReferences(newRefsArray);
    }

    boolean useBuildDirectory = MavenWtpPlugin.getDefault().getMavenWtpPreferencesManager().getPreferences(project).isApplicationXmGeneratedInBuildDirectory();
    DeploymentDescriptorManagement.INSTANCE.updateConfiguration(project, mavenProject, config, useBuildDirectory, monitor);
  }



  private void updateLibDir(IProject project, String newLibDir, IProgressMonitor monitor) {
    //Update lib dir only applies to Java EE 5 ear projects
    if(!J2EEProjectUtilities.isJEEProject(project)){ 
      return;
    }
    
    //if the ear project Java EE level was < 5.0, the following would throw a ClassCastException  
    final IEARModelProvider earModel = (IEARModelProvider)ModelProviderManager.getModelProvider(project);
    if (earModel == null) {
      return;
    }
    final Application app = (Application)earModel.getModelObject();
    if (app != null) {
      if (newLibDir == null || "/".equals(newLibDir)) { //$NON-NLS-1$
        newLibDir = "lib"; //$NON-NLS-1$
      } 
      //MECLIPSEWTP-167 : lib directory mustn't start with a slash
      else if (newLibDir.startsWith("/")) { //$NON-NLS-1$
        newLibDir = newLibDir.substring(1);
      }
      String oldLibDir = app.getLibraryDirectory();
      if (newLibDir.equals(oldLibDir)) return;
      final String libDir = newLibDir;
      earModel.modify(new Runnable() {
        @Override
		public void run() {     
        app.setLibraryDirectory(libDir);
      }}, null);
    }
  }


  private IVirtualComponent createDependencyComponent(IVirtualComponent earComponent, IProject project) {
    IVirtualComponent depComponent = ComponentCore.createComponent(project);
    return depComponent;
  }

  private IVirtualComponent createDependencyComponent(IVirtualComponent earComponent, Artifact artifact) {
      //Create dependency component, referenced from the local Repo.
      String artifactPath = ArtifactHelper.getM2REPOVarPath(artifact);
      IVirtualComponent depComponent = ComponentCore.createArchiveComponent(earComponent.getProject(), artifactPath);
      return depComponent;
  }
}
