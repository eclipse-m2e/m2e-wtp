/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.m2e.wtp.overlay.internal.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.m2e.wtp.overlay.OverlayConstants;
import org.eclipse.m2e.wtp.overlay.internal.OverlayPluginActivator;

public class OverlayPreferencesInitializer extends AbstractPreferenceInitializer {

  /**
   * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
   */
  @Override
public void initializeDefaultPreferences() {
    IEclipsePreferences store = DefaultScope.INSTANCE.getNode(OverlayPluginActivator.PLUGIN_ID);
    store.putBoolean(OverlayConstants.P_REPUBLISH_ON_PROJECT_CHANGE, true);
  }
}