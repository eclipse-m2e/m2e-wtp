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
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
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
	
	/**
	 * Log a message at the INFO level.
	 * 
	 * @since 1.1.0
	 */
	public static void log(String message) {
		if (instance != null && message != null) {
			Status statusObj = new Status(IStatus.INFO, PLUGIN_ID, message);
			instance.getLog().log(statusObj);
		}
	}

	/**
	 * Log an {@link IStatus}.
	 * 
	 * @since 1.1.0
	 */
	public static void log(IStatus status) {
		if (instance != null && status != null) {
			instance.getLog().log(status);
		}
	}

	/**
	 * Log an ERROR message.
	 * 
	 * @since 1.1.0
	 */
	public static void logError(String message, Exception e) {
		if (instance != null) {
			if (message == null) {
				message = e.getLocalizedMessage();
			}
			Status status = new Status(IStatus.ERROR, PLUGIN_ID, message, e);
			instance.getLog().log(status);
		}
	}
	
}
