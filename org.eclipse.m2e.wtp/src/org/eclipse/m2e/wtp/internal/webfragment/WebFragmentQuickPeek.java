/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
  *     Fred Bricon (Red Hat, Inc.) - initial API and implementation
 ******************************************************************************/
package org.eclipse.m2e.wtp.internal.webfragment;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.jst.jee.util.internal.JavaEEQuickPeek;
import org.eclipse.jst.jee.util.internal.XMLRootHandler;
import org.xml.sax.InputSource;

@SuppressWarnings("restriction")
public class WebFragmentQuickPeek {

	private static final String WEB_FRAGMENT_SCHEMA_ID_3_0 = "http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-fragment_3_0.xsd"; //$NON-NLS-1$
	private static final String WEB_FRAGMENT_SCHEMA_ID_3_1 = "http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-fragment_3_1.xsd"; //$NON-NLS-1$
	private static final String WEB_FRAGMENT_3_0 = "3.0"; //$NON-NLS-1$
	private static final String WEB_FRAGMENT_3_1 = "3.1"; //$NON-NLS-1$
	
	private XMLRootHandler handler;

	private String storedVersion = null;
	
	private boolean versionSet = false;
	
	public WebFragmentQuickPeek(InputStream in) {
		if (in != null) {
			try {
				InputSource inputSource = new InputSource(in);
				handler = new XMLRootHandler();
				handler.parseContents(inputSource);
			} catch (Exception ex) {
				// ignore
			} finally {
				try {
					in.reset();
				} catch (IOException ex) {
					// ignore
				}
			}
		}
	}
	
	public String getVersion() {
		if (!versionSet) {
			if (handler != null && "web-fragment".equals(handler.getRootName())) { //$NON-NLS-1$
				String version = null;
				if (handler.getRootAttributes() != null) {
					version = handler.getRootAttributes().getValue("version"); //$NON-NLS-1$
				}
				if (version == null || version.trim().length() == 0) {
					version = getVersionFromDtdSchema();
				}
				storedVersion = version;
				versionSet = true;
			}
			
		}
		return storedVersion;
	}

	private String getVersionFromDtdSchema() {
		//Algorithm copied from org.eclipse.jst.jee.util.internal.JavaEEQuickPeek
		if (handler == null) {
			return null;
		}
		String schemaName = null;
		if (handler.getRootAttributes() != null) {
			schemaName = JavaEEQuickPeek.normalizeSchemaLocation(handler.getRootAttributes().getValue("xsi:schemaLocation")); //$NON-NLS-1$
		}
		if (schemaName == null) {
			return null;
		}
		String version = null;
		if (schemaName.equals(WEB_FRAGMENT_SCHEMA_ID_3_0)) {
			version = WEB_FRAGMENT_3_0;
		} else if (schemaName.equals(WEB_FRAGMENT_SCHEMA_ID_3_1)) {
			version = WEB_FRAGMENT_3_1;
		} 
		return version;
	}
}
