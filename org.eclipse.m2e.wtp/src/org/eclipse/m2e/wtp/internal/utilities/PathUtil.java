/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.m2e.wtp.internal.utilities;

import java.io.File;

public class PathUtil {

  private static final char BACKSLASH = '\\';

  private static final char SLASH = '/';

  public static String toOsPath(String path) {
		if (path == null) return null;
		return path.replace(SLASH, File.separatorChar)
	            .replace(BACKSLASH, File.separatorChar);
	}
	
	public static String toPortablePath(String path) {
	  if (path == null) return null;
	  return path.replace(BACKSLASH, SLASH);
	}
}
