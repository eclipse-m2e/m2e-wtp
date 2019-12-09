/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.wtp.internal.mavenarchiver;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.wtp.MavenWtpConstants;
import org.eclipse.m2e.wtp.ProjectUtils;
import org.eclipse.m2e.wtp.mavenarchiver.AbstractWTPArchiverConfigurator;


/**
 * EarMavenArchiverConfigurator
 */
public class EarMavenArchiverConfigurator extends AbstractWTPArchiverConfigurator {

  @Override
  protected IPath getOutputDir(IMavenProjectFacade facade) {
    IProject project = facade.getProject();
    IPath localEarResourceFolder = ProjectUtils.getM2eclipseWtpFolder(facade.getMavenProject(), project);
    return project.getFullPath().append(localEarResourceFolder).append(MavenWtpConstants.EAR_RESOURCES_FOLDER);
  }

  @Override
  protected MojoExecutionKey getExecutionKey() {
    MojoExecutionKey key = new MojoExecutionKey("org.apache.maven.plugins", "maven-ear-plugin", "", "ear", null, null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    return key;
  }

}
