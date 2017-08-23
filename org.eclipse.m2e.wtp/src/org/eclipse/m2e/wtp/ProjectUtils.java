/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;

/**
 * Utility class around {@link IProject}
 * 
 * @provisional This class has been added as part of a work in progress. 
 * It is not guaranteed to work or remain the same in future releases. 
 * For more information contact <a href="mailto:m2e-wtp-dev@eclipse.org">m2e-wtp-dev@eclipse.org</a>.
 * 
 * @author Fred Bricon
 */
public class ProjectUtils {

  /**
   * Transform an absolute path into a relative path to a project, if possible
   * @param project
   * @param absolutePath : relative path to the project
   * @return
   */
  public static String getRelativePath(IProject project, String absolutePath){
	//Code copied from org.eclipse.m2e.jdt.internal.AbstractJavaProjectConfigurator 
	//since Path.makeRelativeTo() doesn't seem to work on Linux
    File basedir = project.getLocation().toFile();
    String relative;
    if(absolutePath.equals(basedir.getAbsolutePath())) {
      relative = "."; //$NON-NLS-1$
    } else if(absolutePath.startsWith(basedir.getAbsolutePath())) {
      relative = absolutePath.substring(basedir.getAbsolutePath().length() + 1);
    } else {
      relative = absolutePath;
    }
    return relative.replace('\\', '/');
  }
  

  /**
   * @return the &lt;project&gt;/&lt;buildOutputDir&gt;/m2e-wtp/ folder
   */
  public static IPath getM2eclipseWtpFolder(MavenProject mavenProject, IProject project) {
    return getBuildFolder(mavenProject, project).append(MavenWtpConstants.M2E_WTP_FOLDER);
  }

  /**
   * Returns the project build output folder
   * @since 0.18.0
   */
  public static IPath getBuildFolder(MavenProject mavenProject, IProject project) {
    String buildOutputDir = mavenProject.getBuild().getDirectory();
    String relativeBuildOutputDir = getRelativePath(project, buildOutputDir);
    return new Path(relativeBuildOutputDir);
  }

  
  /**
   * @return the &lt;project&gt;/&lt;buildOutputDir&gt;/m2e-wtp/web-resources folder
   */
  public static IFolder getGeneratedWebResourcesFolder(MavenProject mavenProject, IProject project) {
    IPath m2eWtpFolder = getM2eclipseWtpFolder(mavenProject, project);
    return project.getFolder(m2eWtpFolder).getFolder(MavenWtpConstants.WEB_RESOURCES_FOLDER);
  }

  
  /**
   * Hides and derives &lt;project&gt;/&lt;buildOutputDir&gt;/m2e-wtp/ folder
   */
  public static void hideM2eclipseWtpFolder(MavenProject mavenProject, IProject project) throws CoreException {
    IPath m2eclipseWtpPath = getM2eclipseWtpFolder(mavenProject, project);
    IFolder folder = project.getFolder(m2eclipseWtpPath);
    if (folder.exists()) {
      IProgressMonitor monitor = new NullProgressMonitor();
      if (!folder.isDerived()) {
        folder.setDerived(true);//TODO Eclipse < 3.6 doesn't support setDerived(bool, monitor)
      }
      if (!folder.isHidden()) {
        folder.setHidden(true);
      }
      folder.getParent().refreshLocal(IResource.DEPTH_ZERO,monitor);
    }
  }
  
  /**
   * Creates an IFolder and its parent hierarchy recursively. 
   */
  public static void createFolder(IFolder folder, IProgressMonitor monitor) throws CoreException {
    if (folder == null || folder.exists()) {
      return;
    }
    IContainer parent = folder.getParent();
    if (parent instanceof IFolder) {
      createFolder((IFolder)parent, monitor);
    }
    folder.create(true, true, monitor);
  }
  
  /**
   * Removes the nature of a project.
   */
  public static void removeNature(IProject project, String natureId, IProgressMonitor monitor) throws CoreException {
    if (project.hasNature(natureId)) {
      IProjectDescription description = project.getDescription();
      String[] prevNatures = description.getNatureIds();
      String[] newNatures = new String[prevNatures.length - 1];
      for (int i=0, j = 0 ; i < prevNatures.length; i++) {
        if (!prevNatures[i].equals(natureId)) {
          newNatures[j++] = prevNatures[i];
        }
      }
      description.setNatureIds(newNatures);
      project.setDescription(description, monitor);
    }
  }
  
  
  /**
   * Refreshes the projects hierarchy. For example, if the project on
   * which a facet should be installed is 'Parent1/Parent2/Child',
   * then both Parent1, Parent2 and Child are refreshed.
   * 
   * @param basedir : the base directory (absolute file system path) of the (child) project to refresh.
   * @param refreshDepth: the refresh depth
   * @param monitor : the progress monitor
   * @return the number of projects that were refreshed
   * @throws CoreException
   *             in case of problem during refresh
   * @see IResource for depth values.
   * 
   * @author Xavier Coulon
   * @author Fred Bricon
   */
  public static int refreshHierarchy(File basedir, int refreshDepth, IProgressMonitor monitor) throws CoreException {
    try {
      final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
      final Set<IProject> projects = new LinkedHashSet<>();
      
      IContainer[] containers = root.findContainersForLocationURI(basedir.toURI());
      for (IContainer container : containers) {
        if (monitor.isCanceled()) {
          return 0;
        }
        collectProjects(container, projects);
      }

      for (IProject p : projects) {
         if (monitor.isCanceled()) {
           break;
         }
         if (p.isAccessible()) {
           p.refreshLocal(refreshDepth, monitor);
         }
      }
      return projects.size();
    } finally {
      monitor.done();
    }
  }

  private static void collectProjects(IContainer c, Set<IProject> projects) {
    if (c == null) {
      return;
    }
    if (c instanceof IProject) {
      projects.add((IProject)c);
    } else if (c instanceof IFolder) {
      IFolder f = (IFolder) c;
      if (f.getProject() != null) {
        projects.add(f.getProject());
      }
      collectProjects(f.getParent(), projects);
    }
  }

   /**
   * Returns the underlying file for a given path 
   * @param project
   * @param path, ex. WEB-INF/web.xml
   * @return the underlying file corresponding to path, or null if no file exists.
   */
  public static IFile getWebResourceFile(IProject project, String path) {
      IVirtualComponent component = ComponentCore.createComponent(project);
      if (component == null) {
       return null;
      }
      IPath filePath = new Path(path);
      IContainer[] underlyingFolders = component.getRootFolder().getUnderlyingFolders();
      for (IContainer underlyingFolder : underlyingFolders) {
        IPath p = underlyingFolder.getProjectRelativePath().append(filePath);
        IFile f = project.getFile(p);
        if (f.exists()) {
         return f;
        }
      }
      return null;
  }
}
