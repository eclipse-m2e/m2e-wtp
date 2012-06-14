/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp.internal.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.m2e.wtp.MavenWtpPlugin;
import org.eclipse.m2e.wtp.preferences.MavenWtpPreferencesConstants;


/**
 * Maven WTP preferences initializer.
 * 
 * @author Fred Bricon
 */
public class MavenWtpPreferenceInitializer extends AbstractPreferenceInitializer {

  /**
   * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
   */
  public void initializeDefaultPreferences() {
    IPreferenceStore store = MavenWtpPlugin.getDefault().getPreferenceStore();
    store.setDefault(MavenWtpPreferencesConstants.P_APPLICATION_XML_IN_BUILD_DIR, true);
    store.setDefault(MavenWtpPreferencesConstants.P_WEB_MAVENARCHIVER_IN_BUILD_DIR, true);
  }

}
