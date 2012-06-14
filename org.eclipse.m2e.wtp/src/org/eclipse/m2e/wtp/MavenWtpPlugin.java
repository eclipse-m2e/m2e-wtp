/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp;

import java.io.File;

import org.eclipse.m2e.wtp.internal.preferences.MavenWtpPreferencesManagerImpl;
import org.eclipse.m2e.wtp.preferences.IMavenWtpPreferencesManager;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * Maven WTP plugin
 *
 * @author Eugene Kuleshov
 */
public class MavenWtpPlugin extends AbstractUIPlugin {

  public static final String ID = "org.eclipse.m2e.wtp";
  
  private static MavenWtpPlugin instance;

  private File explodedWarsDir;
  

  private IMavenWtpPreferencesManager preferenceManager; 
  
  public IMavenWtpPreferencesManager getMavenWtpPreferencesManager() {
    return preferenceManager;
  }

  public MavenWtpPlugin() {
    instance = this;
  }
  
  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    
    File stateLocationDir = getStateLocation().toFile();
    explodedWarsDir = new File(stateLocationDir, "exploded-wars");
    if (!explodedWarsDir.exists()) {
      explodedWarsDir.mkdirs();
    }

    this.preferenceManager = new MavenWtpPreferencesManagerImpl();
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    super.stop(context);
  }
  
  public static MavenWtpPlugin getDefault() {
    return instance;
  }

  /**
   * @return Returns the explodedWarsDir.
   */
  public File getExplodedWarsDir() {
    return explodedWarsDir;
  }
  
}
