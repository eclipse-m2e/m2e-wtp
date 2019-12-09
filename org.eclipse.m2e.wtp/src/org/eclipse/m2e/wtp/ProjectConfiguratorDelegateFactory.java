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
 * This factory creates IProjectConfiguratorDelegate based on Maven projects packaging.  
 *
 * @author Fred Bricon
 */
//XXX See if we could refactor this to do JEEPackaging.createProjectConfiguratorDelegate() instead. 
class ProjectConfiguratorDelegateFactory {

  private ProjectConfiguratorDelegateFactory() {
    //We don't need to instantiate this class
  }
   
  
  /**
   * IProjectConfiguratorDelegate factory method.  
   * @param packaging supported values are war, ejb, ear.
   * @return a new instance of IProjectConfiguratorDelegate or null if packaging is not supported.
   */
  static IProjectConfiguratorDelegate getProjectConfiguratorDelegate(String packaging){
    JEEPackaging mvnPackaging = JEEPackaging.getValue(packaging);
    
    switch(mvnPackaging) {
      case WAR:
        return new WebProjectConfiguratorDelegate();
      case EJB:
        return new EjbProjectConfiguratorDelegate();
      case EAR:
        return new EarProjectConfiguratorDelegate();
      case RAR:
        return new ConnectorProjectConfiguratorDelegate();
      case APP_CLIENT:
    	  return new AppClientProjectConfiguratorDelegate();
      default :
        return null;
    }
  
  }
  
}
