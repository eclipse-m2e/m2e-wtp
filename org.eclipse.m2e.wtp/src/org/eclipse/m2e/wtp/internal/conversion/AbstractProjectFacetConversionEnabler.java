/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *      Red Hat, Inc - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.wtp.internal.conversion;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.m2e.core.project.conversion.AbstractProjectConversionEnabler;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract Project Conversion Enabler that accepts a {@link IProject} based on the presence of an {@link IProjectFacet} 
 * on the given project.
 *
 * @author Fred Bricon
 * @since 0.17.0
 */
public abstract class AbstractProjectFacetConversionEnabler extends AbstractProjectConversionEnabler {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractProjectFacetConversionEnabler.class); 
  
  private IProjectFacet requiredFacet;

  public AbstractProjectFacetConversionEnabler(IProjectFacet requiredFacet) {
    this.requiredFacet = requiredFacet;
  }
  
  /**
   * Checks the project has the required {@link IProjectFacet}
   */
  public boolean accept(IProject project) {
    if (project != null) {
      IFacetedProject facetedProject;
      try {
        facetedProject = ProjectFacetsManager.create(project);
        return facetedProject != null && facetedProject.hasProjectFacet(requiredFacet);
      } catch(CoreException ex) {
        LOG.error("Can not accept "+ project.getName(), ex);
      }
    }
    return false;
  }
  
  

}
