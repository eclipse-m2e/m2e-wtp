/*******************************************************************************
 * Copyright (c) 2008-2014 Sonatype, Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.wtp.preferences;

import org.eclipse.m2e.wtp.MavenWtpPlugin;


/**
 * m2e-wtp preferences constants 
 */
public final class MavenWtpPreferencesConstants {

  /**
   * m2e-wtp preference page id.
   */
  public static final String MAVEN_WTP_PREFERENCE_PAGE = "org.eclipse.m2e.wtp.preferences.MavenWtpPreferencePage"; //$NON-NLS-1$

  /**
   * m2e-wtp preference keys prefix.
   */

  public static final String PREFIX = MavenWtpPlugin.ID;

  /**
   * Key to toggle application.xml file generation in build directory.
   */
  public static final String P_APPLICATION_XML_IN_BUILD_DIR = PREFIX + ".ear.applicationXmlInBuilDir"; //$NON-NLS-1$

  /**
   * Key to toggle MANIFEST.MF and pom properties file generation 
   * in build directory.
   */
  public static final String P_WEB_MAVENARCHIVER_IN_BUILD_DIR = PREFIX + ".war.archiverFilesInBuilDir"; //$NON-NLS-1$

  /**
   * Key to (en|dis)able project specific m2e-wtp settings.
   */
  public static final String P_ENABLED_PROJECT_SPECIFIC__PREFS = PREFIX + ".enabledProjectSpecificPrefs"; //$NON-NLS-1$

  /**
   * Key to (en|dis)able m2e-wtp
   * 
   * @since 1.1.0
   */
  public static final String P_ENABLE_M2EWTP = PREFIX + ".enableM2eWtp"; //$NON-NLS-1$

  /**
   * Private constructor to avoid instanciation
   */
  private MavenWtpPreferencesConstants() {}

}
