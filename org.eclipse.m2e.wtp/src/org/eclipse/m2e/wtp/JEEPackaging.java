/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
  WAR("war"),
  /**
   * Enterprise Java Bean.
   */
  EJB("ejb"),
  /**
   * Enterprise Application Resource.
   */
  EAR("ear"),
  /**
   * Resource Adapter Archive.
   */
  RAR("rar"),
  /**
   * Application client
   */
  APP_CLIENT("app-client"),
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
      throw new IllegalArgumentException("packaging must not be null");
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
