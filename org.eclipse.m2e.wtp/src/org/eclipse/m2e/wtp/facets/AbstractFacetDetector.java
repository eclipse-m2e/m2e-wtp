/*************************************************************************************
 * Copyright (c) 2013 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Fred Bricon (Red Hat, Inc.) - initial API and implementation
 ************************************************************************************/
package org.eclipse.m2e.wtp.facets;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;

/**
 * Base class to determine the project Facet
 *
 * @author Fred Bricon
 * @since 0.18.0
 */
public abstract class AbstractFacetDetector implements IExecutableExtension, Comparable<AbstractFacetDetector> {
  
  public static final String ATTR_ID = "id"; //$NON-NLS-1$
  
  public static final String ATTR_FACET_ID = "facetId"; //$NON-NLS-1$
  
  public static final String ATTR_PRIORITY = "priority"; //$NON-NLS-1$
  
  public static final String ATTR_CLASS = "class"; //$NON-NLS-1$
  
  private String id;
  
  private String facetId;
  
  private int priority;
  
  /**
   * Returns the id of this detector
   */
  public String getId() {
    if(id == null) {
      id = getClass().getName();
    }
    return id;
  }
  
  /**
   * returns the Facet Id this detector applies to.
   */
  public String getFacetId() {
    return facetId;
  }
  

  /**
   * Identify the project Facet version for the given maven project facade.
   * 
   * @param mavenProjectFacade
   * @param context - an optional map containing context sensitive data
   * @param monitor a progress monitor
   * @return the {@link IProjectFacetVersion} of the corresponding facetId, or <code>null</code> if not facet could be identified  
   * @throws CoreException
   */
  public abstract IProjectFacetVersion findFacetVersion(IMavenProjectFacade mavenProjectFacade, Map<?, ?> context, IProgressMonitor monitor) throws CoreException;

  /**
   * Returns the priority
   */
  public int getPriority() {
    return priority;
  }
  
  /**
   * Compare detectors priority
   * 
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
public int compareTo(AbstractFacetDetector other) {
    if (other == null) {
      return priority;
    }
    int result = other.priority - priority;
    if (result == 0) {
      return 0;
    }
    return (result > 0)? -1 : 1;
  }

  /**
   * @see org.eclipse.core.runtime.IExecutableExtension#setInitializationData(org.eclipse.core.runtime.IConfigurationElement, java.lang.String, java.lang.Object)
   */
  @Override
public void setInitializationData(IConfigurationElement config, String propertyName, Object data)
      throws CoreException {
      this.id = config.getAttribute(ATTR_ID);
      this.facetId = config.getAttribute(ATTR_FACET_ID);
      try {
        priority = Integer.parseInt(config.getAttribute(ATTR_PRIORITY));
      } catch(Exception e) {
        priority = 100;
      }
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
public String toString() {
    return getId();
  }
}
