/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp.internal.conversion;

import static org.eclipse.m2e.wtp.internal.conversion.MavenPluginUtils.configure;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jst.j2ee.project.facet.IJ2EEFacetConstants;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.m2e.wtp.WTPProjectsUtil;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

/**
 * Converts Eclipse WTP Dynamic Web project settings into maven-war-plugin configuration 
 *
 * @author Fred Bricon
 */
public class WebProjectConverter extends AbstractWtpProjectConversionParticipant {

  private static final String DEFAULT_WAR_SOURCE_FOLDER = "src/main/webapp"; //$NON-NLS-1$
  
  private static final String WAR_SOURCE_DIRECTORY_KEY = "warSourceDirectory"; //$NON-NLS-1$

  private static final String FAIL_IF_MISSING_WEBXML_KEY = "failOnMissingWebXml"; //$NON-NLS-1$

  @Override
  public void convert(IProject project, Model model, IProgressMonitor monitor) throws CoreException {
    if (!accept(project) || !"war".equals(model.getPackaging())) { //$NON-NLS-1$
      return;
    }
    IVirtualComponent component = ComponentCore.createComponent(project);
    if (component == null) {
      return;
    }

    setWarPlugin(component, model);
  }

  private void setWarPlugin(IVirtualComponent component, Model model) throws CoreException {
    Build build = getCloneOrCreateBuild(model);
    String warPluginVersion = getWarPluginVersion();
    Plugin warPlugin = setPlugin(build, "org.apache.maven.plugins", "maven-war-plugin", warPluginVersion); //$NON-NLS-1$ //$NON-NLS-2$
  
    // Set  <warSourceDirectory>WebContent</warSourceDirectory>
    IFolder webContentFolder = findWebRootFolder(component);
    boolean customized = false;
    if (webContentFolder != null) {
      String webContent = webContentFolder.getProjectRelativePath().toPortableString();
      if (!DEFAULT_WAR_SOURCE_FOLDER.equals(webContent)) {
        configure(warPlugin, WAR_SOURCE_DIRECTORY_KEY, webContent);
        customized = true;
      }
    }
    
    try {
    	VersionRange mwp300 = VersionRange.createFromVersionSpec("[3.0.0,)");
		if (mwp300.containsVersion(new DefaultArtifactVersion(warPluginVersion))){
			customized = true;//we simply force adding the latest m-w-p without having to add failOnMissingWebXml
		} else {
			//Set <failOnMissingWebXml>false</failOnMissingWebXml> for web > 2.4
			IFacetedProject fProject = ProjectFacetsManager.create(component.getProject());
			if (fProject != null) {
				IProjectFacetVersion webVersion = fProject.getProjectFacetVersion(IJ2EEFacetConstants.DYNAMIC_WEB_FACET);
				if (webVersion != null && webVersion.compareTo(IJ2EEFacetConstants.DYNAMIC_WEB_24) > 0) {
					configure(warPlugin, FAIL_IF_MISSING_WEBXML_KEY, "false"); //$NON-NLS-1$
					customized = true;
				}
			}
		}
	} catch (InvalidVersionSpecificationException ex) {
		//not possible
	}

    if (customized) {
      model.setBuild(build);
    }
  }

  private IFolder findWebRootFolder(IVirtualComponent component) {
    return WTPProjectsUtil.getDefaultDeploymentDescriptorFolder(component.getRootFolder());
  }

  @Override
  protected IProjectFacet getRequiredFaced() {
    return WebFacetUtils.WEB_FACET;
  }

  private String getWarPluginVersion() {
	 //For test purposes only, must not be considered API behavior.
	 String version = System.getProperty("org.eclipse.m2e.wtp.conversion.warplugin.version");//$NON-NLS-1$
	 if(version != null) {
	   return version;
	 }
	 return MavenPluginUtils.getMostRecentPluginVersion("org.apache.maven.plugins", "maven-war-plugin", "3.2.0"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
  }

  
  
}
