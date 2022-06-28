/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.wtp.internal.conversion;

import org.apache.maven.model.Plugin;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.runtime.Assert;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.jdt.internal.JavaProjectConversionParticipant;
import org.eclipse.m2e.wtp.internal.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for {@link Plugin} manipulations.
 *
 * @author Fred Bricon
 */
public class MavenPluginUtils {

  private static final Logger log = LoggerFactory.getLogger(MavenPlugin.class);
  
  private static final String CONFIGURATION_KEY = "configuration"; //$NON-NLS-1$

  private MavenPluginUtils() {
  }
  
  private static Xpp3Dom getOrCreateConfiguration(Plugin plugin) {
    Xpp3Dom configuration = (Xpp3Dom)plugin.getConfiguration();
    if (configuration == null) {
      configuration = new Xpp3Dom(CONFIGURATION_KEY);
      plugin.setConfiguration(configuration);
    }
    return configuration;
  }
  
  public static void configure(Plugin plugin, String key, String value) {
    if (plugin == null) {
      return;
    }
    Xpp3Dom configuration = getOrCreateConfiguration(plugin);
    Xpp3Dom keyDom = configuration.getChild(key);
    if (keyDom == null) {
      keyDom = new Xpp3Dom(key);
      configuration.addChild(keyDom);
    }  
    keyDom.setValue(value);
  }
  

  /**
   * Returns the highest knwon (visible to local or global remote repos), non-snapshot plugin version between the given reference version and the versions.
   * 
   * This code was copied from {@link JavaProjectConversionParticipant}
   */
  @SuppressWarnings("restriction")
  public static String getMostRecentPluginVersion(String groupId, String artifactId, String referenceVersion) {
      Assert.isNotNull(groupId, Messages.MavenPluginUtils_GroupId_Cant_Be_Null);
      Assert.isNotNull(artifactId, Messages.MavenPluginUtils_ArtifactId_Cant_Be_Null);
      // TODO lookup local repo, remote global repos, central search...
      return referenceVersion;
  }
}
