/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp.preferences;

import org.eclipse.m2e.wtp.MavenWtpPlugin;


/**
 * Maven WTP preferences constants 
 */
public final class MavenWtpPreferencesConstants {
  
  /**
   * 
   */
  public static final String MAVEN_WTP_PREFERENCE_PAGE = "org.eclipse.m2e.wtp.preferences.MavenWtpPreferencePage"; //$NON-NLS-1$

  public static final String PREFIX = MavenWtpPlugin.ID;

  public static final String P_APPLICATION_XML_IN_BUILD_DIR = PREFIX + ".ear.applicationXmlInBuilDir"; //$NON-NLS-1$

  public static final String P_WEB_MAVENARCHIVER_IN_BUILD_DIR = PREFIX + ".war.archiverFilesInBuilDir"; //$NON-NLS-1$

  public static final String P_ENABLED_PROJECT_SPECIFIC__PREFS = PREFIX + ".enabledProjectSpecificPrefs"; //$NON-NLS-1$

  /**
   * Private constructor to avoid instanciation
   */
  private MavenWtpPreferencesConstants() {}

}
