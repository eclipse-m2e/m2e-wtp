/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
import org.eclipse.jst.common.project.facet.WtpUtils;
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
import org.eclipse.m2e.wtp.internal.Messages;
import org.eclipse.m2e.wtp.internal.utilities.DebugUtilities;
import org.eclipse.m2e.wtp.internal.webfragment.WebFragmentUtil;
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
      IClasspathDependencyConstants.CLASSPATH_COMPONENT_NON_DEPENDENCY, ""); //$NON-NLS-1$

  protected static final IPath ROOT_PATH = new Path("/");  //$NON-NLS-1$

  protected final IMavenProjectRegistry projectManager;

  protected final IMavenMarkerManager mavenMarkerManager;

  AbstractProjectConfiguratorDelegate() {
    this.projectManager = MavenPlugin.getMavenProjectRegistry();
    this.mavenMarkerManager = MavenPluginActivator.getDefault().getMavenMarkerManager();
  }
  
  @Override
public void configureProject(IProject project, MavenProject mavenProject, IProgressMonitor monitor) throws MarkedException {
    try {
      mavenMarkerManager.deleteMarkers(project,MavenWtpConstants.WTP_MARKER_CONFIGURATION_ERROR_ID);
      configure(project, mavenProject, monitor);
    } catch (CoreException cex) {
      //TODO Filter out constraint violations
      mavenMarkerManager.addErrorMarkers(project, MavenWtpConstants.WTP_MARKER_CONFIGURATION_ERROR_ID, cex);
      throw new MarkedException(NLS.bind(Messages.AbstractProjectConfiguratorDelegate_Unable_To_Configure_Project,project.getName()), cex);
    }
  }
 
  protected abstract void configure(IProject project, MavenProject mavenProject, IProgressMonitor monitor) throws CoreException;

  protected List<IMavenProjectFacade> getWorkspaceDependencies(IProject project, MavenProject mavenProject) {
    Set<IProject> projects = new HashSet<>();
    List<IMavenProjectFacade> dependencies = new ArrayList<>();
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
    WTPProjectsUtil.configureWtpUtil(facade, monitor);
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
    String depPackaging = dependencyMavenProjectFacade.getPackaging();
    //jee dependency has not been configured yet - i.e. it has no JEE facet-
    if(!JEEPackaging.isJEEPackaging(depPackaging)) {
      // XXX Probably should create a UtilProjectConfiguratorDelegate
      configureWtpUtil(dependencyMavenProjectFacade, monitor);
    }
    return dependency;
  }

  @SuppressWarnings("restriction")
  protected void configureDeployedName(IProject project, String deployedFileName) {
    IVirtualComponent projectComponent = ComponentCore.createComponent(project);
    if(projectComponent != null && !deployedFileName.equals(projectComponent.getDeployedName())){//MNGECLIPSE-2331 : Seems projectComponent.getDeployedName() can be null 
      StructureEdit moduleCore = null;
      try {
        moduleCore = StructureEdit.getStructureEditForWrite(project);
        if (moduleCore != null){
          WorkbenchComponent component = moduleCore.getComponent();
          if (component != null) {
            component.setName(deployedFileName);
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

  @Override
public void configureClasspath(IProject project, MavenProject mavenProject, IClasspathDescriptor classpath,
      IProgressMonitor monitor) throws CoreException {
    // do nothing
  }

  @Override
  public void setModuleDependencies(IProject project, MavenProject mavenProject, IProgressMonitor monitor)
      throws CoreException {
    // do nothing
  }
  
  protected void addFoldersToClean(ResourceCleaner fileCleaner, IMavenProjectFacade facade) {
    WTPProjectsUtil.addFoldersToClean(fileCleaner, facade);
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
    if (!componentInclusions.equals(props.getProperty(MavenWtpConstants.COMPONENT_INCLUSION_PATTERNS, ""))) { //$NON-NLS-1$
      component.setMetaProperty(MavenWtpConstants.COMPONENT_INCLUSION_PATTERNS, componentInclusions);
    }
    if (!componentExclusions.equals(props.getProperty(MavenWtpConstants.COMPONENT_EXCLUSION_PATTERNS, ""))) { //$NON-NLS-1$
      component.setMetaProperty(MavenWtpConstants.COMPONENT_EXCLUSION_PATTERNS, componentExclusions);
    }
  }
  
}
