/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.m2e.wtp.overlay.internal.utilities;

import java.io.File;

public class PathUtil {

	public static String useSystemSeparator(String name) {
		if (name == null) return null;
		return name.replace('/', File.separatorChar)
	            .replace('\\', File.separatorChar);
	}
}
