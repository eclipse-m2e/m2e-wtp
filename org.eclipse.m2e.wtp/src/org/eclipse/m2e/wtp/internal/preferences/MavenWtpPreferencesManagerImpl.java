/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp.internal.preferences;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.m2e.wtp.preferences.IMavenWtpPreferences;
import org.eclipse.m2e.wtp.preferences.IMavenWtpPreferencesManager;
import org.eclipse.m2e.wtp.preferences.MavenWtpPreferencesConstants;
import org.osgi.service.prefs.BackingStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of IMavenWtpPreferencesManager
 *
 * @author Fred Bricon
 */
public class MavenWtpPreferencesManagerImpl implements IMavenWtpPreferencesManager {

  private static Logger LOG = LoggerFactory.getLogger(MavenWtpPreferencesManagerImpl.class);
  /**
   * @see org.eclipse.m2e.wtp.preferences.IMavenWtpPreferencesManager#getPreferences(org.eclipse.core.resources.IProject)
   */
  public IMavenWtpPreferences getPreferences(IProject project) {
    if (project == null) {
      return loadWorkspacePreferences();
    }
    
    IEclipsePreferences eclipsePrefs = getEclipsePreferences(project);
    if (eclipsePrefs.getBoolean(MavenWtpPreferencesConstants.P_ENABLED_PROJECT_SPECIFIC__PREFS, false)) {
      return convertPreferences(eclipsePrefs);
    }
    return loadWorkspacePreferences();    
  }

  /**
   * @see org.eclipse.m2e.wtp.preferences.IMavenWtpPreferencesManager#createNewPreferences()
   */
  public IMavenWtpPreferences createNewPreferences() {
    return new MavenWtpPreferencesImpl();
  }
  
  /**
   * @see org.eclipse.m2e.wtp.preferences.IMavenWtpPreferencesManager#getWorkspacePreferences()
   */
  public IMavenWtpPreferences getWorkspacePreferences() {
    return loadWorkspacePreferences();
  }

  
  /**
   * @see org.eclipse.m2e.wtp.preferences.IMavenWtpPreferencesManager#savePreferences(org.eclipse.m2e.wtp.preferences.IMavenWtpPreferences, org.eclipse.core.resources.IProject)
   */
  public void savePreferences(IMavenWtpPreferences preferences, IProject project) {
    if (preferences == null) return;
    
    IEclipsePreferences eclipsePrefs;
    if (project != null) {
      eclipsePrefs = getEclipsePreferences(project);
      if (preferences.isEnabledProjectSpecificSettings()) {
        transformPreferences(preferences, eclipsePrefs);
      } else {
        removeSpecificSettings(eclipsePrefs);
      }
      eclipsePrefs.putBoolean(MavenWtpPreferencesConstants.P_ENABLED_PROJECT_SPECIFIC__PREFS, preferences.isEnabledProjectSpecificSettings());
    } else {
      eclipsePrefs = getEclipsePreferences();
      transformPreferences(preferences, eclipsePrefs);
    }
    try {
      eclipsePrefs.flush();
    } catch (BackingStoreException e) { 
      LOG.error("can't store m2e-wtp preferences", e);
    } 
  }
  
  private void removeSpecificSettings(IEclipsePreferences eclipsePrefs) {
    eclipsePrefs.remove(MavenWtpPreferencesConstants.P_APPLICATION_XML_IN_BUILD_DIR);
    eclipsePrefs.remove(MavenWtpPreferencesConstants.P_WEB_MAVENARCHIVER_IN_BUILD_DIR);
    
  }

  private IMavenWtpPreferences loadWorkspacePreferences() {
    return convertPreferences(getEclipsePreferences());
  }
  
  private void transformPreferences(IMavenWtpPreferences preferences, IEclipsePreferences eclipsePrefs) {
    eclipsePrefs.putBoolean(MavenWtpPreferencesConstants.P_APPLICATION_XML_IN_BUILD_DIR, preferences.isApplicationXmGeneratedInBuildDirectory());
    eclipsePrefs.putBoolean(MavenWtpPreferencesConstants.P_WEB_MAVENARCHIVER_IN_BUILD_DIR, preferences.isWebMavenArchiverUsesBuildDirectory());
  }

  private IMavenWtpPreferences convertPreferences(IEclipsePreferences eclipsePrefs) {
    IMavenWtpPreferences preferences = createNewPreferences();
    preferences.setEnabledProjectSpecificSettings(eclipsePrefs.getBoolean(MavenWtpPreferencesConstants.P_ENABLED_PROJECT_SPECIFIC__PREFS, false));
    preferences.setApplicationXmGeneratedInBuildDirectory(eclipsePrefs.getBoolean(MavenWtpPreferencesConstants.P_APPLICATION_XML_IN_BUILD_DIR, true));
    preferences.setWebMavenArchiverUsesBuildDirectory(eclipsePrefs.getBoolean(MavenWtpPreferencesConstants.P_WEB_MAVENARCHIVER_IN_BUILD_DIR, true));
    return preferences;
  }

  private static IEclipsePreferences getEclipsePreferences()
  {
    return new InstanceScope().getNode(MavenWtpPreferencesConstants.PREFIX);    
  }

  private static IEclipsePreferences getEclipsePreferences(IProject project)
  {
    return new ProjectScope(project).getNode(MavenWtpPreferencesConstants.PREFIX);    
  }

}
