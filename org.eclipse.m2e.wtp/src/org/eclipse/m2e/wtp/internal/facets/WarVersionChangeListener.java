/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp.internal.facets;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jst.j2ee.project.facet.IJ2EEFacetConstants;
import org.eclipse.jst.j2ee.project.facet.IJ2EEModuleFacetInstallDataModelProperties;
import org.eclipse.jst.j2ee.web.project.facet.IWebFacetInstallDataModelProperties;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.wtp.WTPProjectsUtil;
import org.eclipse.m2e.wtp.internal.Messages;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.datamodel.FacetDataModelProvider;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.frameworks.datamodel.IDataModelOperation;
import org.eclipse.wst.common.project.facet.core.events.IFacetedProjectEvent;
import org.eclipse.wst.common.project.facet.core.events.IFacetedProjectListener;
import org.eclipse.wst.common.project.facet.core.events.IProjectFacetActionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * War Facet update Listener. Based off the org.eclipse.jst.j2ee.web.project.facet.WebFacetInstallDelegate
 * 
 * @author Fred Bricon
 */
public class WarVersionChangeListener implements IFacetedProjectListener {

  private static final Logger LOG = LoggerFactory.getLogger(WarVersionChangeListener.class);

  /* (non-Javadoc)
   * @see org.eclipse.wst.common.project.facet.core.events.IFacetedProjectListener#handleEvent(org.eclipse.wst.common.project.facet.core.events.IFacetedProjectEvent)
   */
  @Override
public void handleEvent(IFacetedProjectEvent event) {
    if (event.getType().equals(IFacetedProjectEvent.Type.POST_VERSION_CHANGE)) {
      IProject project = ((IProjectFacetActionEvent) event).getProject().getProject();
      //The action applies if the Project has Maven nature and web facet
      try {
        if(project.hasNature(IMavenConstants.NATURE_ID)){
          if(((IProjectFacetActionEvent) event)
              .getProjectFacet().getId().equals(IJ2EEFacetConstants.DYNAMIC_WEB)){

            NullProgressMonitor monitor = new NullProgressMonitor();
            Object cfg = ((IProjectFacetActionEvent) event).getActionConfig();

            if(cfg == null)
              return;

            IDataModel model = (IDataModel) cfg;

            final IVirtualComponent c = ComponentCore.createComponent(project, true);

            if (c == null)
              return;

            try{
              if (model != null) {
                //The model could not provide us the property we require
                if(model.isProperty(IWebFacetInstallDataModelProperties.CONTEXT_ROOT)){
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
              }
            }catch (ExecutionException e) {
              LOG.error(Messages.WarVersionChangeListener_Error_Notifying_WebApp_Version_Change, e);
            }
          }
        }
      }catch(CoreException e) {
          LOG.error(Messages.VersionChangeListener_Unreadeable_Project_Nature, e);
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
