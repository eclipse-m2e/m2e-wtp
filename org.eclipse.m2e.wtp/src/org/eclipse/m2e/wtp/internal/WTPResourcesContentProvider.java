/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.wtp.WTPProjectsUtil;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.IPipelinedTreeContentProvider;
import org.eclipse.ui.navigator.PipelinedShapeModification;
import org.eclipse.ui.navigator.PipelinedViewerUpdate;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * WTP resources content provider
 *
 * @author Eugene Kuleshov
 */
public class WTPResourcesContentProvider extends BaseWorkbenchContentProvider implements IPipelinedTreeContentProvider {

  private static final Logger LOG = LoggerFactory.getLogger(WTPResourcesContentProvider.class); 
  
  @Override
  public void init(ICommonContentExtensionSite config) {
  }

  @Override
  public void restoreState(IMemento memento) {
  }

  @Override
  public void saveState(IMemento memento) {
  }

  @Override
  public Object[] getChildren(Object element) {
    if(element instanceof WTPResourcesNode) {
      return ((WTPResourcesNode) element).getResources();
    }
    return super.getChildren(element);
  }

  // IPipelinedTreeContentProvider

  @Override
  @SuppressWarnings("rawtypes")
  public void getPipelinedElements(Object element, Set currentElements) {
  }
  
  @Override
  @SuppressWarnings({"unchecked", "rawtypes"})
  public void getPipelinedChildren(Object parent, Set currentChildren) {
    if (parent instanceof IProject) {
      IProject project = (IProject) parent;
      if(project.isAccessible()) {
        try {
          if (!project.hasNature(IMavenConstants.NATURE_ID)) {
        	  return;
          }
          IFacetedProject facetedProject = ProjectFacetsManager.create(project);//MNGECLIPSE-1992 there's no reason to actually create a ProjectFacet at this point
          if(facetedProject != null && 
              (facetedProject.hasProjectFacet(WTPProjectsUtil.DYNAMIC_WEB_FACET) || 
               facetedProject.hasProjectFacet(WTPProjectsUtil.EAR_FACET))) {
            List newChildren = new ArrayList<Object>(currentChildren.size()+1);
            newChildren.add(new WTPResourcesNode(project));
            newChildren.addAll(currentChildren);
            currentChildren.clear();
            currentChildren.addAll(newChildren);
          }
        } catch(CoreException ex) {
          LOG.error(Messages.WTPResourcesContentProvider_Error_Getting_Pipelined_Children, ex);
        }
      }
    }
  }

  @Override
  public Object getPipelinedParent(Object element, Object suggestedParent) {
    return suggestedParent;
  }

  @Override
  public boolean interceptRefresh(PipelinedViewerUpdate refreshSynchronization) {
    return false;
  }
  
  @Override
  public boolean interceptUpdate(PipelinedViewerUpdate updateSynchronization) {
    return false;
  }
  
  @Override
  public PipelinedShapeModification interceptAdd(PipelinedShapeModification addModification) {
    return addModification;
  }

  @Override
  public PipelinedShapeModification interceptRemove(PipelinedShapeModification removeModification) {
    return removeModification;
  }
  
}

