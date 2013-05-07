/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Fred Bricon (Red Hat, Inc.) - initial API and implementation
 ******************************************************************************/
package org.eclipse.m2e.wtp.jsf.internal;

import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Fred Bricon
 */
public class MavenJSFConstants {
	
	public static final String JSF_CONFIGURATION_ERROR_MARKER_ID = "org.eclipse.m2e.core.maven2Problem.wtp.jsf.configuration"; //$NON-NLS-1$

	public static final String JSF_VERSION_2_2 = "2.2"; //$NON-NLS-1$
	
	public static final String JSF_VERSION_2_1 = "2.1"; //$NON-NLS-1$

	public static final String JSF_VERSION_2_0 = "2.0"; //$NON-NLS-1$
	
	public static final String JSF_VERSION_1_2 = "1.2"; //$NON-NLS-1$
	
	public static final String JSF_VERSION_1_1 = "1.1"; //$NON-NLS-1$
	
	public static final IProjectFacet JSF_FACET;
	
	public static final IProjectFacetVersion JSF_FACET_VERSION_2_2;

	public static final IProjectFacetVersion JSF_FACET_VERSION_2_1;
	
	public static final IProjectFacetVersion JSF_FACET_VERSION_2_0;
	
	public static final IProjectFacetVersion JSF_FACET_VERSION_1_2;
	
	public static final IProjectFacetVersion JSF_FACET_VERSION_1_1;

	private static final Logger LOG = LoggerFactory.getLogger(MavenJSFConstants.class);
	
	static {
		JSF_FACET = ProjectFacetsManager.getProjectFacet("jst.jsf"); //$NON-NLS-1$
		JSF_FACET_VERSION_2_0 = JSF_FACET.getVersion(JSF_VERSION_2_0); 
		JSF_FACET_VERSION_1_2 = JSF_FACET.getVersion(JSF_VERSION_1_2); 
		JSF_FACET_VERSION_1_1 = JSF_FACET.getVersion(JSF_VERSION_1_1);
		
		IProjectFacetVersion jsf21Version = null;
		try {
			jsf21Version = JSF_FACET.getVersion(JSF_VERSION_2_1); 
		} catch (Exception e) {
			LOG.warn(Messages.MavenJSFConstants_Warning_JSF21_Unavailable);
			jsf21Version = JSF_FACET_VERSION_2_0; 
		}
		JSF_FACET_VERSION_2_1 = jsf21Version;

		IProjectFacetVersion jsf22Version = null;
		try {
			jsf22Version = JSF_FACET.getVersion(JSF_VERSION_2_2); 
		} catch (Exception e) {
			LOG.warn(NLS.bind(Messages.MavenJSFConstants_Warning_JSF22_Unavailable, JSF_FACET_VERSION_2_1.getVersionString()));
			jsf22Version = JSF_FACET_VERSION_2_1; 
		}
		JSF_FACET_VERSION_2_2 = jsf22Version;
	}
	
	
	/**
	 * Private constructor to prevent instantiation.
	 */
	private MavenJSFConstants() {};
	
}
