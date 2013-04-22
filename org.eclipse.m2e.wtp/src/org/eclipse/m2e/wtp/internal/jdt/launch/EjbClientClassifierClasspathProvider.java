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
 * Adds main classes of EJB project referenced using the "client" classifier to the runtime classpath. 
 *
 * @author Fred Bricon
 */
public class EjbClientClassifierClasspathProvider extends AbstractClassifierClasspathProvider {

  private static final Logger LOG = LoggerFactory.getLogger(EjbClientClassifierClasspathProvider .class);
  
  /**
   * Applies if project of type <code>ejb</code> and having maven-ejb-plugin > configuration > generateClient = true 
   */
  @Override
  public boolean applies(IMavenProjectFacade mavenProjectFacade, String classifier) {
    return getClassifier().equals(classifier) 
        && "ejb".equals(mavenProjectFacade.getPackaging()) 
        && generatesClient(mavenProjectFacade);
  }

  private boolean generatesClient(IMavenProjectFacade mavenProjectFacade) {
    MavenProject mavenProject;
    try {
      mavenProject = mavenProjectFacade.getMavenProject(new NullProgressMonitor());
    } catch(CoreException ex) {
      LOG.error("Could not load mavenProject instance ", ex);
      return false;
    }
    Plugin ejbPlugin = mavenProject.getPlugin("org.apache.maven.plugins:maven-ejb-plugin");
    if (ejbPlugin != null) {
      Xpp3Dom config = (Xpp3Dom)ejbPlugin.getConfiguration();
      if (config != null) {
        Xpp3Dom generateClient = config.getChild("generateClient");
        if (generateClient != null && Boolean.parseBoolean(generateClient.getValue())) {
         return true; 
        }
      }
    }
    return false;
  }

  
  public String getClassifier() {
    return "client";
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
    return "EJB Client Classifier Classpath Provider";
  }

}
