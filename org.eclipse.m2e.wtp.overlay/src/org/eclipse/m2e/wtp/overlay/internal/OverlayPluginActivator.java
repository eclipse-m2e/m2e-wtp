/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.m2e.wtp.overlay.internal;

import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.m2e.wtp.overlay.OverlayConstants;
import org.eclipse.m2e.wtp.overlay.internal.servers.OverlayResourceChangeListener;
import org.osgi.framework.BundleContext;

public class OverlayPluginActivator extends Plugin {
	
	public static final String PLUGIN_ID = OverlayConstants.PLUGIN_ID;

	IResourceChangeListener overlayresourceChangeListener;
	
	private static OverlayPluginActivator instance;
	
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		instance = this;
		overlayresourceChangeListener = new OverlayResourceChangeListener();
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
	    workspace.addResourceChangeListener(overlayresourceChangeListener);
	}
	
	@Override
	public void stop(BundleContext context) throws Exception {
		if (overlayresourceChangeListener != null) {
			IWorkspace workspace = ResourcesPlugin.getWorkspace();
		    workspace.removeResourceChangeListener(overlayresourceChangeListener);
		}
		instance = null;
		super.stop(context);
	}
	
	public static IPath getWorkspacePluginPath() {
		return instance == null? null : instance.getStateLocation();
	}
	
}
