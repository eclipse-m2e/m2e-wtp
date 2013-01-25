/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp.preferences;

import org.eclipse.core.resources.IProject;


/**
 * An instance of this interface allows clients to access and interact with <code>m2e-wtp</code> preferences.
 * <p>
 * This interface is not intended to be extended or implemented by clients.
 * </p>
 
 * @provisional This class has been added as part of a work in progress. 
 * It is not guaranteed to work or remain the same in future releases. 
 * For more information contact <a href="mailto:m2e-wtp-dev@eclipse.org">m2e-wtp-dev@eclipse.org</a>.
 *
 * @author Fred Bricon
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IMavenWtpPreferencesManager {

  /**
   * Returns the <code>m2e-wtp</code> preferences for the project.
   */
  IMavenWtpPreferences getPreferences(IProject project);

  /**
   * Creates a new the instance {@link IMavenWtpPreferences}.
   */
  IMavenWtpPreferences createNewPreferences();

  /**
   * Returns the <code>m2e-wtp</code> preferences for the workspace.
   */
  IMavenWtpPreferences getWorkspacePreferences();

  /**
   * saves the given {@link IMavenWtpPreferences} for the project, or for the workspace, 
   * if project is <code>null</code>.
   */
  void savePreferences(IMavenWtpPreferences preferences, IProject project);

  /**
   * Returns all {@link ConfiguratorEnabler}s for optional Java EE project configurators.
   * 
   * @since 0.17.0
   */
  ConfiguratorEnabler[] getConfiguratorEnablers();
  
  /**
   * Checks if an optional configurator is enabled. If no preferences are set for that configurator, 
   * it's assumed it's enabled by default. 
   *
   * @since 0.17.0
   */
  boolean isEnabled(String configuratorId);
}
