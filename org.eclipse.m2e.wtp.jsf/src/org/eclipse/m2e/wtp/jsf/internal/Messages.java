/*******************************************************************************
 * Copyright (c) 2011-2012 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Snjezana Peco (Red Hat, Inc.) - initial API and implementation
 ******************************************************************************/
package org.eclipse.m2e.wtp.jsf.internal;

import org.eclipse.osgi.util.NLS;

/**
 * 
 * @author snjeza
 *
 */
public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.m2e.wtp.jsf.internal.messages"; //$NON-NLS-1$
	public static String JSFAppConfigUtils_Error_Reading_WebXml;
	public static String JSFProjectConfigurator_Marker_Facet_Version_Cant_Be_Installed;
	public static String JSFProjectConfigurator_The_project_does_not_contain_the_Web_Module_facet;
	public static String JSFUtils_Error_Finding_Faces_Servlet;
	public static String JSFUtils_Error_Finding_Faces_Servlet_In_WebXml;
	public static String JSFUtils_Error_Finding_JSF_Version;
	public static String JSFUtils_Error_Finding_Latest_JSF_Version;
	public static String JSFUtils_Error_Reading_FacesConfig;
	public static String JSFUtils_Error_Searching_For_JSF_Type;
	public static String MavenJSFConstants_Warning_JSF21_Unavailable;
	public static String MavenJSFConstants_Warning_JSF22_Unavailable;
	public static String WebXmlJSFFacetDetector_Error_Cant_Detect_JSF_From_WebXml;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
