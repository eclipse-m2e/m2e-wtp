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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.m2e.wtp.overlay.internal.modulecore.OverlaySelfComponent;
import org.eclipse.m2e.wtp.overlay.internal.modulecore.OverlayVirtualArchiveComponent;
import org.eclipse.m2e.wtp.overlay.internal.modulecore.OverlayVirtualComponent;

/**
 * Overlay Component factory.
 * 
 * @author Fred Bricon
 */
public class OverlayComponentCore {
	
	public static IOverlayVirtualComponent createOverlayComponent(IProject aProject) {
		return new OverlayVirtualComponent(aProject);
	}

	//TODO check and prevent circular references
	public static IOverlayVirtualComponent createSelfOverlayComponent(IProject aProject) {
		return new OverlaySelfComponent(aProject);
	}

	public static IOverlayVirtualComponent createOverlayArchiveComponent(IProject aComponentProject, String archiveLocation, IPath unpackDirPath, IPath aRuntimePath) throws CoreException {
		final OverlayVirtualArchiveComponent component = new OverlayVirtualArchiveComponent(aComponentProject, archiveLocation, unpackDirPath, aRuntimePath);
		return component;
	}
}
