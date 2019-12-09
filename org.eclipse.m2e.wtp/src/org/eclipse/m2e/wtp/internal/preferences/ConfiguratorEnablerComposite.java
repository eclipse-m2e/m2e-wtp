/*******************************************************************************
 * Copyright (c) 2013-2014 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *      Red Hat, Inc - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.wtp.internal.preferences;

import org.eclipse.core.runtime.Assert;
import org.eclipse.m2e.wtp.preferences.ConfiguratorEnabler;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Composite displaying a checkbox {@link Control} based on a {@link ConfiguratorEnabler}
 * 
 * @author Fred Bricon
 * @since 0.17.0
 */
public class ConfiguratorEnablerComposite extends Composite {
  
  private Button enableConfigurator;
  
  private ConfiguratorEnabler enabler;

  public ConfiguratorEnablerComposite(Composite parent, ConfiguratorEnabler enabler, int style) {
    super(parent, style);
    Assert.isNotNull(enabler);
    this.enabler = enabler;
    enableConfigurator = new Button(parent, SWT.CHECK);
    enableConfigurator.setSelection(enabler.isEnabled());
    enableConfigurator.setText(enabler.getConfiguratorLabel());
    enableConfigurator.setToolTipText(enabler.getDescription());
  }

  public void setDefaultValue() {
    enableConfigurator.setSelection(true);
  }
  
  public void savePreferences() {
    if (enabler != null) {
      enabler.setEnabled(enableConfigurator.getSelection());
    }
  }
  
  @Override
  public void dispose() {
    enabler = null;
    enableConfigurator.dispose();
    super.dispose();
  }
  
  /**
   * Enable configurator 
   * 
   * @since 1.1.0
   */
  public void enableConfigurator(boolean enable) {
     if (enableConfigurator != null) {
    	 enableConfigurator.setSelection(enable);
      }
  }
  
  @Override
  public void setEnabled(boolean enabled) {
	super.setEnabled(enabled);
	if (enableConfigurator != null) {
	  enableConfigurator.setEnabled(enabled);
	}		
  }
}
