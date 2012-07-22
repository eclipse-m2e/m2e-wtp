/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jst.j2ee.project.facet.IAppClientFacetInstallDataModelProperties;
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
 * Application Client Facet version change delegate
 * 
 * @author Fred Bricon
 */
public class AppClientVersionChangeDelegate implements IDelegate {

  private static final Logger LOG = LoggerFactory.getLogger(AppClientVersionChangeDelegate.class);

  public void execute(IProject project, IProjectFacetVersion fv, Object cfg, IProgressMonitor monitor)
      throws CoreException {

    if (cfg == null)  {
      return;
    }
    
    if(monitor != null) {
      monitor.beginTask("Updating Application Client facet version", 1); //$NON-NLS-1$
    }

    try {
      IDataModel model = (IDataModel) cfg;
      
      if(monitor != null) {
        monitor.worked(1);
      }
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
        LOG.error("Unable to notify Application Client version change", e);
      }
    }

    finally {
      if(monitor != null) {
        monitor.done();
      }
    }
  }
}
