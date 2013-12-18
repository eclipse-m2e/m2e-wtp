/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp.internal;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jst.j2ee.project.facet.IJ2EEFacetConstants;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * WTP Resources node
 * 
 * @author Eugene Kuleshov
 */
public class WTPResourcesNode implements IWorkbenchAdapter {

  private static final int HASHCODE_ADDITION = 0x504;
  private static final Logger LOG = LoggerFactory.getLogger(WTPResourcesNode.class);

  private final IProject project;

  public WTPResourcesNode(IProject project) {
    this.project = project;
  }

  public Object[] getResources() {
    IContainer[] folders = getRootFolders();
    if(folders != null && folders.length == 1) {
      try {
        return folders[0].members();
      } catch(CoreException ex) {
        LOG.error(Messages.WTPResourcesNode_Error_Getting_WTP_Resources,ex);
      }
    }

    return folders;
  }

  // IWorkbenchAdapter
  
  @Override
  public String getLabel(Object o) {
//    IContainer[] folders = getRootFolders();
//    StringBuilder label = new StringBuilder(getLabel());
//    if(folders.length == 1) {
//      IContainer c = folders[0];
//      label.append(" : ").append(c.getFullPath().removeFirstSegments(1));
//    }
//    return label.toString();
    return getLabel();
  }

  @Override
  public ImageDescriptor getImageDescriptor(Object object) {
    try {
      IFacetedProject facetedProject = ProjectFacetsManager.create(project);
      if (facetedProject != null && facetedProject.hasProjectFacet(IJ2EEFacetConstants.ENTERPRISE_APPLICATION_FACET)) {
        return WTPResourcesImages.APP_RESOURCES;
      }
    } catch(CoreException ex) {
      LOG.error(Messages.WTPResourcesNode_Cant_Retrieve_Project_Facet,ex);
    }    
    return WTPResourcesImages.WEB_RESOURCES;
  }

  @Override
  public Object getParent(Object o) {
    return project;
  }

  @Override
  public Object[] getChildren(Object o) {
    return getResources();
  }

  // helper methods
  
  private IContainer[] getRootFolders() {
    IVirtualComponent component = ComponentCore.createComponent(project);
    IContainer[] folders = null;
    if (component != null) {
      IVirtualFolder rootFolder = component.getRootFolder();
      folders = rootFolder.getUnderlyingFolders();
    } else {
      folders = new IContainer[0];
    }
    return folders;
  }
  
  public String getLabel() {  
    return Messages.WTPResourcesNode_Deployed_Resources_Label;
  }

  @Override
  public boolean equals(Object o) {
    if( !(o instanceof WTPResourcesNode))
      return false;
    IProject p = ((WTPResourcesNode)o).project;
    return this.project == null ? p == null : this.project.equals(p);
  }
  @Override
  public int hashCode() {
    return HASHCODE_ADDITION + (project == null ? 0 : project.hashCode());
  }
  
}
