/*******************************************************************************
 * Copyright (c) 2008, 2013 Sonatype, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp;

import static org.eclipse.m2e.wtp.WTPProjectsUtil.removeConflictingFacets;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jst.j2ee.classpathdep.IClasspathDependencyConstants;
import org.eclipse.jst.j2ee.componentcore.J2EEModuleVirtualComponent;
import org.eclipse.jst.j2ee.internal.project.J2EEProjectUtilities;
import org.eclipse.jst.j2ee.project.facet.IJ2EEModuleFacetInstallDataModelProperties;
import org.eclipse.jst.j2ee.web.project.facet.IWebFacetInstallDataModelProperties;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetInstallDataModelProvider;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.jdt.IClasspathDescriptor;
import org.eclipse.m2e.jdt.IClasspathEntryDescriptor;
import org.eclipse.m2e.wtp.internal.ExtensionReader;
import org.eclipse.m2e.wtp.internal.Messages;
import org.eclipse.m2e.wtp.internal.filtering.WebResourceFilteringConfiguration;
import org.eclipse.m2e.wtp.internal.utilities.ComponentModuleUtil;
import org.eclipse.m2e.wtp.namemapping.FileNameMapping;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * Configures web projects based on their maven-war-plugin configuration.
 *
 * @author Igor Fedorenko
 * @author Fred Bricon
 */
@SuppressWarnings("restriction")
class WebProjectConfiguratorDelegate extends AbstractProjectConfiguratorDelegate {

  private static final Logger LOG = LoggerFactory.getLogger(WebProjectConfiguratorDelegate.class);
  /**
   * See http://wiki.eclipse.org/ClasspathEntriesPublishExportSupport
   */
  static final IClasspathAttribute DEPENDENCY_ATTRIBUTE = JavaCore.newClasspathAttribute(
      IClasspathDependencyConstants.CLASSPATH_COMPONENT_DEPENDENCY, "/WEB-INF/lib"); //$NON-NLS-1$

  /**
  * Name of maven property that overrides WTP context root.
  */
  private static final String M2ECLIPSE_WTP_CONTEXT_ROOT = "m2eclipse.wtp.contextRoot"; //$NON-NLS-1$

  @Override
protected void configure(IProject project, MavenProject mavenProject, IProgressMonitor monitor)
      throws CoreException {
    IFacetedProject facetedProject = ProjectFacetsManager.create(project, true, monitor);
    IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().create(project.getFile(IMavenConstants.POM_FILE_NAME), true, monitor);

    // make sure to update the main deployment folder
    WarPluginConfiguration config = new WarPluginConfiguration(mavenProject, project);
    String warSourceDirectory = config.getWarSourceDirectory();

    IFolder contentFolder = project.getFolder(warSourceDirectory);

    Set<Action> actions = new LinkedHashSet<>();

    installJavaFacet(actions, project, facetedProject);

    IVirtualComponent component = ComponentCore.createComponent(project, true);

    //MNGECLIPSE-2279 get the context root from the final name of the project, or artifactId by default.
    String contextRoot = getContextRoot(mavenProject, config.getWarName());

    IProjectFacetVersion webFv = config.getWebFacetVersion(project);
    IDataModel webModelCfg = getWebModelConfig(warSourceDirectory, contextRoot);
    if(!facetedProject.hasProjectFacet(WebFacetUtils.WEB_FACET)) {
      removeConflictingFacets(facetedProject, webFv, actions);
      actions.add(new IFacetedProject.Action(IFacetedProject.Action.Type.INSTALL, webFv, webModelCfg));
    } else {
      IProjectFacetVersion projectFacetVersion = facetedProject.getProjectFacetVersion(WebFacetUtils.WEB_FACET);
      if(webFv.getVersionString() != null && !webFv.getVersionString().equals(projectFacetVersion.getVersionString())){
          actions.add(new IFacetedProject.Action(IFacetedProject.Action.Type.VERSION_CHANGE, webFv, webModelCfg));
      }
    }

    String customWebXml = config.getCustomWebXml(project);

    if(!actions.isEmpty()) {
      ResourceCleaner fileCleaner = new ResourceCleaner(project, contentFolder);
      try {
        addFoldersToClean(fileCleaner, facade);
        fileCleaner.addFiles(contentFolder.getFile("META-INF/MANIFEST.MF").getProjectRelativePath()); //$NON-NLS-1$
        fileCleaner.addFolder(contentFolder.getFolder("WEB-INF/lib")); //$NON-NLS-1$
        if (customWebXml != null) {
          fileCleaner.addFiles(contentFolder.getFile("WEB-INF/web.xml").getProjectRelativePath()); //$NON-NLS-1$
        }

        facetedProject.modify(actions, monitor);
      } finally {
        //Remove any unwanted MANIFEST.MF the Facet installation has created
        fileCleaner.cleanUp();
      }
    }

    //MECLIPSEWTP-41 Fix the missing moduleCoreNature
    fixMissingModuleCoreNature(project, monitor);

    configureDeployedName(project, config.getWarName());

    // MNGECLIPSE-632 remove test sources/resources from WEB-INF/classes
    removeTestFolderLinks(project, mavenProject, monitor, "/WEB-INF/classes"); //$NON-NLS-1$

    addContainerAttribute(project, DEPENDENCY_ATTRIBUTE, monitor);

    //MNGECLIPSE-2279 change the context root if needed
    if (!contextRoot.equals(J2EEProjectUtilities.getServerContextRoot(project))) {
      J2EEProjectUtilities.setServerContextRoot(project, contextRoot);
    }

    if (customWebXml != null) {
      linkFileFirst(project, customWebXml, "/WEB-INF/web.xml", monitor); //$NON-NLS-1$
    }


    component = ComponentCore.createComponent(project, true);
    if(component != null) {
      IVirtualFolder rootFolder = component.getRootFolder();
      IPath warPath = new Path("/").append(contentFolder.getProjectRelativePath()); //$NON-NLS-1$
      boolean warPathExists = WTPProjectsUtil.hasLink(project, ROOT_PATH, warPath, monitor);
      if (!warPathExists) {
        component.getRootFolder().createLink(warPath, IVirtualResource.NONE, monitor);
      }
      IPath currentDefaultLocation = J2EEModuleVirtualComponent.getDefaultDeploymentDescriptorFolder(rootFolder);
      if (currentDefaultLocation == null) {
    	  WTPProjectsUtil.setDefaultDeploymentDescriptorFolder(rootFolder , warPath, monitor);
      }
      //MECLIPSEWTP-22 support web filtered resources. Filtered resources directory must be declared BEFORE
      //the regular web source directory. First resources discovered take precedence on deployment
      IPath filteredFolder = new Path("/").append(WebResourceFilteringConfiguration.getTargetFolder(mavenProject, project)); //$NON-NLS-1$

      boolean useBuildDir = MavenWtpPlugin.getDefault().getMavenWtpPreferencesManager().getPreferences(project).isWebMavenArchiverUsesBuildDirectory();
      boolean useWebresourcefiltering = config.getWebResources() != null
                                        && config.getWebResources().length > 0
                                        || config.isFilteringDeploymentDescriptorsEnabled();

      if (useBuildDir || useWebresourcefiltering) {

        if (!useBuildDir && useWebresourcefiltering) {
          mavenMarkerManager.addMarker(project, MavenWtpConstants.WTP_MARKER_CONFIGURATION_ERROR_ID,
                                      Messages.markers_mavenarchiver_output_settings_ignored_warning, -1, IMarker.SEVERITY_WARNING);
        }
        if (!WTPProjectsUtil.hasLink(project, ROOT_PATH, filteredFolder, monitor)) {
        	WTPProjectsUtil.insertLinkBefore(project, filteredFolder, warPath, ROOT_PATH, monitor);
        }
      } else {
        component.getRootFolder().removeLink(filteredFolder,IVirtualResource.NONE, monitor);
      }

      addComponentExclusionPatterns(component, config);
    }
    WTPProjectsUtil.removeWTPClasspathContainer(project);

    setModuleDependencies(project, mavenProject, monitor);
  }

  private IDataModel getWebModelConfig(String warSourceDirectory, String contextRoot) {
    IDataModel webModelCfg = DataModelFactory.createDataModel(new WebFacetInstallDataModelProvider());
    webModelCfg.setProperty(IJ2EEModuleFacetInstallDataModelProperties.CONFIG_FOLDER, warSourceDirectory);
    webModelCfg.setProperty(IWebFacetInstallDataModelProperties.CONTEXT_ROOT, contextRoot);
    webModelCfg.setProperty(IJ2EEModuleFacetInstallDataModelProperties.GENERATE_DD, false);
    webModelCfg.setBooleanProperty(IWebFacetInstallDataModelProperties.ADD_TO_EAR, false);
    return webModelCfg;
  }

  @Override
  public void setModuleDependencies(IProject project, MavenProject mavenProject, IProgressMonitor monitor)
      throws CoreException {
    if (!ModuleCoreNature.isFlexibleProject(project)) {
      return;
    }
    //MECLIPSEWTP-41 Fix the missing moduleCoreNature
    fixMissingModuleCoreNature(project, monitor);

    IVirtualComponent component = ComponentModuleUtil.getOrCreateComponent(project, monitor);
    //if the attempt to create dependencies happens before the project is actually created, abort.
    //this will be created again when the project exists.
    if(component == null){
      LOG.error(project.getName() + "/.settings/org.eclipse.wst.common.component is missing or invalid. "
      		+ "Skipping module dependency configuration. Deployment issues may arise.");
      return;
    }

    WarPluginConfiguration config = new WarPluginConfiguration(mavenProject, project);
    Map<Artifact, String> deployedArtifacts = getDeployedArtifacts(mavenProject.getArtifacts(), config);

    List<AbstractDependencyConfigurator> depConfigurators = ExtensionReader.readDependencyConfiguratorExtensions(projectManager,
        MavenPlugin.getMavenRuntimeManager(), mavenMarkerManager);

    Set<IVirtualReference> references = new LinkedHashSet<>();

    List<IMavenProjectFacade> exportedDependencies = getWorkspaceDependencies(project, mavenProject);

    for(IMavenProjectFacade dependency : exportedDependencies) {
      String depPackaging = dependency.getPackaging();
      if ("pom".equals(depPackaging) //MNGECLIPSE-744 pom dependencies shouldn't be deployed //$NON-NLS-1$
          || "war".equals(depPackaging) //Overlays are dealt with the overlay configurator //$NON-NLS-1$
          || "zip".equals(depPackaging)) { //$NON-NLS-1$
        continue;
      }

      preConfigureDependencyProject(dependency, monitor);

      if (!ModuleCoreNature.isFlexibleProject(dependency.getProject())) {
        //Projects unsupported by WTP (ex. adobe flex projects) should not be added as references
        continue;
      }
      MavenProject depMavenProject =  dependency.getMavenProject(monitor);

      IVirtualComponent depComponent = ComponentCore.createComponent(dependency.getProject());

      ArtifactKey artifactKey = ArtifactHelper.toArtifactKey(depMavenProject.getArtifact());
      //Get artifact using the proper classifier
      Artifact artifact = ArtifactHelper.getArtifact(mavenProject.getArtifacts(), artifactKey);
      if (artifact == null) {
        //could not map key to artifact
        artifact = depMavenProject.getArtifact();
      }
      String deployedName = deployedArtifacts.get(artifact);

  		//an artifact in mavenProject.getArtifacts() doesn't have the "optional" value as depMavenProject.getArtifact();
  		if (deployedName != null) {
  		  IVirtualReference reference = ComponentCore.createReference(component, depComponent);
  		  IPath path = new Path("/WEB-INF/lib"); //$NON-NLS-1$
  		  reference.setArchiveName(deployedName);
  		  reference.setRuntimePath(path);
  		  references.add(reference);
  		}
    }

    IVirtualReference[] oldRefs = WTPProjectsUtil.extractHardReferences(component, false);

    IVirtualReference[] newRefs = references.toArray(new IVirtualReference[references.size()]);

    if (WTPProjectsUtil.hasChanged(oldRefs, newRefs)){
      //Only write in the .component file if necessary
      IVirtualReference[] overlayRefs = WTPProjectsUtil.extractHardReferences(component, true);
      IVirtualReference[] allRefs = new IVirtualReference[overlayRefs.length + newRefs.length];
      System.arraycopy(newRefs, 0, allRefs, 0, newRefs.length);
      System.arraycopy(overlayRefs, 0, allRefs, newRefs.length, overlayRefs.length);
      component.setReferences(allRefs);
    }

    //TODO why a 2nd loop???
    for(IMavenProjectFacade dependency : exportedDependencies) {
      MavenProject depMavenProject =  dependency.getMavenProject(monitor);
      Iterator<AbstractDependencyConfigurator> configurators = depConfigurators.iterator();
      while (configurators.hasNext()) {
        try {
          configurators.next().configureDependency(mavenProject, project, depMavenProject, dependency.getProject(), monitor);
        } catch(MarkedException ex) {
          //XXX handle this
        }
      }
    }
  }

  /**
   * Get the context root from a maven web project
   * @param mavenProject
   * @param warName
   * @return the final name of the project if it exists, or the project's artifactId.
   */
  protected String getContextRoot(MavenProject mavenProject, String warName) {
    String contextRoot;
	//MECLIPSEWTP-43 : Override with maven property
   String property = mavenProject.getProperties().getProperty(M2ECLIPSE_WTP_CONTEXT_ROOT);
   if (StringUtils.isBlank(property)) {
  		String finalName = warName;
  		if (StringUtils.isBlank(finalName)
  		   || finalName.equals(mavenProject.getArtifactId() + "-" + mavenProject.getVersion())) { //$NON-NLS-1$
  		  contextRoot = mavenProject.getArtifactId();
  		}  else {
  		  contextRoot = finalName;
  		}
  	} else {
  		contextRoot = property;
  	}

    return contextRoot.trim().replace(" ", "_"); //$NON-NLS-1$ //$NON-NLS-2$
  }

  @Override
  public void configureClasspath(IProject project, MavenProject mavenProject, IClasspathDescriptor classpath,
      IProgressMonitor monitor) throws CoreException {

    WarPluginConfiguration config = new WarPluginConfiguration(mavenProject, project);
    Set<Artifact> artifacts = mavenProject.getArtifacts();
    Map<Artifact, String> deployedArtifacts = getDeployedArtifacts(artifacts, config);

    Iterator<IClasspathEntryDescriptor> iter = classpath.getEntryDescriptors().iterator();
    while (iter.hasNext()) {
      IClasspathEntryDescriptor descriptor = iter.next();
      Artifact artifact = ArtifactHelper.getArtifact(artifacts, descriptor.getArtifactKey());
      if (artifact == null) {
        return;
      }

      String deployedName = deployedArtifacts.get(artifact);

      if(deployedName == null || isWorkspaceProject(artifact)) {
        descriptor.setClasspathAttribute(NONDEPENDENCY_ATTRIBUTE.getName(), NONDEPENDENCY_ATTRIBUTE.getValue());
        continue;
      }
      descriptor.getClasspathAttributes().put(IClasspathDependencyConstants.CLASSPATH_ARCHIVENAME_ATTRIBUTE, deployedName);
    }
  }

  private boolean isWorkspaceProject(Artifact artifact) {
	  IMavenProjectFacade facade = projectManager.getMavenProject(artifact.getGroupId(),
																	artifact.getArtifactId(),
																	artifact.getVersion());

	  return facade != null && facade.getFullPath(artifact.getFile()) != null;
  }

  private Map<Artifact, String> getDeployedArtifacts(Collection<Artifact> artifacts, WarPluginConfiguration config ) {
    if (artifacts == null || artifacts.isEmpty()) {
      return Collections.emptyMap();
    }
    int size = artifacts.size();
    Map<Artifact, String> artifactsMap = new LinkedHashMap<>(size);

    IPackagingConfiguration opts = new PackagingConfiguration(config.getPackagingIncludes(), config.getPackagingExcludes());
    FileNameMapping fileNameMapping = config.getFileNameMapping();

    Set<String> names = new HashSet<>(size);

    Set<String> duplicates = new HashSet<>(size);

    for (Artifact artifact : artifacts) {
      ArtifactHelper.fixArtifactHandler(artifact.getArtifactHandler());
      String deployedName = fileNameMapping.mapFileName(artifact);
      String scope = artifact.getScope();
    	boolean isDeployed =  (Artifact.SCOPE_COMPILE.equals(scope) || Artifact.SCOPE_RUNTIME.equals(scope))
    	                      && !artifact.isOptional()
    	                      && opts.isPackaged("WEB-INF/lib/"+deployedName); //$NON-NLS-1$
    	if (isDeployed) {
    	  if (!names.add(deployedName)) {
    		  duplicates.add(deployedName);
     	  }
    	  artifactsMap.put(artifact, deployedName);
    	}
	  }

    //disambiguate duplicates
    for (String name : duplicates) {
    	for (Map.Entry<Artifact, String> entry : artifactsMap.entrySet()) {
    		if (name.equals(entry.getValue())) {
    		    String newDeployedName = entry.getKey().getGroupId() + "-" + name; //$NON-NLS-1$
    			entry.setValue(newDeployedName);
    		}
    	}
    }

    return artifactsMap;
  }

}
