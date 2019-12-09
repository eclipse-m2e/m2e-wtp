/*******************************************************************************
 * Copyright (c) 2008-2015 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.wtp.internal.filtering;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectUtils;
import org.eclipse.m2e.wtp.EarPluginConfiguration;
import org.eclipse.m2e.wtp.MavenWtpConstants;
import org.eclipse.m2e.wtp.ProjectUtils;
import org.eclipse.m2e.wtp.WTPProjectsUtil;

/**
 * EarResourceFilteringConfiguration
 *
 * @author Fred Bricon
 */
public class EarResourceFilteringConfiguration extends AbstractResourceFilteringConfiguration {

	private EarPluginConfiguration earPluginConfiguration;

	public EarResourceFilteringConfiguration(IMavenProjectFacade mavenProjectFacade) {
		super(mavenProjectFacade);
		earPluginConfiguration = new EarPluginConfiguration(mavenProjectFacade.getMavenProject());
		pluginConfiguration = earPluginConfiguration;
	}

	@Override
	public IPath getTargetFolder() {
		return getTargetFolder(mavenProjectFacade.getMavenProject(), mavenProjectFacade.getProject());
	}

	public static IPath getTargetFolder(MavenProject mavenProject, IProject project) {
		return ProjectUtils.getM2eclipseWtpFolder(mavenProject, project).append(MavenWtpConstants.EAR_RESOURCES_FOLDER);
	}

	@Override
	public List<Xpp3Dom> getResources() {
		MavenProject mavenProject = mavenProjectFacade.getMavenProject();  
		IProject project = mavenProjectFacade.getProject();
		
		IPath cliPackageDir = getCLIPackageDir(project, mavenProject.getBuild().getDirectory());
		
		List<Xpp3Dom> resources = new ArrayList<>();
		
		IPath targetClasses = WTPProjectsUtil.getClassesFolder(mavenProjectFacade).getLocation();
		
		for (Resource r : mavenProject.getResources()) {
			String targetPath = r.getTargetPath();
			if (targetPath == null || targetPath.trim().isEmpty()) {
				continue;
			}
			IPath originalTarget = Path.fromOSString(targetPath);
			if (!originalTarget.isAbsolute()) {
				originalTarget = targetClasses.append(originalTarget);
			}
			if (cliPackageDir.isPrefixOf(originalTarget)) {
				Xpp3Dom resource = getAsXpp3Dom(r.getDirectory(), 
						                        r.getFiltering(), 
						                        originalTarget.makeRelativeTo(cliPackageDir).toPortableString(),
						                        r.getIncludes(),
						                        r.getExcludes());
				resources.add(resource);
			}
		}

		if (earPluginConfiguration.isFilteringDeploymentDescriptorsEnabled()) {
			String earContentDir = earPluginConfiguration.getEarContentDirectory(project);
			Xpp3Dom resource = getAsXpp3Dom(earContentDir, Boolean.TRUE.toString(), null, null, null);
			resources.add(resource);
		}
		
		return resources;
	}

	private IPath getCLIPackageDir(IProject project, String targetDir) {
	    IPath relativeTargetPath = MavenProjectUtils.getProjectRelativePath(project, targetDir);
	    if (relativeTargetPath == null) {
	      // target folder not under the project directory, we bail
	      return null;
	    }
	    IPath fullTargetPath = new Path(targetDir);
	    return fullTargetPath.append(earPluginConfiguration.getFinalName());
	}

	private Xpp3Dom getAsXpp3Dom(String folder, String filtering, String targetPath, List<String> inclusions, List<String> exclusions) {
		Xpp3Dom resource = new Xpp3Dom("resource"); //$NON-NLS-1$
		Xpp3Dom directory = new Xpp3Dom("directory"); //$NON-NLS-1$
		directory.setValue(folder);
		resource.addChild(directory);
		Xpp3Dom filter = new Xpp3Dom("filtering"); //$NON-NLS-1$
		filter.setValue(filtering);
		resource.addChild(filter);
		if (targetPath != null && !targetPath.isEmpty()) {
			Xpp3Dom targetPathDom = new Xpp3Dom("targetPath"); //$NON-NLS-1$
			targetPathDom.setValue(targetPath);
			resource.addChild(targetPathDom);
		}
		if (exclusions != null && !exclusions.isEmpty()) {
			Xpp3Dom exclusionsDom = new Xpp3Dom("excludes"); //$NON-NLS-1$
			for (String excl : exclusions) {
				Xpp3Dom exclusion = new Xpp3Dom("exclude"); //$NON-NLS-1$
				exclusion.setValue(excl);
				exclusionsDom.addChild(exclusion);
			}
			resource.addChild(exclusionsDom);
		}
		if (inclusions != null && !inclusions.isEmpty()) {
			Xpp3Dom inclusionsDom = new Xpp3Dom("includes"); //$NON-NLS-1$
			for (String incl : inclusions) {
				Xpp3Dom inclusion = new Xpp3Dom("include"); //$NON-NLS-1$
				inclusion.setValue(incl);
				inclusionsDom.addChild(inclusion);
			}
			resource.addChild(inclusionsDom);
		}
		return resource;
	}

}
