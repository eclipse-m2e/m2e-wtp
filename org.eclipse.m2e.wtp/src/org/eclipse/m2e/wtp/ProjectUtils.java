/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp;

import java.io.File;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

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
      relative = ".";
    } else if(absolutePath.startsWith(basedir.getAbsolutePath())) {
      relative = absolutePath.substring(basedir.getAbsolutePath().length() + 1);
    } else {
      relative = absolutePath;
    }
    return relative.replace('\\', '/'); //$NON-NLS-1$ //$NON-NLS-2$
  }
  

  /**
   * @return the &lt;project&gt;/&lt;buildOutputDir&gt;/m2e-wtp/ folder
   */
  public static IPath getM2eclipseWtpFolder(MavenProject mavenProject, IProject project) {
    String buildOutputDir = mavenProject.getBuild().getDirectory();
    String relativeBuildOutputDir = getRelativePath(project, buildOutputDir);
    return new Path(relativeBuildOutputDir).append(MavenWtpConstants.M2E_WTP_FOLDER);
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
  
}
