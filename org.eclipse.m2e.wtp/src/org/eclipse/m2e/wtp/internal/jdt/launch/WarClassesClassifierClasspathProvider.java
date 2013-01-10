/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
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
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.jdt.internal.launch.MainClassifierClasspathProvider;

/**
 * TestClassifierClasspathProvider
 *
 * @author Fred Bricon
 */
public class WarClassesClassifierClasspathProvider extends MainClassifierClasspathProvider {

  @Override
  public boolean applies(IMavenProjectFacade mavenProjectFacade, String classifier) {
    return getClassifier().equals(classifier) 
           && "war".equals(mavenProjectFacade.getPackaging()) 
           && hasAttachedClasses(mavenProjectFacade);
  }

  private boolean hasAttachedClasses(IMavenProjectFacade mavenProjectFacade) {
    Plugin warPlugin = mavenProjectFacade.getMavenProject().getPlugin("org.apache.maven.plugins:maven-war-plugin");
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
  
  @Override
  public void setTestClasspath(Set<IRuntimeClasspathEntry> resolved, IMavenProjectFacade mavenProjectFacade,
      IProgressMonitor monitor) throws CoreException {
    //Don't set the test classes
  }

  public String getName() {
    return "War classes Classifier Classpath Provider";
  }

}
