/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.m2e.wtp.overlay.internal.ui.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.m2e.wtp.overlay.OverlayConstants;
import org.eclipse.m2e.wtp.overlay.internal.ui.OverlayUIPluginActivator;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class OverlayPublishingPreferencePage  extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

  public OverlayPublishingPreferencePage() {
    super(GRID);
    this.setPreferenceStore(OverlayUIPluginActivator.getDefault().getPreferenceStore());
  }
  
  @Override
  protected void createFieldEditors() {
    addField(new BooleanFieldEditor(OverlayConstants.P_REPUBLISH_ON_PROJECT_CHANGE, 
                                    "Automatically republish servers on overlay modifications",
                                    getFieldEditorParent()));
  }

  public void init(IWorkbench workbench) {
    // Nothing to do
  }


}
