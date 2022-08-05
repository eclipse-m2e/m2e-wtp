/*******************************************************************************
 * Copyright (c) 2008-2014 Sonatype, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.wtp;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.common.project.facet.core.internal.JavaFacetUtil;
import org.eclipse.jst.j2ee.classpathdep.IClasspathDependencyConstants;
import org.eclipse.jst.j2ee.componentcore.J2EEModuleVirtualComponent;
import org.eclipse.jst.j2ee.internal.project.J2EEProjectUtilities;
import org.eclipse.jst.j2ee.project.JavaEEProjectUtilities;
import org.eclipse.jst.j2ee.project.facet.IJ2EEFacetConstants;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingFactory;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectUtils;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.jdt.internal.MavenClasspathHelpers;
import org.eclipse.m2e.wtp.internal.Messages;
import org.eclipse.m2e.wtp.internal.utilities.DebugUtilities;
import org.eclipse.m2e.wtp.internal.webfragment.WebFragmentUtil;
import org.eclipse.m2e.wtp.overlay.modulecore.IOverlayVirtualComponent;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.ModuleCoreNature;
import org.eclipse.wst.common.componentcore.internal.ComponentResource;
import org.eclipse.wst.common.componentcore.internal.StructureEdit;
import org.eclipse.wst.common.componentcore.internal.WorkbenchComponent;
import org.eclipse.wst.common.componentcore.internal.impl.ResourceTreeNode;
import org.eclipse.wst.common.componentcore.internal.impl.ResourceTreeRoot;
import org.eclipse.wst.common.componentcore.internal.util.FacetedProjectUtilities;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.eclipse.wst.common.componentcore.resources.IVirtualReference;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IFacetedProject.Action;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for WTP projects.
 * 
 * @provisional This class has been added as part of a work in progress. 
 * It is not guaranteed to work or remain the same in future releases. 
 * For more information contact <a href="mailto:m2e-wtp-dev@eclipse.org">m2e-wtp-dev@eclipse.org</a>.
 * 
 * @author Fred Bricon
 */
@SuppressWarnings("restriction")
public class WTPProjectsUtil {

  private static final Logger LOG = LoggerFactory.getLogger(WTPProjectsUtil.class);

  public static final IProjectFacet UTILITY_FACET = ProjectFacetsManager.getProjectFacet(IJ2EEFacetConstants.UTILITY);

  public static final IProjectFacetVersion UTILITY_10 = UTILITY_FACET.getVersion("1.0"); //$NON-NLS-1$

  public static final IProjectFacet EJB_FACET = ProjectFacetsManager.getProjectFacet(IJ2EEFacetConstants.EJB);

  public static final IProjectFacet JCA_FACET = ProjectFacetsManager.getProjectFacet(IJ2EEFacetConstants.JCA);
  
  public static final IProjectFacet WEB_FRAGMENT_FACET = ProjectFacetsManager.getProjectFacet(IJ2EEFacetConstants.WEBFRAGMENT);

  public static final IProjectFacetVersion WEB_FRAGMENT_3_0 = WEB_FRAGMENT_FACET.getVersion("3.0"); //$NON-NLS-1$

  public static final IProjectFacet DYNAMIC_WEB_FACET = ProjectFacetsManager
      .getProjectFacet(IJ2EEFacetConstants.DYNAMIC_WEB);

  public static final IProjectFacet APP_CLIENT_FACET = ProjectFacetsManager.getProjectFacet(IJ2EEFacetConstants.APPLICATION_CLIENT);

  public static final IClasspathAttribute NONDEPENDENCY_ATTRIBUTE = JavaCore.newClasspathAttribute(
      IClasspathDependencyConstants.CLASSPATH_COMPONENT_NON_DEPENDENCY, "");//$NON-NLS-1$

  /**
   * Defaults Web facet version to 2.5
   */
  public static final IProjectFacetVersion DEFAULT_WEB_FACET = DYNAMIC_WEB_FACET.getVersion("2.5"); //$NON-NLS-1$

  public static final IProjectFacet EAR_FACET = ProjectFacetsManager
      .getProjectFacet(IJ2EEFacetConstants.ENTERPRISE_APPLICATION);

  public static final String DYN_REQUESTED_REFERENCE_TYPE;

  public static final String M2E_WTP_ACTIVATION_PROPERTY = "m2e.wtp.activation"; //$NON-NLS-1$
  
  //TODO Clean that up for Eclipse Mars
  static {
    //Bug #385605 : IVirtualComponent.DISPLAYABLE_REFERENCES_ALL is not available in helios
    String reqRefType = null;
    try {
      Field displayableRefsAllField = IVirtualComponent.class.getField("DISPLAYABLE_REFERENCES_ALL"); //$NON-NLS-1$
      reqRefType = (String)displayableRefsAllField.get(null);
    } catch(Throwable e) {
      //Falling back on IVirtualComponent.HARD_REFERENCES works in helios wrt bug #385229 : 
      reqRefType = IVirtualComponent.HARD_REFERENCES;
    }
    DYN_REQUESTED_REFERENCE_TYPE = reqRefType;
  }
  
  /**
   * Checks if a project has a given class in its classpath 
   * @param project : the workspace project
   * @param className : the fully qualified name of the class to search for
   * @return true if className is found in the project's classpath (provided the project is a JavaProject and its classpath has been set.)   
   */
  public static boolean hasInClassPath(IProject project, String className) {
    boolean result = false;
    if (project != null){
      IJavaProject javaProject = JavaCore.create(project);
      try {
        if (javaProject!= null && javaProject.findType(className)!=null){
         result = true; 
        }
      } catch(JavaModelException ex) {
        //Ignore this
      }
    }
    return result;
  }

  
  /**
   * Checks if the project is one of Dynamic Web, EJB, Application client, EAR or JCA project.
   * @param project - the project to be checked.
   * @return true if the project is a JEE - or legacy J2EE - project (but not a utility project). 
   */
  public static boolean isJavaEEProject(IProject project) {
    return (J2EEProjectUtilities.isLegacyJ2EEProject(project) || J2EEProjectUtilities.isJEEProject(project)) && !JavaEEProjectUtilities.isUtilityProject(project); 
  }
  
  /**
   * Delete a project's component resources having a given runtimePath
   * @param project - the project to modify
   * @param runtimePath - the component resource runtime path (i.e. deploy path)
   * @param monitor - an eclipse monitor
   * @throws CoreException
   */
  public static void deleteLinks(IProject project, IPath runtimePath, IProgressMonitor monitor) throws CoreException {
    deleteLinks(project, runtimePath, null, monitor);
  }

  /**
   * Delete a project's component resources having a given runtimePath
   * @param project - the project to modify
   * @param runtimePath - the component resource runtime path (i.e. deploy path)
   * @param sourcePathToKeep - the list of source paths to keep
   * @param monitor - an eclipse monitor
   * @throws CoreException
   */
  public static void deleteLinks(IProject project, IPath runtimePath, List<IPath> sourcePathToKeep, IProgressMonitor monitor) throws CoreException {
    //Looks like WTP'APIS doesn't have such feature, hence this implementation.
    StructureEdit moduleCore = null;
    try {
      moduleCore = StructureEdit.getStructureEditForWrite(project);
      if (moduleCore == null) {
        return;
      }
      WorkbenchComponent component = moduleCore.getComponent();
      if (component == null)  {
        return;
      }
      ResourceTreeRoot root = ResourceTreeRoot.getDeployResourceTreeRoot(component);
      ComponentResource[] resources = root.findModuleResources(runtimePath, 0);
      for (ComponentResource link : resources) {
        if (runtimePath.equals(link.getRuntimePath()) && 
           (sourcePathToKeep == null || !sourcePathToKeep.contains(link.getSourcePath()))) {
          component.getResources().remove(link);
        }
      }
   }
   finally {
     if (moduleCore != null) {
       moduleCore.saveIfNecessary(monitor);
       moduleCore.dispose();
     }
    }
  }
  
  public static void insertLinkBefore(IProject project, IPath newSource, IPath referenceSource, IPath runtimePath, IProgressMonitor monitor) throws CoreException {
    //Looks like WTP'APIS doesn't have such feature, hence this implementation.
    StructureEdit moduleCore = null;
    try {
      moduleCore = StructureEdit.getStructureEditForWrite(project);
      if (moduleCore == null) {
        return;
      }
      WorkbenchComponent component = moduleCore.getComponent();
      if (component == null)  {
        return;
      }
      
      int i = 0;
      int refPosition = -1;
      int newSourcePosition = -1;
      List<ComponentResource> resources = component.getResources();
      
      for (ComponentResource resource : resources) {
        IPath sourcePath = resource.getSourcePath();
        if (referenceSource.equals(sourcePath)) {
          refPosition = i;
        } else if (newSource.equals(sourcePath)) {
          newSourcePosition = i;
        }
        if (refPosition > -1 &&  newSourcePosition > -1) {
          break;
        }
        i++;
      }
      if (refPosition < 0) {
        refPosition = i;
      }
      IResource folder = project.getFolder(newSource);
      if (newSourcePosition > refPosition) {
        component.getResources().move(newSourcePosition, refPosition);
      } else if (newSourcePosition < 0) {
        ComponentResource componentResource = moduleCore.createWorkbenchModuleResource(folder);
        componentResource.setRuntimePath(runtimePath);
        component.getResources().add(refPosition,componentResource);
      }
   }
   finally {
     if (moduleCore != null) {
       moduleCore.saveIfNecessary(monitor);
       moduleCore.dispose();
     }
    }
  }

  public static void insertLinkFirst(IProject project, IPath newSource, IPath runtimePath, IProgressMonitor monitor) throws CoreException {
    //Looks like WTP'APIS doesn't have such feature, hence this implementation.
    StructureEdit moduleCore = null;
    try {
      moduleCore = StructureEdit.getStructureEditForWrite(project);
      if (moduleCore == null) {
        return;
      }
      WorkbenchComponent component = moduleCore.getComponent();
      if (component == null)  {
        return;
      }
      
      IResource folder = project.getFolder(newSource);
      ComponentResource componentResource = moduleCore.createWorkbenchModuleResource(folder);
      componentResource.setRuntimePath(runtimePath);
      component.getResources().add(0,componentResource);
   }
   finally {
     if (moduleCore != null) {
       moduleCore.saveIfNecessary(monitor);
       moduleCore.dispose();
     }
    }
  }
  
  public static boolean hasLink(IProject project, IPath runtimePath, IPath aProjectRelativeLocation, IProgressMonitor monitor) throws CoreException {
    StructureEdit moduleCore = null;
    try {
      moduleCore = StructureEdit.getStructureEditForRead(project);
      if( moduleCore != null ) {
        WorkbenchComponent component = moduleCore.getComponent();
        if (component != null) {
          ResourceTreeRoot root = ResourceTreeRoot.getDeployResourceTreeRoot(component);
          ComponentResource[] resources = root.findModuleResources(runtimePath, ResourceTreeNode.CREATE_NONE);
          if (resources.length > 0) {
            for (int resourceIndx = 0; resourceIndx < resources.length; resourceIndx++) {
              if (aProjectRelativeLocation.makeAbsolute().equals(resources[resourceIndx].getSourcePath())) {
                return true;
              }
            }
          }
        }
      }
    }
    finally {
      if (moduleCore != null) {
        moduleCore.dispose();
      }
    }
    return false;
  }

  /**
   * @param project
   * @param dir
   * @return
   */
  public static IPath tryProjectRelativePath(IProject project, String resourceLocation) {
    if(resourceLocation == null) {
      return null;
    }
    IPath projectLocation = project.getLocation();
    IPath directory = Path.fromOSString(resourceLocation); // this is an absolute path!
    if(projectLocation == null || !projectLocation.isPrefixOf(directory)) {
      return directory;
    }
    return directory.removeFirstSegments(projectLocation.segmentCount()).makeRelative().setDevice(null);
  }
  
  
  public static boolean hasChanged(IVirtualReference[] existingRefs, IVirtualReference[] refArray) {
    
    if (existingRefs==refArray) {
      return false;
    }
    if (existingRefs == null || existingRefs.length != refArray.length) {
      return true;
    }
    for (int i=0; i<existingRefs.length;i++){
      IVirtualReference existingRef = existingRefs[i];
      IVirtualReference newRef = refArray[i];
      if ((existingRef.getArchiveName() != null && !existingRef.getArchiveName().equals(newRef.getArchiveName())) ||
          (existingRef.getArchiveName() == null && newRef.getArchiveName() != null) ||
          !existingRef.getReferencedComponent().equals(newRef.getReferencedComponent()) ||
          !existingRef.getRuntimePath().equals(newRef.getRuntimePath())) 
      {
        return true;  
      }
    }
    return false;    
  }
  
  public static boolean hasChanged2(IVirtualReference[] existingRefs, IVirtualReference[] refArray) {
    
    if (existingRefs==refArray) {
      return false;
    }
    if (existingRefs == null || existingRefs.length != refArray.length) {
      return true;
    }
    for (int i=0; i<existingRefs.length;i++){
      IVirtualReference existingRef = existingRefs[i];
      IVirtualReference newRef = refArray[i];
      if (
          !existingRef.getReferencedComponent().equals(newRef.getReferencedComponent()) ||
          !existingRef.getRuntimePath().equals(newRef.getRuntimePath())) 
      {
        return true;  
      }
    }
    return false;    
  }
  
  public static IVirtualReference[] extractHardReferences(IVirtualComponent warComponent, boolean overlays) {
    Map<String, Object> options = new HashMap<String, Object>(1);
    options.put(IVirtualComponent.REQUESTED_REFERENCE_TYPE, DYN_REQUESTED_REFERENCE_TYPE);
    IVirtualReference[] allReferences = warComponent.getReferences(options);
    if (allReferences == null || allReferences.length == 0) {
      return new IVirtualReference[]{};
    }
    
    List<IVirtualReference> selectedRefs = new ArrayList<>();
    for (IVirtualReference ref  : allReferences) {
      IVirtualComponent component = ref.getReferencedComponent();
      if (component != null) {
        if (component instanceof IOverlayVirtualComponent) {
          if (overlays) {
            selectedRefs.add(ref);
          }
        } else {
          if (!overlays) {
            selectedRefs.add(ref);
          }
        }
      }
    }
    
    IVirtualReference[] selectedReferences = new IVirtualReference[selectedRefs.size()]; 
    selectedRefs.toArray(selectedReferences);
    return selectedReferences;
  }

  /**
   * Remove the WTP classpath containers that might conflicts with the Maven Library 
   * classpath container 
   * @param project
   * @throws JavaModelException
   */
  public static void removeWTPClasspathContainer(IProject project) throws JavaModelException {
    IJavaProject javaProject = JavaCore.create(project);
    if(javaProject != null) {
      // remove classpatch container from JavaProject
      ArrayList<IClasspathEntry> newEntries = new ArrayList<>();
      for(IClasspathEntry entry : javaProject.getRawClasspath()) {
      String path = entry.getPath().toString();
        if(!"org.eclipse.jst.j2ee.internal.module.container".equals(path) //$NON-NLS-1$
          && !"org.eclipse.jst.j2ee.internal.web.container".equals(path)) { //$NON-NLS-1$
            newEntries.add(entry);
        }
      }
      javaProject.setRawClasspath(newEntries.toArray(new IClasspathEntry[newEntries.size()]), null);
    }
  }

 /**
  * Adds uninstall actions of facets from the faceted project that conflict with the given facetVersion. 
  */
  public static void removeConflictingFacets(IFacetedProject project, IProjectFacetVersion facetVersion, Set<Action> actions) {
    if (project == null) {
      throw new IllegalArgumentException(Messages.WTPProjectsUtil_Project_Cant_Be_Null);
    }
    if (facetVersion == null) {
      throw new IllegalArgumentException(Messages.WTPProjectsUtil_Facet_Version_Cant_Be_Null);
    }
    if (actions == null) {
      throw new IllegalArgumentException(Messages.WTPProjectsUtil_Actions_Cant_Be_Null);
    }
    for (IProjectFacetVersion existingFacetVersion : project.getProjectFacets()) {
    	if (facetVersion.conflictsWith(existingFacetVersion)
    			&& !containsVersionChange(actions, existingFacetVersion)
    			&& !containsVersionUninstall(actions, existingFacetVersion)){
    		//TODO: in case of version change, we may need to search if the new version is conflicting. What about also of chained modifications?
    			actions.add(new IFacetedProject.Action(IFacetedProject.Action.Type.UNINSTALL, existingFacetVersion, null));
    	}
    }
  }

  /**
   * return if the list of Action contains an uninstall action for the provided facetVersion
   */
  private static boolean containsVersionUninstall(Set<Action> actions, IProjectFacetVersion facetVersion) {
	  return actions.stream()
			  .filter(action -> IFacetedProject.Action.Type.UNINSTALL.equals(action.getType()))
			  .filter(action -> facetVersion.equals(action.getProjectFacetVersion()))
			  .findAny().isPresent();
  }

  /**
   * return if the list of Action contains a version change action for the provided facetVersion
   */
  private static boolean containsVersionChange(Set<Action> actions, IProjectFacetVersion facetVersion) {
	  IProjectFacet projectFacet = facetVersion.getProjectFacet();
	  return actions.stream()
			  .filter(action -> IFacetedProject.Action.Type.VERSION_CHANGE.equals(action.getType()))
			  .filter(action -> projectFacet.equals(action.getProjectFacetVersion().getProjectFacet()))
			  .findAny().isPresent();
  }

  /**
   * @param actions
   * @param project
   * @param facetedProject
   */
  public static void installJavaFacet(Set<Action> actions, IProject project, IFacetedProject facetedProject) {
    IProjectFacetVersion javaFv = JavaFacet.FACET.getVersion(JavaFacetUtil.getCompilerLevel(project));
    if(!facetedProject.hasProjectFacet(JavaFacet.FACET)) {
      actions.add(new IFacetedProject.Action(IFacetedProject.Action.Type.INSTALL, javaFv, null));
    } else if(!facetedProject.hasProjectFacet(javaFv)) {
      actions.add(new IFacetedProject.Action(IFacetedProject.Action.Type.VERSION_CHANGE, javaFv, null));
    } 
  }

  /**
   * @param project
   * @return
   */
  public static boolean hasWebFragmentFacet(IProject project) {
    return FacetedProjectUtilities.isProjectOfType(project, WTPProjectsUtil.WEB_FRAGMENT_FACET.getId());
  }

  /**
   * @param mavenProject
   * @return
   */
  public static boolean isQualifiedAsWebFragment(IMavenProjectFacade facade) {
    return WebFragmentUtil.isQualifiedAsWebFragment(facade);
  }
  
  /**
   * Return the project's classes folder, a.k.a. output build directory
   * @param facade
   * @return the project's classes folder
   */
  public static IFolder getClassesFolder(IMavenProjectFacade facade) {
    final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    IFolder output = root.getFolder(facade.getOutputLocation());
    return output;
  }


  public static void removeTestFolderLinks(IProject project, MavenProject mavenProject, IProgressMonitor monitor,
      String folder) throws CoreException {
    IVirtualComponent component = ComponentCore.createComponent(project);
    if (component == null){
      return;
    }
    IVirtualFolder jsrc = component.getRootFolder().getFolder(folder);
    for(IPath location : MavenProjectUtils.getSourceLocations(project, mavenProject.getTestCompileSourceRoots())) {
      if (location == null) {
		continue;
	}
      jsrc.removeLink(location, 0, monitor);
    }
    for(IPath location : MavenProjectUtils.getResourceLocations(project, mavenProject.getTestResources())) {
      if (location == null) {
		continue;
	}
      jsrc.removeLink(location, 0, monitor);
    }

    //MECLIPSEWTP-217 : exclude other test source folders, added by build-helper for instance
    if (project.hasNature(JavaCore.NATURE_ID)) {
      IJavaProject javaProject = JavaCore.create(project);
      if (javaProject == null) {
        return;
      }
      IPath testOutputDirPath = MavenProjectUtils.getProjectRelativePath(project, mavenProject.getBuild().getTestOutputDirectory());
      if (testOutputDirPath == null) {
        return;
      }
      IPath testPath = project.getFullPath().append(testOutputDirPath);
      IClasspathEntry[] cpes = javaProject.getRawClasspath();
      IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
      for (IClasspathEntry cpe : cpes) {
        if (cpe != null && cpe.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
          IPath outputLocation = cpe.getOutputLocation();
          if (testPath.equals(outputLocation)) {
            IPath sourcePath = root.getFolder(cpe.getPath()).getProjectRelativePath();
            if (sourcePath != null) {
              jsrc.removeLink(sourcePath, 0, monitor);
            }
          }
        }
      }
    }
  }


  public static void setNonDependencyAttributeToContainer(IProject project, IProgressMonitor monitor) throws JavaModelException {
    updateContainerAttributes(project, NONDEPENDENCY_ATTRIBUTE, IClasspathDependencyConstants.CLASSPATH_COMPONENT_DEPENDENCY, monitor);
  }

  public static void updateContainerAttributes(IProject project, IClasspathAttribute attributeToAdd, String attributeToDelete, IProgressMonitor monitor)
  throws JavaModelException {
    IJavaProject javaProject = JavaCore.create(project);
    if (javaProject == null) {
		return;
	}
    IClasspathEntry[] cp = javaProject.getRawClasspath();
    for(int i = 0; i < cp.length; i++ ) {
      if(IClasspathEntry.CPE_CONTAINER == cp[i].getEntryKind()
          && MavenClasspathHelpers.isMaven2ClasspathContainer(cp[i].getPath())) {
        LinkedHashMap<String, IClasspathAttribute> attrs = new LinkedHashMap<>();
        for(IClasspathAttribute attr : cp[i].getExtraAttributes()) {
          if (!attr.getName().equals(attributeToDelete)) {
            attrs.put(attr.getName(), attr);            
          }
        }
        attrs.put(attributeToAdd.getName(), attributeToAdd);
        IClasspathAttribute[] newAttrs = attrs.values().toArray(new IClasspathAttribute[attrs.size()]);
        cp[i] = JavaCore.newContainerEntry(cp[i].getPath(), cp[i].getAccessRules(), newAttrs, cp[i].isExported());
        break;
      }
    }
    javaProject.setRawClasspath(cp, monitor);
  }


  /**
   * Add the ModuleCoreNature to a project, if necessary.
   * 
   * @param project An accessible project.
   * @param monitor A progress monitor to track the time to completion
   * @throws CoreException if the ModuleCoreNature cannot be added
   */
  public static void fixMissingModuleCoreNature(IProject project, IProgressMonitor monitor) throws CoreException {
    //MECLIPSEWTP-41 Fix the missing moduleCoreNature
    if (null == ModuleCoreNature.addModuleCoreNatureIfNecessary(project, monitor)) {
      //If we can't add the missing nature, then the project is useless, so let's tell the user
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, NLS.bind(Messages.WTPProjectsUtil_Unable_To_Add_ModuleCoreNature,project.getName()),null));
    }
  }


  /**
   * @return true if the Maven project associated ArtifactHandler's language is java.
   */
  public static boolean isJavaProject(IMavenProjectFacade facade) {
    //Java rocks ... not
    if (facade == null 
     || facade.getMavenProject() == null
     || facade.getMavenProject().getArtifact() == null
     || facade.getMavenProject().getArtifact().getArtifactHandler() == null) {
      return false;
    }
    String language = facade.getMavenProject().getArtifact().getArtifactHandler().getLanguage();
    return "java".equals(language); //$NON-NLS-1$
  }
  
  /**
   * Sets the default deployment descriptor folder for Eclipse > Indigo
   */
  public static void setDefaultDeploymentDescriptorFolder(IVirtualFolder folder, IPath aProjectRelativeLocation, IProgressMonitor monitor) {
      IPath currentDefaultLocation = J2EEModuleVirtualComponent.getDefaultDeploymentDescriptorFolder(folder);
      if (aProjectRelativeLocation.equals(currentDefaultLocation)) {
        return;
      }
      J2EEModuleVirtualComponent.setDefaultDeploymentDescriptorFolder(folder, aProjectRelativeLocation, monitor);
  }

  
  /**
   * Gets the default deployment descriptor folder's relative path. 
   * @return  An IFolder representing the default folder for deployment descriptor, or null if 
   *          the default folder is the root of the project.
   */
  public static IFolder getDefaultDeploymentDescriptorFolder(IVirtualFolder vFolder) {
    IPath defaultPath = J2EEModuleVirtualComponent.getDefaultDeploymentDescriptorFolder(vFolder);
    IFolder folder = null;
    IVirtualComponent component = vFolder.getComponent();
    if (defaultPath == null) {
      IContainer container = vFolder.getUnderlyingFolder();
      if (container instanceof IFolder) {
		folder = (IFolder)container;
	  }
    } else if (!defaultPath.isRoot()){
        folder = component.getProject().getFolder(defaultPath);
    }    
    return folder;
  }  
  
  public static boolean isWTPProject(IProject project) {
    return ModuleCoreNature.getModuleCoreNature(project) != null;
  }
  
  public static boolean isM2eWtpDisabled(IMavenProjectFacade facade, IProgressMonitor monitor) {
	if (facade == null) {
	  return true;
	}
	MavenProject mavenProject;
	Object pomActivationValue = null; 
	try {
		mavenProject = facade.getMavenProject(monitor);
		pomActivationValue = mavenProject == null? null : mavenProject.getProperties().get(M2E_WTP_ACTIVATION_PROPERTY);
	} catch (CoreException ex) {
		// TODO Auto-generated catch block
		LOG.error("Unable to load Maven project",ex); //$NON-NLS-1$
	}
	boolean enabled;
	if (pomActivationValue == null) {
	  enabled = MavenWtpPlugin.getDefault().getMavenWtpPreferencesManager().getPreferences(facade.getProject()).isEnabled();
	} else {
	  enabled = Boolean.parseBoolean(pomActivationValue.toString());
	}	
	return !enabled;
  }


  public static boolean isLastConfigurator(IMavenProjectFacade facade, Class<? extends AbstractProjectConfigurator> clazz, String id) {
	String  lastConfigurator = null;
	Map<String, AbstractProjectConfigurator> configurators = LifecycleMappingFactory.getProjectConfigurators(facade);
	for (Map.Entry<String, AbstractProjectConfigurator> e : configurators.entrySet()) {
		if (e.getValue().getClass().equals(clazz)){
			lastConfigurator = e.getKey();
		}
	}
	return id.equals(lastConfigurator);
  } 
  

  public static void configureWtpUtil(IMavenProjectFacade facade, IProgressMonitor monitor) throws CoreException {
    // Adding utility facet on JEE projects is not allowed
    IProject project = facade.getProject();
    MavenProject mavenProject = facade.getMavenProject();
    if(  !WTPProjectsUtil.isJavaProject(facade)
       || WTPProjectsUtil.isJavaEEProject(project)
       || WebFragmentUtil.isQualifiedAsWebFragment(facade)) {
      return;
    }

    //MECLIPSEWTP-66 delete extra MANIFEST.MF
    List<IPath> sourceRoots = MavenProjectUtils.getSourceLocations(project, mavenProject.getCompileSourceRoots());
    List<IPath> resourceRoots = MavenProjectUtils.getResourceLocations(project, mavenProject.getResources());

    //MECLIPSEWTP-182 check if the Java Project configurator has been successfully run before doing anything :
    if (!checkJavaConfiguration(project, sourceRoots, resourceRoots)) {
      LOG.warn(NLS.bind(Messages.AbstractProjectConfiguratorDelegate_Error_Inconsistent_Java_Configuration, project.getName()));
      return;
    }

    boolean isDebugEnabled = DebugUtilities.isDebugEnabled();
    if (isDebugEnabled) {
      DebugUtilities.debug(DebugUtilities.dumpProjectState("Before configuration ",project)); //$NON-NLS-1$
    }

    // 2 - check if the manifest already exists, and its parent folder

    IFacetedProject facetedProject = ProjectFacetsManager.create(project, true, monitor);
    Set<Action> actions = new LinkedHashSet<>();
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
      DebugUtilities.debug(DebugUtilities.dumpProjectState("after configuration ",project)); //$NON-NLS-1$
    }
    //MNGECLIPSE-904 remove tests folder links for utility jars
    removeTestFolderLinks(project, mavenProject, monitor, "/"); //$NON-NLS-1$

    //Remove "library unavailable at runtime" warning.
    if (isDebugEnabled) {
      DebugUtilities.debug(DebugUtilities.dumpProjectState("after removing test folders ",project)); //$NON-NLS-1$
    }

    setNonDependencyAttributeToContainer(project, monitor);

    WTPProjectsUtil.removeWTPClasspathContainer(project);
  }

  public static void addFoldersToClean(ResourceCleaner fileCleaner, IMavenProjectFacade facade) {
    for (IPath p : facade.getCompileSourceLocations()) {
        if (p != null) {
          fileCleaner.addFiles(p.append("META-INF/MANIFEST.MF")); //$NON-NLS-1$
          fileCleaner.addFolder(p);
        }
      }
      for (IPath p : facade.getResourceLocations()) {
        if (p != null) {
          fileCleaner.addFiles(p.append("META-INF/MANIFEST.MF")); //$NON-NLS-1$
          fileCleaner.addFolder(p);
        }
      }
      // add default resource folder
      IPath defaultResource = new Path("src/main/resources"); //$NON-NLS-1$
      fileCleaner.addFiles(defaultResource.append("META-INF/MANIFEST.MF")); //$NON-NLS-1$
      fileCleaner.addFolder(defaultResource);

      for (IPath p : facade.getTestCompileSourceLocations()) {
        if (p != null) fileCleaner.addFolder(p);
      }
      for (IPath p : facade.getTestResourceLocations()) {
        if (p != null) fileCleaner.addFolder(p);
      }
  }

  /**
   * Checks the maven source folders are correctly added to the project classpath
   */
  private static boolean checkJavaConfiguration(IProject project, List<IPath> sourceRoots, List<IPath> resourceRoots) throws JavaModelException {
    IJavaProject javaProject = JavaCore.create(project);
    if (javaProject == null) {
      return false;
    }
    IClasspathEntry[] cpEntries = javaProject.getRawClasspath();
    if (cpEntries == null) {
      return false;
    }
    Set<IPath> currentPaths = new HashSet<>();
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

}
