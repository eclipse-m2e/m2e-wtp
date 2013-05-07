/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp.internal.facets;

import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.eclipse.wst.common.componentcore.resources.IVirtualResource;
import org.eclipse.wst.common.project.facet.core.IDelegate;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;


/**
 * WAR uninstall delegate
 * 
 * @author Eugene Kuleshov
 */
public class WarUninstallDelegate implements IDelegate {

  @Override
public void execute(IProject project, IProjectFacetVersion fv, Object config, IProgressMonitor monitor)
      throws CoreException {
    // remove web containers
    ArrayList<IClasspathEntry> entries = new ArrayList<IClasspathEntry>();

    IClasspathEntry[] cp = JavaCore.create(project).getRawClasspath();
    for(IClasspathEntry entry : cp) {
      String segment = entry.getPath().segment(0);
      if(!"org.eclipse.jst.j2ee.internal.web.container".equals(segment) //$NON-NLS-1$
          && !"org.eclipse.jst.j2ee.internal.module.container".equals(segment)) { //$NON-NLS-1$
        entries.add(entry);
      }
    }

    if(entries.size() < cp.length) {
      JavaCore.create(project).setRawClasspath(entries.toArray(new IClasspathEntry[entries.size()]), monitor);
    }
    
    IVirtualComponent c = ComponentCore.createComponent(project);
    IVirtualFolder rootFolder = c.getRootFolder();
    
    IPath runtimePath = rootFolder.getRuntimePath();
    rootFolder.removeLink(runtimePath, IVirtualResource.FORCE, monitor);
    
    rootFolder.delete(IVirtualResource.IGNORE_UNDERLYING_RESOURCE, monitor);
  }

}
