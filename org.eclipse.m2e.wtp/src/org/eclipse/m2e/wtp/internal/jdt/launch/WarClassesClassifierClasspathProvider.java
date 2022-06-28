/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Red Hat, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.wtp.internal.jdt.launch;

import java.util.Set;

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.jdt.AbstractClassifierClasspathProvider;
import org.eclipse.m2e.wtp.JEEPackaging;
import org.eclipse.m2e.wtp.internal.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adds main classes of war project referenced using the "classes" classifier to the runtime classpath. 
 *
 * @author Fred Bricon
 */
public class WarClassesClassifierClasspathProvider extends AbstractClassifierClasspathProvider {
  
  private static final Logger LOG = LoggerFactory.getLogger(WarClassesClassifierClasspathProvider.class);

  /**
   * Applies if project of type <code>war</code> and having maven-war-plugin > configuration > attachClasses = true 
   */
  @Override
  public boolean applies(IMavenProjectFacade mavenProjectFacade, String classifier) {
    return getClassifier().equals(classifier) 
           && JEEPackaging.WAR.getName().equals(mavenProjectFacade.getPackaging()) 
           && hasAttachedClasses(mavenProjectFacade);
  }

  private boolean hasAttachedClasses(IMavenProjectFacade mavenProjectFacade) {
    MavenProject mavenProject;
    try {
      mavenProject = mavenProjectFacade.getMavenProject(new NullProgressMonitor());
    } catch(CoreException ex) {
      LOG.error(Messages.ClassifierClasspathProvider_Error_Loading_Maven_Instance, ex); 
      return false;
    }
    Plugin warPlugin = mavenProject.getPlugin("org.apache.maven.plugins:maven-war-plugin"); //$NON-NLS-1$
    if (warPlugin != null) {
      Xpp3Dom config = (Xpp3Dom)warPlugin.getConfiguration();
      if (config != null) {
        Xpp3Dom attachClasses = config.getChild("attachClasses"); //$NON-NLS-1$
        if (attachClasses != null && Boolean.parseBoolean(attachClasses.getValue())) {
         return true; 
        }
      }
    }
    return false;
  }

  @Override
public String getClassifier() {
    return "classes"; //$NON-NLS-1$
  }
  
  @Override
public void setRuntimeClasspath(Set<IRuntimeClasspathEntry> runtimeClasspath, IMavenProjectFacade mavenProjectFacade,
      IProgressMonitor monitor, int classpathProperty) throws CoreException {
    addMainFolder(runtimeClasspath, mavenProjectFacade, monitor, classpathProperty);
  }
  
  @Override
public void setTestClasspath(Set<IRuntimeClasspathEntry> runtimeClasspath, IMavenProjectFacade mavenProjectFacade,
      IProgressMonitor monitor, int classpathProperty) throws CoreException {
    setRuntimeClasspath(runtimeClasspath, mavenProjectFacade, monitor, classpathProperty);
  }

  @Override
public String getName() {
    return Messages.WarClassesClassifierClasspathProvider_WAR_Classes_Classifier_Classpath_Provider;
  }

}
