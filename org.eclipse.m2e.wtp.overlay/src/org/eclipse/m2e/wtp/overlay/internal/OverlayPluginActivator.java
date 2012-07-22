/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.m2e.wtp.overlay.internal;

import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.m2e.wtp.overlay.OverlayConstants;
import org.eclipse.m2e.wtp.overlay.internal.servers.OverlayResourceChangeListener;
import org.osgi.framework.BundleContext;

public class OverlayPluginActivator extends Plugin {
	
	public static final String PLUGIN_ID = OverlayConstants.PLUGIN_ID;
	
	IResourceChangeListener overlayresourceChangeListener;
	
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
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
		super.stop(context);
	}
}
