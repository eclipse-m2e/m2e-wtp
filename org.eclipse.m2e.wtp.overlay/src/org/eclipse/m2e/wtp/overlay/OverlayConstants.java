/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

  public static final String PLUGIN_ID = "org.eclipse.m2e.wtp.overlay";

  /**
   * Republish on server if an overlay dependency changed.
   */
  public static final String P_REPUBLISH_ON_PROJECT_CHANGE = "republishOnProjectChange";

}
