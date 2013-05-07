/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp.internal.facets;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jst.j2ee.project.facet.IAppClientFacetInstallDataModelProperties;
import org.eclipse.jst.j2ee.project.facet.IJ2EEFacetConstants;
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
 * Application Client Facet version change listener
 * 
 * @author Fred Bricon
 */
public class AppClientVersionChangeListener implements IFacetedProjectListener {

  private static final Logger LOG = LoggerFactory.getLogger(AppClientVersionChangeListener.class);

  /* (non-Javadoc)
   * @see org.eclipse.wst.common.project.facet.core.events.IFacetedProjectListener#handleEvent(org.eclipse.wst.common.project.facet.core.events.IFacetedProjectEvent)
   */
  @Override
public void handleEvent(IFacetedProjectEvent event) {
    if (event.getType().equals(IFacetedProjectEvent.Type.POST_VERSION_CHANGE)) {
      IProject project = ((IProjectFacetActionEvent) event).getProject().getProject();
      //The action applies if the Project has Maven nature and AppClient facet
      try {
        if(project.hasNature(IMavenConstants.NATURE_ID)){
          if(((IProjectFacetActionEvent) event)
              .getProjectFacet().getId().equals(IJ2EEFacetConstants.APPLICATION_CLIENT)){

            NullProgressMonitor monitor = new NullProgressMonitor();
            Object cfg = ((IProjectFacetActionEvent) event).getActionConfig();

            if(cfg == null)
              return;

            IDataModel model = (IDataModel) cfg;

            if(model.isProperty(IAppClientFacetInstallDataModelProperties.CONFIG_FOLDER)){
              IPath appClientContent = new Path("/" + model.getStringProperty(IAppClientFacetInstallDataModelProperties.CONFIG_FOLDER));//$NON-NLS-1$
              if (!WTPProjectsUtil.hasLink(project, new Path("/"), appClientContent, monitor)) {//$NON-NLS-1$
                final IVirtualComponent c = ComponentCore.createComponent(project, true);
                if (c != null) {
                  final IVirtualFolder root = c.getRootFolder();
                  root.createLink(appClientContent , 0, null); 
                }
              }
              try {
                ((IDataModelOperation) model.getProperty(FacetDataModelProvider.NOTIFICATION_OPERATION)).execute(monitor, null);
              } catch(ExecutionException e) {
                LOG.error(Messages.AppClientVersionChangeListener_Error_Notifying_Application_Client_Version_Change, e);
              } 
            }
          }
        }
      } catch(CoreException e) {
        LOG.error(Messages.VersionChangeListener_Unreadeable_Project_Nature, e);
      }
    }
  }
}