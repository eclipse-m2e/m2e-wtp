/*************************************************************************************
 * Copyright (c) 2011-2012 Red Hat, Inc. and others.
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
package org.eclipse.m2e.wtp.jaxrs.internal;

import org.eclipse.jst.ws.jaxrs.core.internal.IJAXRSCoreConstants;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

/**
 * 
 * @author Fred Bricon
 *
 */
public class MavenJaxRsConstants {
	
	public static final String JAXRS_CONFIGURATION_ERROR_MARKER_ID = "org.eclipse.m2e.core.maven2Problem.wtp.jaxrs.configuration"; //$NON-NLS-1$

	public static final IProjectFacet JAX_RS_FACET; 

	public static final IProjectFacetVersion JAX_RS_FACET_1_0; 
	
	public static final IProjectFacetVersion JAX_RS_FACET_1_1; 
	
	public static final IProjectFacetVersion JAX_RS_FACET_2_0; 
	
	public static final IProjectFacetVersion JAX_RS_FACET_2_1; 

	static {
		JAX_RS_FACET = ProjectFacetsManager.getProjectFacet(IJAXRSCoreConstants.JAXRS_FACET_ID);
		JAX_RS_FACET_1_0 = JAX_RS_FACET.getVersion(IJAXRSCoreConstants.JAXRS_VERSION_1_0);
		JAX_RS_FACET_1_1 = JAX_RS_FACET.getVersion(IJAXRSCoreConstants.JAXRS_VERSION_1_1);
		if (JAX_RS_FACET.hasVersion("2.0")) { //$NON-NLS-1$
			JAX_RS_FACET_2_0 = JAX_RS_FACET.getVersion("2.0"); //$NON-NLS-1$
		} else {
			JAX_RS_FACET_2_0 = JAX_RS_FACET_1_1; 
		}
		if (JAX_RS_FACET.hasVersion("2.1")) { //$NON-NLS-1$
			JAX_RS_FACET_2_1 = JAX_RS_FACET.getVersion("2.1"); //$NON-NLS-1$
		} else {
			JAX_RS_FACET_2_1 = JAX_RS_FACET_2_0; 
		}
	}
	
	/**
	 * Private constructor to prevent instanciation.
	 */
	private MavenJaxRsConstants() {};
}
