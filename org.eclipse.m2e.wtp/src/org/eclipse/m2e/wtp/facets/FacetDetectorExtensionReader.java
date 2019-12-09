/*************************************************************************************
 * Copyright (c) 2013 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Fred Bricon (Red Hat, Inc.) - initial API and implementation
 ************************************************************************************/
package org.eclipse.m2e.wtp.facets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.m2e.wtp.internal.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Extension reader used to instanciate {@link AbstractFacetDetector}s from plugin.xml extensions
 * 
 * @author Fred Bricon
 * @since 0.18.0
 */
/* package */ class FacetDetectorExtensionReader {

  private static final Logger LOG = LoggerFactory.getLogger(FacetDetectorExtensionReader.class);

  private static final String EXTENSION_FACET_DETECTORS = "org.eclipse.m2e.wtp.facetDetectors"; //$NON-NLS-1$

  private static final Object ELEMENT_FACET_DETECTOR = "facetDetector"; //$NON-NLS-1$

  public static synchronized Map<String, List<AbstractFacetDetector>> readFacetDetectorExtensions() {
    
    Map<String, List<AbstractFacetDetector>> map = new HashMap<String, List<AbstractFacetDetector>>();

    IExtensionRegistry registry = Platform.getExtensionRegistry();
    IExtensionPoint configuratorsExtensionPoint = registry.getExtensionPoint(EXTENSION_FACET_DETECTORS);
    if(configuratorsExtensionPoint != null) {
      IExtension[] configuratorExtensions = configuratorsExtensionPoint.getExtensions();
      for(IExtension extension : configuratorExtensions) {
        IConfigurationElement[] elements = extension.getConfigurationElements();
        for(IConfigurationElement element : elements) {
          if(element.getName().equals(ELEMENT_FACET_DETECTOR)) {
            try {
              String facetId = element.getAttribute(AbstractFacetDetector.ATTR_FACET_ID);
              Object o = element.createExecutableExtension(AbstractFacetDetector.ATTR_CLASS);
              AbstractFacetDetector facetDetector = (AbstractFacetDetector) o;
              List<AbstractFacetDetector> detectors = map.get(facetId);
              if (detectors == null){
                detectors = new ArrayList<>();
                map.put(facetId, detectors);
              }
              detectors.add(facetDetector);
            } catch(CoreException ex) {
              LOG.error(Messages.FacetDetectorExtensionReader_Error_Configuring_Facet_Detector, ex);
            }
          }
        }
      }
    }
    //Sort each detector list
    for ( List<AbstractFacetDetector> detectors : map.values()) {
      Collections.sort(detectors);
    }
    return map;
  }
}
