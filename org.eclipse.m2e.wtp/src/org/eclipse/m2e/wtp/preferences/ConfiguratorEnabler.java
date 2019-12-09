/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.wtp.preferences;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.m2e.wtp.MavenWtpPlugin;
import org.osgi.service.prefs.BackingStoreException;


/**
 * This class allows clients to enable or disable optional Java EE project configurators.
 * 
 * @provisional This class has been added as part of a work in progress. 
 * It is not guaranteed to work or remain the same in future releases. 
 * For more information contact <a href="mailto:m2e-wtp-dev@eclipse.org">m2e-wtp-dev@eclipse.org</a>.
 * 
 * @author Fred Bricon
 * @since 0.17.0
 */
public class ConfiguratorEnabler {

  private String label;

  private String description;

  private String id;

  private Set<String> projectConfiguratorIds = new LinkedHashSet<>();

  IEclipsePreferences preferenceStore;

  public ConfiguratorEnabler(String enablerId, String label, String[] configuratorIds, String description) {
    //Keep new DefaultScope() to maintain Helios compatibility
    preferenceStore = new InstanceScope().getNode(MavenWtpPlugin.ID);
    this.id = enablerId;
    this.label = label;
    this.description = description;
    if(configuratorIds != null) {
      for(String id : configuratorIds) {
        projectConfiguratorIds.add(id);
      }
    }
  }

  /**
   * @return the label of the meta project configurator this enabler applies to.
   */
  public String getConfiguratorLabel() {
    return label == null ? id : label;
  }

  public String getDescription() {
    return description == null ? getConfiguratorLabel() : description;
  }

  public String getId() {
    return id;
  }

  /**
   * Checks the <code>org.eclipse.m2e.wtp</code> plugin preferences to see if the configurators this enabler applies to
   * are enabled.
   * 
   * @return <code>true</code> if the configurators are enabled, or no preference has been set yet.
   */
  public boolean isEnabled() {
    return preferenceStore.getBoolean(getPreferenceKey(), true);
  }

  /**
   * Stores in the <code>org.eclipse.m2e.wtp</code> plugin preferences the activation status of this enabler.
   */
  public void setEnabled(boolean enabled) {
    preferenceStore.putBoolean(getPreferenceKey(), enabled);
    try {
      preferenceStore.flush();
    } catch(BackingStoreException ex) {
      ex.printStackTrace();
    }
  }

  private String getPreferenceKey() {
    return getId() + ".enabled"; //$NON-NLS-1$
  }

  /**
   * @return true if the given <code>configuratorId</code> is controlled by this enabler.
   */
  public boolean appliesTo(String configuratorId) {
    return projectConfiguratorIds.contains(configuratorId);
  }

  @Override
public String toString() {
    return getId() + " : " + getConfiguratorLabel(); //$NON-NLS-1$
  }
}
