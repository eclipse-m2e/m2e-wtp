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
 * IMavenWtpPreferencesManager
 *
 * @author fbricon
 */
public interface IMavenWtpPreferencesManager {

  IMavenWtpPreferences getPreferences(IProject project);

  IMavenWtpPreferences createNewPreferences();

  IMavenWtpPreferences getWorkspacePreferences();

  void savePreferences(IMavenWtpPreferences preferences, IProject project);

}
