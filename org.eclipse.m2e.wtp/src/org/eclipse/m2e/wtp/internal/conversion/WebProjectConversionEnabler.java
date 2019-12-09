/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *      Red Hat, Inc - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.wtp.internal.conversion;

import org.eclipse.core.resources.IProject;
import org.eclipse.jst.j2ee.project.facet.IJ2EEFacetConstants;
import org.eclipse.m2e.wtp.JEEPackaging;

/**
 * Enables Maven project conversion on Faceted Dynamic Web projects, 
 * i.e having the {@link IJ2EEFacetConstants.DYNAMIC_WEB_FACET} Facet. 
 *
 * @author Fred Bricon
 * @since 0.17.0
 */
public class WebProjectConversionEnabler extends AbstractProjectFacetConversionEnabler {

  public WebProjectConversionEnabler() {
    super(IJ2EEFacetConstants.DYNAMIC_WEB_FACET);
  }
  
  /**
   * Returns the <code>war</code> packaging
   */
  @Override
public String[] getPackagingTypes(IProject project) {
    return new String[]{JEEPackaging.WAR.getName()};
  }
}
