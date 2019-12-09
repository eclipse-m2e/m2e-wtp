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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

/**
 * Utility class used to delete generated resources.
 * 
 * @provisional This class has been added as part of a work in progress. 
 * It is not guaranteed to work or remain the same in future releases. 
 * For more information contact <a href="mailto:m2e-wtp-dev@eclipse.org">m2e-wtp-dev@eclipse.org</a>.
 * 
 * @author Fred Bricon
 */
public class ResourceCleaner {
  
  private static final Path EMPTY_PATH = new Path(""); //$NON-NLS-1$

  private final IProject project;

  private List<IFolder> folders = new ArrayList<>(); 

  private List<IFile> files = new ArrayList<>(); 
  
  private Set<IFolder> keepers = new HashSet<>();

  /**
   * @param project
   */
  public ResourceCleaner(IProject project) {
    this.project = project;
  }

  public ResourceCleaner(IProject project, IFolder ... foldersToKeep) {
    this.project = project;
    if (foldersToKeep != null) {
      for (IFolder folder : foldersToKeep) {
        if (folder != null) {
          keepers.add(folder);
          IContainer parent = folder.getParent();
          while (parent instanceof IFolder) {
            keepers.add((IFolder)parent);
            parent = parent.getParent();
          }
        }
      }
    }
  }

  
  public void addFolder(IPath folderPath) {
    if (folderPath == null || EMPTY_PATH.equals(folderPath)) {
      return;
    }
    addFolder(project.getFolder(folderPath));
  }

  public void addFolder(IFolder folder) {
    if (folder != null && !folder.exists()) {
      if (!folder.getProject().getFullPath().equals(folder.getFullPath())){
        folders.add(folder);
        addInexistentParentFolders(folder);
      }
    }
  }

  public void addFiles(IPath ... filePaths) {
    if (filePaths == null) {
      return;
    }
    for (IPath fileName : filePaths) {
      IFile fileToDelete = project.getFile(fileName);
      if (!fileToDelete.exists()) {
        files.add(fileToDelete);
        addInexistentParentFolders(fileToDelete);
      }
    }
  }

  public void addFiles(IFile ... filesToDelete) {
    if (filesToDelete == null) {
      return;
    }
    for (IFile fileToDelete : filesToDelete) {
      if (!fileToDelete.exists()) {
        files.add(fileToDelete);
        addInexistentParentFolders(fileToDelete);
      }
    }
  }

  public void cleanUp() throws CoreException {
    IProgressMonitor monitor = new NullProgressMonitor();
    for (IFile file : files) {
      if (file.exists()) {
        file.delete(true, monitor);
      }
    }
    for (IFolder folder : folders) {
      if (folder.exists() && folder.members().length == 0) {
        folder.delete(true, monitor);
      }
    }
  }
  
  protected void addInexistentParentFolders(IResource resource) {
    IContainer parentContainer = resource.getParent();
    while (parentContainer instanceof IFolder) {
      if (keepers.contains(parentContainer) 
          || parentContainer.exists()) {
        break;
      }
      IFolder parent = (IFolder)parentContainer;
      folders.add(parent);
      parentContainer = parentContainer.getParent();
    }
  }
  
}
