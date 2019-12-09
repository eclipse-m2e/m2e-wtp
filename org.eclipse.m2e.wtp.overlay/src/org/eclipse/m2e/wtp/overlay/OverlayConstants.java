/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.m2e.wtp.overlay;

/**
 * Constants used by the org.eclipse.m2e.wtp.overlay plugin
 * 
 * @author Fred Bricon
 *
 */
public class OverlayConstants {

  private OverlayConstants(){}

  public static final String PLUGIN_ID = "org.eclipse.m2e.wtp.overlay"; //$NON-NLS-1$

  /**
   * Republish on server if an overlay dependency changed.
   */
  public static final String P_REPUBLISH_ON_PROJECT_CHANGE = "republishOnProjectChange"; //$NON-NLS-1$

}
