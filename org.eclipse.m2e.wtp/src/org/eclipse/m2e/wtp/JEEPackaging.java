/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.wtp;

/**
 * Enumeration of Maven JEE related packaging types.
 * 
 * @author Fred Bricon
 */
public enum JEEPackaging {
  /**
   * Web project.
   */
  WAR("war"), //$NON-NLS-1$
  /**
   * Enterprise Java Bean.
   */
  EJB("ejb"), //$NON-NLS-1$
  /**
   * Enterprise Application Resource.
   */
  EAR("ear"), //$NON-NLS-1$
  /**
   * Resource Adapter Archive.
   */
  RAR("rar"), //$NON-NLS-1$
  /**
   * Application client
   */
  APP_CLIENT("app-client"), //$NON-NLS-1$
  /**
   * Unknown packaging.
   */
  UNKNOWN(null);

  private String name;

  private JEEPackaging(String name) {
    this.name = name;
  }

  /**
   * @return the packaging name.
   */
  public String getName() {
    return name;
  }

  /**
   * Gets a JEEPackaging from maven packaging name. Supported values are <i>war</i>, <i>ejb</i>, <i>ear</i>,
   * <i>rar</i>, <i>app-client</i>.
   * 
   * @param packaging of a maven artifact.
   * @return the corresponding JEEPackaging or UNKNOWN if the package type is not supported.
   */
  public static JEEPackaging getValue(String packaging) {
    if(packaging == null) {
      throw new IllegalArgumentException("packaging must not be null"); //$NON-NLS-1$
    }
    for(JEEPackaging pkg : values()) {
      if(packaging.equals(pkg.getName())) {
        return pkg;
      }
    }
    return UNKNOWN;
  }
  
  static boolean isJEEPackaging(String packaging) {
    JEEPackaging pkg = getValue(packaging);
   return !UNKNOWN.equals(pkg);  
  }


}
