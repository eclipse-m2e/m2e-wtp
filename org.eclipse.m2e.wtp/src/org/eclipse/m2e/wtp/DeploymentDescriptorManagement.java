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

/**
 * DeploymentDescriptorManagement
 *
 * @author Fred Bricon
 */
public interface DeploymentDescriptorManagement {

  static final DeploymentDescriptorManagement INSTANCE = new MavenDeploymentDescriptorManagement();
  //static final DeploymentDescriptorManagement INSTANCE = new WTPDeploymentDescriptorManagement();
  
  void updateConfiguration(IProject project, MavenProject mavenProject, EarPluginConfiguration plugin, boolean useBuildDirectory, IProgressMonitor monitor) throws CoreException;
}
