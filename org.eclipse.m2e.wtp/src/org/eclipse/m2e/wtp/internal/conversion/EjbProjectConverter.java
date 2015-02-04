/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp.internal.conversion;

import static org.eclipse.m2e.wtp.internal.conversion.MavenPluginUtils.configure;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jst.j2ee.project.facet.IJ2EEFacetConstants;
import org.eclipse.m2e.wtp.WTPProjectsUtil;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

/**
 * Converts Eclipse WTP EJB project settings into maven-ejb-plugin configuration 
 *
 * @author Fred Bricon
 */
public class EjbProjectConverter extends AbstractWtpProjectConversionParticipant {

  private static final String EJB_VERSION= "ejbVersion"; //$NON-NLS-1$

  @Override
public void convert(IProject project, Model model, IProgressMonitor monitor) throws CoreException {
    if (!accept(project) || !"ejb".equals(model.getPackaging())) { //$NON-NLS-1$
      return;
    }
    IVirtualComponent component = ComponentCore.createComponent(project);
    if (component == null) {
      return;
    }

    setEjbPlugin(component, model);
  }

  private void setEjbPlugin(IVirtualComponent component, Model model) throws CoreException {
    Build build = getCloneOrCreateBuild(model);
    String pluginVersion = getEjbPluginVersion();
    Plugin ejbPlugin = setPlugin(build, "org.apache.maven.plugins", "maven-ejb-plugin", pluginVersion); //$NON-NLS-1$ //$NON-NLS-2$
  
    IFacetedProject fProject = ProjectFacetsManager.create(component.getProject());
    boolean customized = false;
    if (fProject != null) {
      IProjectFacetVersion ejbVersion = fProject.getProjectFacetVersion(IJ2EEFacetConstants.EJB_FACET);
      if (!IJ2EEFacetConstants.EJB_21.equals(ejbVersion)) {
        configure(ejbPlugin, EJB_VERSION, ejbVersion.getVersionString());
        customized = true;
      }
    }

    if (customized) {
      model.setBuild(build);
    }
  }

  @Override
  protected IProjectFacet getRequiredFaced() {
    return WTPProjectsUtil.EJB_FACET;
  }

  private String getEjbPluginVersion() {
	 //For test purposes only, must not be considered API behavior.
	 String version = System.getProperty("org.eclipse.m2e.wtp.conversion.ejbplugin.version");//$NON-NLS-1$
	 if(version != null) {
	   return version;
	 }
	 return MavenPluginUtils.getMostRecentPluginVersion("org.apache.maven.plugins", "maven-ejb-plugin", "2.5"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
  }
  
}
