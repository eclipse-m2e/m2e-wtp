/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.m2e.wtp.overlay.modulecore;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ModuleURIUtil {

	public static final String URI_SEPARATOR = "&";
	
	public static Map<String, String> parseUri(String uri) {
		if (uri == null || uri.length() == 0) {
			return Collections.emptyMap();
		}
		Map<String, String> parameters = new HashMap<String, String>();
		int start = uri.indexOf("?"); 
		if (start > -1) {
			uri = uri.substring(start+1);
			String[] entries = uri.split(URI_SEPARATOR);
			for (String entry : entries) {
				if ("".equals(entry)) {
					continue;
				}
				String[] keyValue = entry.split("=");
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
		sb.append("?");
		for (Map.Entry<String, String> entry : parameters.entrySet()) {
			   sb.append(entry.getKey())
			   .append("=")
			   .append(entry.getValue())
			   .append(URI_SEPARATOR);
		}
		return sb.substring(0, sb.length()-1);
	}
	
	public static String extractModuleName(String uri) {
		if (uri != null && uri.indexOf("?") > 0) {
			return uri.substring(0,uri.indexOf("?"));
		}
		return uri;
	}
}
