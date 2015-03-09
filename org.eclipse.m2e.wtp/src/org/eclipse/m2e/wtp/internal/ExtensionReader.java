/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp.internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.m2e.core.embedder.MavenRuntimeManager;
import org.eclipse.m2e.core.internal.markers.IMavenMarkerManager;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.wtp.AbstractDependencyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Extension reader
 * 
 * @author Eugene Kuleshov
 */
public class ExtensionReader {

  public static final String EXTENSION_DEPENDENCY_CONFIGURATORS = "org.eclipse.m2e.wtp.dependencyConfigurators"; //$NON-NLS-1$
  
  private static final Logger LOG = LoggerFactory.getLogger(ExtensionReader.class);
      
  private static final String ELEMENT_CONFIGURATOR = "configurator"; //$NON-NLS-1$
  
  private static ArrayList<AbstractDependencyConfigurator> dependencyConfigurators;

  public static List<AbstractDependencyConfigurator> readDependencyConfiguratorExtensions(IMavenProjectRegistry projectManager,
      MavenRuntimeManager runtimeManager, IMavenMarkerManager markerManager) {
    if (dependencyConfigurators == null) {
      dependencyConfigurators = new ArrayList<>();
      
      IExtensionRegistry registry = Platform.getExtensionRegistry();
      IExtensionPoint configuratorsExtensionPoint = registry.getExtensionPoint(EXTENSION_DEPENDENCY_CONFIGURATORS);
      if(configuratorsExtensionPoint != null) {
        IExtension[] configuratorExtensions = configuratorsExtensionPoint.getExtensions();
        for(IExtension extension : configuratorExtensions) {
          IConfigurationElement[] elements = extension.getConfigurationElements();
          for(IConfigurationElement element : elements) {
            if(element.getName().equals(ELEMENT_CONFIGURATOR)) {
              try {
                Object o = element.createExecutableExtension(AbstractProjectConfigurator.ATTR_CLASS);
  
                AbstractDependencyConfigurator projectConfigurator = (AbstractDependencyConfigurator) o;
                projectConfigurator.setProjectManager(projectManager);
                projectConfigurator.setRuntimeManager(runtimeManager);
                projectConfigurator.setMarkerManager(markerManager);
                
                dependencyConfigurators.add(projectConfigurator);
              } catch(CoreException ex) {
                LOG.error("Error configuring dependency configurator", ex); //$NON-NLS-1$
              }
            }
          }
        }
      }
      
      return dependencyConfigurators;
    }
    
    return dependencyConfigurators;
  }
}

