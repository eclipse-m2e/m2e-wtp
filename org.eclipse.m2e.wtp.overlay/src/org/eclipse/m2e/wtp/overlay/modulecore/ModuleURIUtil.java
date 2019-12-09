/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.m2e.wtp.overlay.modulecore;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to manipulate module URIs.
 * 
 * @provisional This class has been added as part of a work in progress. 
 * It is not guaranteed to work or remain the same in future releases. 
 * For more information contact <a href="mailto:m2e-wtp-dev@eclipse.org">m2e-wtp-dev@eclipse.org</a>.
 * 
 * @author Fred Bricon
 *
 */
public class ModuleURIUtil {

	public static final String URI_SEPARATOR = "&"; //$NON-NLS-1$
	
	public static Map<String, String> parseUri(String uri) {
		if (uri == null || uri.length() == 0) {
			return Collections.emptyMap();
		}
		Map<String, String> parameters = new HashMap<String, String>();
		int start = uri.indexOf("?");  //$NON-NLS-1$
		if (start > -1) {
			uri = uri.substring(start+1);
			String[] entries = uri.split(URI_SEPARATOR);
			for (String entry : entries) {
				if ("".equals(entry)) { //$NON-NLS-1$
					continue;
				}
				String[] keyValue = entry.split("="); //$NON-NLS-1$
				if (keyValue.length == 2) {
					parameters.put(keyValue[0], keyValue[1]);
				}
			}
		}
		return parameters;
	}
	
	public static String appendToUri(String uri, Map<String, String> parameters) {
		if (parameters == null || parameters.isEmpty()) {
			return uri;
		}
		StringBuilder sb = new StringBuilder(uri);
		sb.append("?"); //$NON-NLS-1$
		for (Map.Entry<String, String> entry : parameters.entrySet()) {
			   sb.append(entry.getKey())
			   .append("=") //$NON-NLS-1$
			   .append(entry.getValue())
			   .append(URI_SEPARATOR);
		}
		return sb.substring(0, sb.length()-1);
	}
	
	public static String extractModuleName(String uri) {
		if (uri != null && uri.indexOf("?") > 0) { //$NON-NLS-1$
			return uri.substring(0,uri.indexOf("?")); //$NON-NLS-1$
		}
		return uri;
	}
}
