/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp.internal.conversion;

import org.apache.maven.model.Plugin;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * MavenPluginUtils
 *
 * @author Fred Bricon
 */
public class MavenPluginUtils {

  private static final String CONFIGURATION_KEY = "configuration";

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
}
