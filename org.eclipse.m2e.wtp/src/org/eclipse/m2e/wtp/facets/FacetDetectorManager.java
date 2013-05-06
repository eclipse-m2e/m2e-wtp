/*************************************************************************************
 * Copyright (c) 2011-2013 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Fred Bricon (Red Hat, Inc.) - initial API and implementation
 ************************************************************************************/
package org.eclipse.m2e.wtp.facets;

import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central class used to identify project Facets based on contributed {@link AbstractFacetDetector}s
 *
 * @author Fred Bricon
 * @since 0.18.0
 */
public class FacetDetectorManager {
  
  private static final Logger LOG = LoggerFactory.getLogger(FacetDetectorManager.class); 
  
  private static final FacetDetectorManager instance = new FacetDetectorManager(); 
  
  private Map<String, List<AbstractFacetDetector>> facetDetectors = null;
  
  private FacetDetectorManager() {
  }
  
  /**
   * @return a singleton instance of {@link FacetDetectorManager} 
   */
  public static FacetDetectorManager getInstance() {
    return instance;
  }
  
  /**
   * Inspects the given project to determine the {@link IProjectFacetVersion} of the given facetId. 
   * Facet detection is delegated to contributed {@link AbstractFacetDetector} strategies. 
   * If a detector fails to detect the corresponding Facet version or throws an error, the next detector is invoked, 
   * in order of its priority.
   * 
   * @param mavenProjectFacade an {@link IMavenProjectFacade} instance
   * @param facetId the id of the {@link IProjectFacet} to look for.
   * @param monitor a progress monitor, can be <code>null</code>;
   * @return an {@link IProjectFacetVersion} corresponding the facetId
   * @throws CoreException
   */
  public IProjectFacetVersion findFacetVersion(IMavenProjectFacade mavenProjectFacade, String facetId, IProgressMonitor monitor) throws CoreException {
    return findFacetVersion(mavenProjectFacade, facetId, null, monitor);
  }
  
  /**
   * Inspects the given project to determine the {@link IProjectFacetVersion} of the given facetId. 
   * Facet detection is delegated to contributed {@link AbstractFacetDetector} strategies. 
   * If a detector fails to detect the corresponding Facet version or throws an error, the next detector is invoked, 
   * in order of its priority. An optional context map can be used by the different detectors to determine the facet version.  
   * 
   * @param mavenProjectFacade an {@link IMavenProjectFacade} instance
   * @param facetId the id of the {@link IProjectFacet} to look for.
   * @param context an optional context map, can be <code>null</code> 
   * @param monitor a progress monitor, can be <code>null</code>;
   * @return an {@link IProjectFacetVersion} corresponding the facetId
   * @throws CoreException
   */
  public IProjectFacetVersion findFacetVersion(IMavenProjectFacade mavenProjectFacade, String facetId, Map<?, ?> context, IProgressMonitor monitor) throws CoreException {
    if (facetId == null) {
      return null;
    }
    List<AbstractFacetDetector> detectors = getFacetDetectors().get(facetId);
    if (detectors == null || detectors.isEmpty()) {
      return null;
    }
    IProjectFacetVersion version = null;
    for (AbstractFacetDetector detector : detectors) {
      if (monitor != null && monitor.isCanceled()) {
        break;
      }
      try {
        version = detector.findFacetVersion(mavenProjectFacade, context, monitor);
        if (version != null) {
          if (!facetId.equals(version.getProjectFacet().getId())) {
            //throws exception 
          }
          break;
        }
      } catch (CoreException ce) {
        LOG.error(ce.getLocalizedMessage());
      }
    }
    return version;
  }

  private Map<String, List<AbstractFacetDetector>> getFacetDetectors() {
    if (facetDetectors == null) {
      facetDetectors = FacetDetectorExtensionReader.readFacetDetectorExtensions();
    }
    return facetDetectors;
  }

}
