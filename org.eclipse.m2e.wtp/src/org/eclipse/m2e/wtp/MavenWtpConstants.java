/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp;

import org.eclipse.m2e.core.internal.IMavenConstants;

/**
 * m2e-wtp Constants
 *
 * @author Fred Bricon
 */
public final class MavenWtpConstants {
  
  private MavenWtpConstants() {} 

  public static final String WTP_MARKER_ID = IMavenConstants.MARKER_ID + ".wtp"; //$NON-NLS-1$

  public static final String WTP_MARKER_CONFIGURATION_ERROR_ID = WTP_MARKER_ID + ".configuration";//$NON-NLS-1$ 
  
  public static final String WTP_MARKER_FILTERING_ERROR = WTP_MARKER_ID + ".filteringError"; //$NON-NLS-1$

  public static final String WTP_MARKER_OVERLAY_ERROR = WTP_MARKER_ID + ".overlayError"; //$NON-NLS-1$

  public static final String WTP_MARKER_GENERATE_APPLICATIONXML_ERROR = WTP_MARKER_ID + ".applicationXmlError"; //$NON-NLS-1$

  public static final String WTP_MARKER_UNSUPPORTED_DEPENDENCY_PROBLEM = WTP_MARKER_ID + ".unsupportedDependencyProblem"; //$NON-NLS-1$

  public static final String M2E_WTP_FOLDER = "m2e-wtp"; //$NON-NLS-1$
  
  public static final String WEB_RESOURCES_FOLDER = "web-resources"; //$NON-NLS-1$

  public static final String EAR_RESOURCES_FOLDER = "ear-resources"; //$NON-NLS-1$
  
  public static final String ROOT_FOLDER = "/"; //$NON-NLS-1$
  

  /**
   * Provisional name of component property used by WTP server adapters to exclude resources from deployment.
   * Only known consumer is JBoss AS adapter.
   */
  public static final String COMPONENT_EXCLUSION_PATTERNS = "component.exclusion.patterns"; //$NON-NLS-1$
  
  /**
   * Provisional name of component property used by WTP server adapters to include resources in deployment.
   * Only known consumer is JBoss AS adapter.
   */
  public static final String COMPONENT_INCLUSION_PATTERNS = "component.inclusion.patterns"; //$NON-NLS-1$

}
