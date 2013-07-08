/*******************************************************************************
 * Copyright (c) 2008-2013 Sonatype, Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp.internal.preferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.m2e.wtp.MavenWtpPlugin;
import org.eclipse.m2e.wtp.internal.Messages;
import org.eclipse.m2e.wtp.internal.StringUtils;
import org.eclipse.m2e.wtp.preferences.ConfiguratorEnabler;
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

  private static final String ATTR_ENABLER_ID = "id"; //$NON-NLS-1$
  private static final String ATTR_CONFIGURATOR_IDS = "configuratorIds"; //$NON-NLS-1$
  private static final String ATTR_LABEL = "label"; //$NON-NLS-1$
  private static final String ATTR_DESCRIPTION = "description"; //$NON-NLS-1$


  private static final Logger LOG = LoggerFactory.getLogger(MavenWtpPreferencesManagerImpl.class);
  
  private static final String CONFIGURATOR_ENABLER_EXTENSION_POINT = MavenWtpPlugin.ID+".javaeeConfiguratorEnabler"; //$NON-NLS-1$

  private List<ConfiguratorEnabler> enablers;

  /**
   * @see org.eclipse.m2e.wtp.preferences.IMavenWtpPreferencesManager#getPreferences(org.eclipse.core.resources.IProject)
   */
  @Override
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
  @Override
public IMavenWtpPreferences createNewPreferences() {
    return new MavenWtpPreferencesImpl();
  }
  
  /**
   * @see org.eclipse.m2e.wtp.preferences.IMavenWtpPreferencesManager#getWorkspacePreferences()
   */
  @Override
public IMavenWtpPreferences getWorkspacePreferences() {
    return loadWorkspacePreferences();
  }

  
  /**
   * @see org.eclipse.m2e.wtp.preferences.IMavenWtpPreferencesManager#savePreferences(org.eclipse.m2e.wtp.preferences.IMavenWtpPreferences, org.eclipse.core.resources.IProject)
   */
  @Override
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
      LOG.error(Messages.MavenWtpPreferencesManagerImpl_0, e);
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

  @Override
public ConfiguratorEnabler[] getConfiguratorEnablers() {
    if (enablers == null) {
      enablers = loadConfiguratorEnablers();
    }
    ConfiguratorEnabler[] enablersArray = new ConfiguratorEnabler[enablers.size()];
    enablers.toArray(enablersArray);
    return enablersArray;
  }

  private static List<ConfiguratorEnabler> loadConfiguratorEnablers() {
    IExtensionRegistry registry = Platform.getExtensionRegistry();
    IConfigurationElement[] enablerConfigs = registry.getConfigurationElementsFor(CONFIGURATOR_ENABLER_EXTENSION_POINT);
    if (enablerConfigs == null) {
      return Collections.emptyList();
    }
    ArrayList<ConfiguratorEnabler> enablers = new ArrayList<ConfiguratorEnabler>();
    for (IConfigurationElement config : enablerConfigs) {
        String enablerId = config.getAttribute(ATTR_ENABLER_ID);
        String[] configuratorIds = split(config.getAttribute(ATTR_CONFIGURATOR_IDS));
        String label = config.getAttribute(ATTR_LABEL);
        String description = config.getAttribute(ATTR_DESCRIPTION);
        enablers.add(new ConfiguratorEnabler(enablerId, label, configuratorIds, description));
    }
    return enablers;
  }

  private static String[] split(String str) {
    return StringUtils.tokenizeToStringArray(str, ","); //$NON-NLS-1$
  }

  /* (non-Javadoc)
   * @see org.eclipse.m2e.wtp.preferences.IMavenWtpPreferencesManager#isEnabled(java.lang.String)
   */
  @Override
public boolean isEnabled(String configuratorId) {
    for (ConfiguratorEnabler enabler : getConfiguratorEnablers()) {
      if (enabler.appliesTo(configuratorId)) {
        return enabler.isEnabled();
      }
    }
    return true;
  }
  
}
