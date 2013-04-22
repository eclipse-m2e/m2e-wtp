/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
           && "war".equals(mavenProjectFacade.getPackaging()) 
           && hasAttachedClasses(mavenProjectFacade);
  }

  private boolean hasAttachedClasses(IMavenProjectFacade mavenProjectFacade) {
    MavenProject mavenProject;
    try {
      mavenProject = mavenProjectFacade.getMavenProject(new NullProgressMonitor());
    } catch(CoreException ex) {
      LOG.error("Could not load mavenProject instance ", ex);
      return false;
    }
    Plugin warPlugin = mavenProject.getPlugin("org.apache.maven.plugins:maven-war-plugin");
    if (warPlugin != null) {
      Xpp3Dom config = (Xpp3Dom)warPlugin.getConfiguration();
      if (config != null) {
        Xpp3Dom attachClasses = config.getChild("attachClasses");
        if (attachClasses != null && Boolean.parseBoolean(attachClasses.getValue())) {
         return true; 
        }
      }
    }
    return false;
  }

  public String getClassifier() {
    return "classes";
  }
  
  public void setRuntimeClasspath(Set<IRuntimeClasspathEntry> runtimeClasspath, IMavenProjectFacade mavenProjectFacade,
      IProgressMonitor monitor) throws CoreException {
    addMainFolder(runtimeClasspath, mavenProjectFacade, monitor);
  }
  
  public void setTestClasspath(Set<IRuntimeClasspathEntry> runtimeClasspath, IMavenProjectFacade mavenProjectFacade,
      IProgressMonitor monitor) throws CoreException {
    setRuntimeClasspath(runtimeClasspath, mavenProjectFacade, monitor);
  }

  public String getName() {
    return "War classes Classifier Classpath Provider";
  }

}
