/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp.facets;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jst.j2ee.project.facet.IJ2EEModuleFacetInstallDataModelProperties;
import org.eclipse.jst.j2ee.web.project.facet.IWebFacetInstallDataModelProperties;
import org.eclipse.m2e.wtp.WTPProjectsUtil;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.datamodel.FacetDataModelProvider;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.frameworks.datamodel.IDataModelOperation;
import org.eclipse.wst.common.project.facet.core.IDelegate;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * War update delegate. Based off the org.eclipse.jst.j2ee.web.project.facet.WebFacetInstallDelegate
 * 
 * @author Fred Bricon
 */
public class WarVersionChangeDelegate implements IDelegate {

  private static final Logger LOG = LoggerFactory.getLogger(WarVersionChangeDelegate.class);

  public void execute(IProject project, IProjectFacetVersion fv, Object cfg, IProgressMonitor monitor)
      throws CoreException {

    if (monitor != null) {
      monitor.beginTask("Updating Dynamic Web facet to "+fv, 1); //$NON-NLS-1$
    }

    try {
      final IDataModel model = (IDataModel) cfg;

      final IVirtualComponent c = ComponentCore.createComponent(project, true);
      if (c == null) {
        return;
      }
      
      try {
        if (model != null) {

          final IWorkspace ws = ResourcesPlugin.getWorkspace();
          final IPath pjpath = project.getFullPath();

          final IPath contentdir = setContentPropertyIfNeeded(model, pjpath, project);
          mkdirs(ws.getRoot().getFolder(contentdir), monitor);
          IVirtualFolder contentRootFolder = c.getRootFolder();
          WTPProjectsUtil.setDefaultDeploymentDescriptorFolder(contentRootFolder, contentdir, monitor);
          
          String contextRoot = model.getStringProperty(IWebFacetInstallDataModelProperties.CONTEXT_ROOT);
          setContextRootPropertyIfNeeded(c, contextRoot);

          
          IDataModelOperation notificationOperation = ((IDataModelOperation) model.getProperty(FacetDataModelProvider.NOTIFICATION_OPERATION));
          if (notificationOperation != null) {
            notificationOperation.execute(monitor, null);
          }
        }
      } catch (ExecutionException e) {
        LOG.error("Unable to notify Dynamic Web version change", e);
      }
      
      if (monitor != null) {
        monitor.worked(1);
      }
    } finally {
      if (monitor != null) {
        monitor.done();
      }
    }
  }

  
  private static void mkdirs(final IFolder folder, IProgressMonitor monitor) throws CoreException {
    if (!folder.exists()) {
      if (folder.getParent() instanceof IFolder) {
        mkdirs((IFolder) folder.getParent(), monitor);
      }
      folder.create(true, true, null);
    }
    else
    {
        IContainer x = folder;
        while( x instanceof IFolder && x.isDerived() )
        {
            x.setDerived( false, monitor);
            x = x.getParent();
        }
    }
  }

  private void setContextRootPropertyIfNeeded(final IVirtualComponent c, String contextRoot) {
    String existing = c.getMetaProperties().getProperty("context-root"); //$NON-NLS-1$
    if (existing == null)
      c.setMetaProperty("context-root", contextRoot); //$NON-NLS-1$
  }

  private IPath setContentPropertyIfNeeded(final IDataModel model, final IPath pjpath, IProject project) {
    IVirtualComponent c = ComponentCore.createComponent(project, false);
    if (c.exists()) {
      if( !c.getRootFolder().getProjectRelativePath().isRoot() ){
        return c.getRootFolder().getUnderlyingResource().getFullPath();
      }
    }
    return pjpath.append(model.getStringProperty(IJ2EEModuleFacetInstallDataModelProperties.CONFIG_FOLDER));
  }
}
