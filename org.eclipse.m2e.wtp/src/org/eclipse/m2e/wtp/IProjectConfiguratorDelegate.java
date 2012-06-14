/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.jdt.IClasspathDescriptor;


/**
 * Configure projects based on maven plugin configuration.
 * 
 * @author Fred Bricon
 */
interface IProjectConfiguratorDelegate {

  /**
   * Set project facet and configure settings according to maven plugin configuration.
   * 
   * @param mavenProject
   * @param project
   * @param monitor
   * @throws MarkedException   
   */
  void configureProject(IProject project, MavenProject mavenProject, IProgressMonitor monitor) throws MarkedException;

  /**
   * Configure project module dependencies based on maven plugin configuration.
   * 
   * @param mavenProject
   * @param project
   * @param monitor
   * @throws CoreException
   */
  void setModuleDependencies(IProject project, MavenProject mavenProject, IProgressMonitor monitor)
      throws CoreException;

  /**
   * Configures Maven project classpath, i.e. content of MavenDependencies classpath container
   * 
   * @param project
   * @param mavenProject
   * @param classpath
   * @param monitor
   */
  void configureClasspath(IProject project, MavenProject mavenProject, IClasspathDescriptor classpath,
      IProgressMonitor monitor) throws CoreException;

}
