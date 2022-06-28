/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.wtp.internal.utilities;

import java.io.InputStream;

import org.codehaus.plexus.util.IOUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.ModuleCoreNature;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Debug Utilities
 * 
 * @author Fred Bricon
 */
public class DebugUtilities {

  private static final Logger LOG = LoggerFactory.getLogger(DebugUtilities.class);
  
  public static String SEP = System.getProperty("line.separator"); //$NON-NLS-1$

  /**
   * Dumps the project's following informations :
   * <ul>
   * <li>underlying resources, if the project can return a IVirtualComponent</li>
   * <li>deploy-name</li>
   * <li>content of .settings/org.eclipse.wst.common.component</li>
   * <li>if the project is a FelixibleProject and has the ModuleCoreNature nature</li>
   * <li>list of all the installed project facets</li>
   * </ul>
   * 
   * @param startMessage The message to display before the informations
   * @param project The project to inspect
   * @return a dump of the project state
   */
  public static String dumpProjectState(String startMessage, IProject project) {
    StringBuilder dump = new StringBuilder((startMessage == null) ? "" : startMessage); //$NON-NLS-1$
    dump.append("Current ").append(Thread.currentThread()).append(SEP); //$NON-NLS-1$
    String projectName = project.getName();
    IVirtualComponent component = ComponentCore.createComponent(project);
    if(component == null) {
      dump.append(projectName).append(" is not a IVirtualComponent").append(SEP); //$NON-NLS-1$
    } else {
      dump.append(projectName).append(" is a ").append(component.getClass().getSimpleName()).append(SEP); //$NON-NLS-1$
      dump.append("Underlying resources for the root folder are :").append(SEP); //$NON-NLS-1$
      for(IResource resource : component.getRootFolder().getUnderlyingResources()) {
        dump.append("  -").append(resource.getFullPath().append(SEP)); //$NON-NLS-1$
      }
      dump.append("deploy-name = ").append(component.getDeployedName()).append(SEP); //$NON-NLS-1$
      dumpFile(dump, project.getFile(".settings/org.eclipse.wst.common.component")); //$NON-NLS-1$
    }
    boolean hasModulecoreNature = ModuleCoreNature.getModuleCoreNature(project) != null;
    boolean isFlexible = ModuleCoreNature.isFlexibleProject(project);
    dump.append(projectName).append(" hasModuleCoreNature:").append(hasModulecoreNature).append(", isFlexible:") //$NON-NLS-1$ //$NON-NLS-2$
        .append(isFlexible).append(SEP);
    dumpFacetInformations(project, dump);
    return dump.toString();
  }

  /**
   * Lists all the project facets associated to a project
   * 
   * @param project The project to analyze
   * @param dump The StringBuilder to append project facets informations to
   */
  private static void dumpFacetInformations(IProject project, StringBuilder dump) {
    try {
      IFacetedProject fProj = ProjectFacetsManager.create(project);
      if(fProj == null) {
        dump.append(project.getName()).append(" is not a faceted project").append(SEP); //$NON-NLS-1$
      } else {
        for(IProjectFacet facet : ProjectFacetsManager.getProjectFacets()) {
          if(fProj.hasProjectFacet(facet)) {
            dump.append("  - has ").append(fProj.getInstalledVersion(facet)).append(" facet").append(SEP); //$NON-NLS-1$ //$NON-NLS-2$
          }
        }
      }
    } catch(CoreException ex) {
      dump.append("An exception occured while accessing facet informations ").append(ex.getMessage()).append(SEP); //$NON-NLS-1$
    }
  }

  /**
   * Reads the contents of a file and appends it to a StringBuilder;
   * 
   * @param The StringBuilder to append the file content to
   * @param file The file to read
   */
  private static void dumpFile(StringBuilder dump, IFile file) {
    if(!file.exists()) {
      dump.append(file.getFullPath()).append(" does not exist").append(SEP); //$NON-NLS-1$
      return;
    }
    InputStream ins = null;
    try {
      dump.append("Contents of ").append(file.getFullPath()).append(SEP); //$NON-NLS-1$
      ins = file.getContents();
      dump.append(IOUtil.toString(ins));
    } catch(Exception e) {
      dump.append("An exception occured while reading ").append(file.getFullPath()).append(" :").append(e.getMessage()) //$NON-NLS-1$ //$NON-NLS-2$
          .append(SEP);
    } finally {
      IOUtil.close(ins);
    }
  }

  /**
   * Writes to the system output if Debug Output is enabled in the Maven Preferences Page
   * 
   * @param text The text to display in the standard output
   */
  public static void debug(String text) {
    if(isDebugEnabled()) {
      LOG.warn(text);
    }
  }

  /**
   * @return true if Debug Output is enabled in the Maven Preferences Page
   */
  public static boolean isDebugEnabled() {
    return MavenPlugin.getMavenConfiguration().isDebugOutput();
  }

}
