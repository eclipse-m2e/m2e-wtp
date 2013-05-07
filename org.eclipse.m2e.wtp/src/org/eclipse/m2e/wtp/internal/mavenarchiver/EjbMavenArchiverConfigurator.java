/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp.internal.mavenarchiver;

import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.wtp.mavenarchiver.AbstractWTPArchiverConfigurator;

/**
 * EjbMavenArchiverConfigurator
 *
 * @author Fred Bricon
 */
public class EjbMavenArchiverConfigurator extends AbstractWTPArchiverConfigurator {

  @Override
  protected MojoExecutionKey getExecutionKey() {
    MojoExecutionKey key = new MojoExecutionKey("org.apache.maven.plugins", "maven-ejb-plugin", "", "ejb", null, null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    return key;
  }
}
