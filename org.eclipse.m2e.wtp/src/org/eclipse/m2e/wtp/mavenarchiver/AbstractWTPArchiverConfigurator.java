/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp.mavenarchiver;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.wst.common.componentcore.ModuleCoreNature;
import org.sonatype.m2e.mavenarchiver.internal.JarArchiverConfigurator;

/**
 * Base Maven Archiver configurator for WTP projects
 *
 * @author Fred Bricon
 */
public class AbstractWTPArchiverConfigurator extends JarArchiverConfigurator {

  /**
   * Eensures the project is WTP enabled before generating the manifest. 
   */
  @Override
  protected boolean needsNewManifest(IFile manifest, IMavenProjectFacade oldFacade, IMavenProjectFacade newFacade,
      IProgressMonitor monitor) {

    if (!ModuleCoreNature.isFlexibleProject(newFacade.getProject())) {
      return false;
    }
    return super.needsNewManifest(manifest, oldFacade, newFacade, monitor);
  }
}
