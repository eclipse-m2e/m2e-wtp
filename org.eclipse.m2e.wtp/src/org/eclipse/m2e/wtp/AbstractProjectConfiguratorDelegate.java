/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp;

import static org.eclipse.m2e.wtp.internal.StringUtils.joinAsString;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jst.j2ee.classpathdep.IClasspathDependencyConstants;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.markers.IMavenMarkerManager;
import org.eclipse.m2e.core.internal.markers.SourceLocation;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.core.project.MavenProjectUtils;
import org.eclipse.m2e.jdt.IClasspathDescriptor;
import org.eclipse.m2e.wtp.internal.utilities.DebugUtilities;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.internal.StructureEdit;
import org.eclipse.wst.common.componentcore.internal.WorkbenchComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualReference;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IFacetedProject.Action;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Base class to configure JavaEE projects
 * 
 * @author Igor Fedorenko
 * @author Fred Bricon
 * 
 * @provisional This class has been added as part of a work in progress. 
 * It is not guaranteed to work or remain the same in future releases. 
 * For more information contact <a href="mailto:m2e-wtp-dev@eclipse.org">m2e-wtp-dev@eclipse.org</a>.
 * 
 */
abstract class AbstractProjectConfiguratorDelegate implements IProjectConfiguratorDelegate {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractProjectConfiguratorDelegate.class); 
  
  static final IClasspathAttribute NONDEPENDENCY_ATTRIBUTE = JavaCore.newClasspathAttribute(
      IClasspathDependencyConstants.CLASSPATH_COMPONENT_NON_DEPENDENCY, "");

  protected static final IPath ROOT_PATH = new Path("/"); 

  protected final IMavenProjectRegistry projectManager;

  protected final IMavenMarkerManager mavenMarkerManager;

  AbstractProjectConfiguratorDelegate() {
    this.projectManager = MavenPlugin.getMavenProjectRegistry();
    this.mavenMarkerManager = MavenPluginActivator.getDefault().getMavenMarkerManager();
  }
  
  public void configureProject(IProject project, MavenProject mavenProject, IProgressMonitor monitor) throws MarkedException {
    try {
      mavenMarkerManager.deleteMarkers(project,MavenWtpConstants.WTP_MARKER_CONFIGURATION_ERROR_ID);
      configure(project, mavenProject, monitor);
    } catch (CoreException cex) {
      //TODO Filter out constraint violations
      mavenMarkerManager.addErrorMarkers(project, MavenWtpConstants.WTP_MARKER_CONFIGURATION_ERROR_ID, cex);
      throw new MarkedException("Unable to configure "+project.getName(), cex);
    }
  }
 
  protected abstract void configure(IProject project, MavenProject mavenProject, IProgressMonitor monitor) throws CoreException;

  protected List<IMavenProjectFacade> getWorkspaceDependencies(IProject project, MavenProject mavenProject) {
    Set<IProject> projects = new HashSet<IProject>();
    List<IMavenProjectFacade> dependencies = new ArrayList<IMavenProjectFacade>();
    Set<Artifact> artifacts = mavenProject.getArtifacts();
    for(Artifact artifact : artifacts) {
      IMavenProjectFacade dependency = projectManager.getMavenProject(artifact.getGroupId(), artifact.getArtifactId(),
          artifact.getVersion());
      
      if((Artifact.SCOPE_COMPILE.equals(artifact.getScope()) 
          || Artifact.SCOPE_RUNTIME.equals(artifact.getScope())) //MNGECLIPSE-1578 Runtime dependencies should be deployed 
          && dependency != null && !dependency.getProject().equals(project) && dependency.getFullPath(artifact.getFile()) != null
          && projects.add(dependency.getProject())) {
        dependencies.add(dependency);
      }
    }
    return dependencies;
  }

  protected void configureWtpUtil(IMavenProjectFacade facade, IProgressMonitor monitor) throws CoreException {
    // Adding utility facet on JEE projects is not allowed
    IProject project = facade.getProject();
    MavenProject mavenProject = facade.getMavenProject();
    if(  !WTPProjectsUtil.isJavaProject(facade)
       || WTPProjectsUtil.isJavaEEProject(project) 
       || WTPProjectsUtil.isQualifiedAsWebFragment(facade)) {
      return;
    }
    
    //MECLIPSEWTP-66 delete extra MANIFEST.MF
    IPath[] sourceRoots = MavenProjectUtils.getSourceLocations(project, mavenProject.getCompileSourceRoots());
    IPath[] resourceRoots = MavenProjectUtils.getResourceLocations(project, mavenProject.getResources());
    
    //MECLIPSEWTP-182 check if the Java Project configurator has been successfully run before doing anything : 
    if (!checkJavaConfiguration(project, sourceRoots, resourceRoots)) {
      LOG.warn("{} Utility Facet configuration is aborted as the Java Configuration is inconsistent", project.getName());
      return;
    }

    boolean isDebugEnabled = DebugUtilities.isDebugEnabled();
    if (isDebugEnabled) {
      DebugUtilities.debug(DebugUtilities.dumpProjectState("Before configuration ",project));
    }

    // 2 - check if the manifest already exists, and its parent folder
    
    IFacetedProject facetedProject = ProjectFacetsManager.create(project, true, monitor);
    Set<Action> actions = new LinkedHashSet<Action>();
    installJavaFacet(actions, project, facetedProject);

    if(!facetedProject.hasProjectFacet(WTPProjectsUtil.UTILITY_FACET)) {
      actions.add(new IFacetedProject.Action(IFacetedProject.Action.Type.INSTALL, WTPProjectsUtil.UTILITY_10, null));
    } else if(!facetedProject.hasProjectFacet(WTPProjectsUtil.UTILITY_10)) {
      actions.add(new IFacetedProject.Action(IFacetedProject.Action.Type.VERSION_CHANGE, WTPProjectsUtil.UTILITY_10,
          null));
    }
    
    if (!actions.isEmpty()) {
      ResourceCleaner fileCleaner = new ResourceCleaner(project);
      try {
        addFoldersToClean(fileCleaner, facade);
        facetedProject.modify(actions, monitor);      
      } finally {
        //Remove any unwanted MANIFEST.MF the Facet installation has created
        fileCleaner.cleanUp();
      } 
    }
    
    fixMissingModuleCoreNature(project, monitor);
    
    if (isDebugEnabled) {
      DebugUtilities.debug(DebugUtilities.dumpProjectState("after configuration ",project));
    }
    //MNGECLIPSE-904 remove tests folder links for utility jars
    removeTestFolderLinks(project, mavenProject, monitor, "/");
    
    //Remove "library unavailable at runtime" warning.
    if (isDebugEnabled) {
      DebugUtilities.debug(DebugUtilities.dumpProjectState("after removing test folders ",project));
    }

    setNonDependencyAttributeToContainer(project, monitor);
    
    WTPProjectsUtil.removeWTPClasspathContainer(project);
  }

  /**
   * Checks the maven source folders are correctly added to the project classpath
   */
  private boolean checkJavaConfiguration(IProject project, IPath[] sourceRoots, IPath[] resourceRoots) throws JavaModelException {
    IJavaProject javaProject = JavaCore.create(project);
    if (javaProject == null) {
      return false;
    }
    IClasspathEntry[] cpEntries = javaProject.getRawClasspath();
    if (cpEntries == null) {
      return false;
    }
    Set<IPath> currentPaths = new HashSet<IPath>();
    for (IClasspathEntry entry  : cpEntries) {
      if (IClasspathEntry.CPE_SOURCE == entry.getEntryKind()){
        currentPaths.add(entry.getPath().makeRelativeTo(project.getFullPath()));
      }
    }
    for(IPath mavenSource : sourceRoots) {
        if (mavenSource != null && !mavenSource.isEmpty()) {
          IFolder sourceFolder = project.getFolder(mavenSource);
          if (sourceFolder.exists() && !currentPaths.contains(mavenSource)) {
            return false;
          }
        }
    }
    for(IPath mavenSource : resourceRoots) {
      if (mavenSource != null && !mavenSource.isEmpty()) {
        IFolder resourceFolder = project.getFolder(mavenSource);
        if (resourceFolder.exists() && !currentPaths.contains(mavenSource)) {
          return false;
        }
      }
  }
    return true;
  }

  /**
   * Add the ModuleCoreNature to a project, if necessary.
   * 
   * @param project An accessible project.
   * @param monitor A progress monitor to track the time to completion
   * @throws CoreException if the ModuleCoreNature cannot be added
   */
  protected void fixMissingModuleCoreNature(IProject project, IProgressMonitor monitor) throws CoreException {
    WTPProjectsUtil.fixMissingModuleCoreNature(project, monitor);
  }

  protected void installJavaFacet(Set<Action> actions, IProject project, IFacetedProject facetedProject) {
    WTPProjectsUtil.installJavaFacet(actions, project, facetedProject);
  }

  protected void removeTestFolderLinks(IProject project, MavenProject mavenProject, IProgressMonitor monitor,
      String folder) throws CoreException {
    WTPProjectsUtil.removeTestFolderLinks(project, mavenProject, monitor, folder);
  }

  protected void addContainerAttribute(IProject project, IClasspathAttribute attribute, IProgressMonitor monitor)
      throws JavaModelException {
    updateContainerAttributes(project, attribute, null, monitor);
  }

  protected void setNonDependencyAttributeToContainer(IProject project, IProgressMonitor monitor) throws JavaModelException {
    WTPProjectsUtil.updateContainerAttributes(project, NONDEPENDENCY_ATTRIBUTE, IClasspathDependencyConstants.CLASSPATH_COMPONENT_DEPENDENCY, monitor);
  }

  protected void updateContainerAttributes(IProject project, IClasspathAttribute attributeToAdd, String attributeToDelete, IProgressMonitor monitor)
  throws JavaModelException {
    WTPProjectsUtil.updateContainerAttributes(project, attributeToAdd, attributeToDelete, monitor);
  }

  /**
   * @param dependencyMavenProjectFacade
   * @param monitor
   * @return
   * @throws CoreException
   */
  protected IProject preConfigureDependencyProject(IMavenProjectFacade dependencyMavenProjectFacade, IProgressMonitor monitor) throws CoreException {
    IProject dependency = dependencyMavenProjectFacade.getProject();
    MavenProject mavenDependency = dependencyMavenProjectFacade.getMavenProject(monitor);
    String depPackaging = dependencyMavenProjectFacade.getPackaging();
    //jee dependency has not been configured yet - i.e. it has no JEE facet-
    if(JEEPackaging.isJEEPackaging(depPackaging) && !WTPProjectsUtil.isJavaEEProject(dependency)) {
      IProjectConfiguratorDelegate delegate = ProjectConfiguratorDelegateFactory
          .getProjectConfiguratorDelegate(dependencyMavenProjectFacade.getPackaging());
      if(delegate != null) {
        //Lets install the proper facets
        try {
          delegate.configureProject(dependency, mavenDependency, monitor);
        } catch(MarkedException ex) {
          //Markers already have been created for this exception, no more to do.
          return dependency;
        }
      }
    } else {
      // XXX Probably should create a UtilProjectConfiguratorDelegate
      configureWtpUtil(dependencyMavenProjectFacade, monitor);
    }
    return dependency;
  }

  @SuppressWarnings("restriction")
  protected void configureDeployedName(IProject project, String deployedFileName) {
    //We need to remove the file extension from deployedFileName 
    int extSeparatorPos  = deployedFileName.lastIndexOf('.');
    String deployedName = extSeparatorPos > -1? deployedFileName.substring(0, extSeparatorPos): deployedFileName;
    //From jerr's patch in MNGECLIPSE-965
    IVirtualComponent projectComponent = ComponentCore.createComponent(project);
    if(projectComponent != null && !deployedName.equals(projectComponent.getDeployedName())){//MNGECLIPSE-2331 : Seems projectComponent.getDeployedName() can be null 
      StructureEdit moduleCore = null;
      try {
        moduleCore = StructureEdit.getStructureEditForWrite(project);
        if (moduleCore != null){
          WorkbenchComponent component = moduleCore.getComponent();
          if (component != null) {
            component.setName(deployedName);
            moduleCore.saveIfNecessary(null);
          }
        }
      } finally {
        if (moduleCore != null) {
          moduleCore.dispose();
        }
      }
    }  
  }

  /**
   * Link a project's file to a specific deployment destination. Existing links will be deleted beforehand. 
   * @param project 
   * @param sourceFile the existing file to deploy
   * @param targetRuntimePath the target runtime/deployment location of the file
   * @param monitor
   * @throws CoreException
   */
  protected void linkFileFirst(IProject project, String sourceFile, String targetRuntimePath, IProgressMonitor monitor) throws CoreException {
      IPath runtimePath = new Path(targetRuntimePath);
      //We first delete any existing links
      WTPProjectsUtil.deleteLinks(project, runtimePath, monitor);
      if (sourceFile != null) {
        //Create the new link
        WTPProjectsUtil.insertLinkFirst(project, new Path(sourceFile), new Path(targetRuntimePath), monitor);
      }
  }

  @Deprecated
  protected boolean hasChanged(IVirtualReference[] existingRefs, IVirtualReference[] refArray) {
      return WTPProjectsUtil.hasChanged(existingRefs, refArray);
  }

  public void configureClasspath(IProject project, MavenProject mavenProject, IClasspathDescriptor classpath,
      IProgressMonitor monitor) throws CoreException {
    // do nothing
  }

  public void setModuleDependencies(IProject project, MavenProject mavenProject, IProgressMonitor monitor)
      throws CoreException {
    // do nothing
  }
  
  protected void addFoldersToClean(ResourceCleaner fileCleaner, IMavenProjectFacade facade) {
    for (IPath p : facade.getCompileSourceLocations()) {
      if (p != null) {
        fileCleaner.addFiles(p.append("META-INF/MANIFEST.MF"));
        fileCleaner.addFolder(p);
      }
    }
    for (IPath p : facade.getResourceLocations()) {
      if (p != null) {
        fileCleaner.addFiles(p.append("META-INF/MANIFEST.MF"));
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
  
  /**
   * Add inclusion/exclusion patterns to .component metadata. WTP server adapters can use that information to 
   * include/exclude resources from deployment accordingly. This is currently implemented in the JBoss AS server adapter.
   * @throws CoreException 
   */
  protected void addComponentExclusionPatterns(IVirtualComponent component, IMavenPackageFilter filter)  {
    String[] warSourceIncludes = filter.getSourceIncludes();
    String[] packagingIncludes = filter.getPackagingIncludes();
    String[] warSourceExcludes = filter.getSourceExcludes();
    String[] packagingExcludes = filter.getPackagingExcludes();
    
    if (warSourceIncludes.length > 0 && packagingIncludes.length >0) {
      IResource pomFile = component.getProject().getFile(IMavenConstants.POM_FILE_NAME);
      // We might get bad pattern overlapping (**/* + **/*.html would return everything, 
      // when maven would only package html files) . 
      // So we arbitrary (kinda) keep the packaging patterns only. But this can lead to other funny discrepancies 
      // things like **/pages/** + **/*.html should return only html files from the pages directory, but here, will return
      // every html files.
      SourceLocation sourceLocation = filter.getSourceLocation();
      if (sourceLocation != null) {
        mavenMarkerManager.addMarker(pomFile, 
                                   MavenWtpConstants.WTP_MARKER_CONFIGURATION_ERROR_ID,
                                   NLS.bind(Messages.markers_inclusion_patterns_problem, filter.getSourceIncludeParameterName()), 
                                   sourceLocation.getLineNumber(), 
                                   IMarker.SEVERITY_WARNING);
      }
      warSourceIncludes = null;
    }
    String componentInclusions = joinAsString(warSourceIncludes, packagingIncludes);
    String componentExclusions = joinAsString(warSourceExcludes, packagingExcludes);
    Properties props = component.getMetaProperties();
    if (!componentInclusions.equals(props.getProperty(MavenWtpConstants.COMPONENT_INCLUSION_PATTERNS, ""))) {
      component.setMetaProperty(MavenWtpConstants.COMPONENT_INCLUSION_PATTERNS, componentInclusions);
    }
    if (!componentExclusions.equals(props.getProperty(MavenWtpConstants.COMPONENT_EXCLUSION_PATTERNS, ""))) {
      component.setMetaProperty(MavenWtpConstants.COMPONENT_EXCLUSION_PATTERNS, componentExclusions);
    }
  }
  
}
