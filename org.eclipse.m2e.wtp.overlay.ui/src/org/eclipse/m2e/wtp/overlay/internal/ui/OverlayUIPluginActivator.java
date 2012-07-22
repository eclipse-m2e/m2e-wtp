/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.m2e.wtp.overlay.internal.ui;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.m2e.wtp.overlay.OverlayConstants;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class OverlayUIPluginActivator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.eclipse.m2e.wtp.overlay.ui"; //$NON-NLS-1$

	// The shared instance
	private static OverlayUIPluginActivator plugin;

  private IPreferenceStore preferenceStore;
	
	/**
	 * The constructor
	 */
	public OverlayUIPluginActivator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static OverlayUIPluginActivator getDefault() {
		return plugin;
	}

  @Override
  public IPreferenceStore getPreferenceStore() {
    // Create the preference store lazily.
    if(preferenceStore == null) {
      // InstanceScope.INSTANCE added in 3.7
      preferenceStore = new ScopedPreferenceStore(new InstanceScope(), OverlayConstants.PLUGIN_ID);

    }
    return preferenceStore;
  }
}
