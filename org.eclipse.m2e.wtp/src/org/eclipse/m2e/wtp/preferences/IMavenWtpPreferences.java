/*******************************************************************************
 * Copyright (c) 2008-2014 Sonatype, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.wtp.preferences;

/**
 * Preferences for Maven WTP
 *
 * @author Fred Bricon
 */
public interface IMavenWtpPreferences {

  /**
   * Indicates if the application.xml should be generated under the build directory.
   * false indicates the first resource directory of the project will be used instead.
   *  
   * @return true if the application.xml should be generated under the build directory
   */
  boolean isApplicationXmGeneratedInBuildDirectory();

  void setApplicationXmGeneratedInBuildDirectory(boolean isEnabled);

  /**
   * Indicates if the project uses specific settings.
   * @return true if the project settings override the workspace preferences.
   */
  boolean isEnabledProjectSpecificSettings();
  
  void setEnabledProjectSpecificSettings(boolean isEnabled);

  /**
   * Indicates if the Maven Archiver should try to generate files in the build directory.
   * false indicates the content directory of the project will be used instead, unless resource filtering is activated.
   *  
   * @return true if the Maven Archiver should try to generate files in the build directory.
   */
  boolean isWebMavenArchiverUsesBuildDirectory();
 
  void setWebMavenArchiverUsesBuildDirectory(boolean isEnabled);
  
  /**
   * Checks if m2e-wtp is enabled.
   * 
   * @since 1.1
   */
  boolean isEnabled();

  /**
   * Disable or enable m2e-wtp.
   * 
   * @since 1.1
   */
  void setEnabled(boolean isEnabled);

}
