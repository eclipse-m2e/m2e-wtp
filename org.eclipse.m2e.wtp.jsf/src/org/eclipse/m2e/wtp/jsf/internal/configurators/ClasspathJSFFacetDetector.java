/*************************************************************************************
 * Copyright (c) 2011-2013 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Fred Bricon (Red Hat, Inc.) - initial API and implementation
 ************************************************************************************/
package org.eclipse.m2e.wtp.jsf.internal.configurators;

import java.util.Map;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.wtp.facets.AbstractFacetDetector;
import org.eclipse.m2e.wtp.jsf.internal.utils.JSFUtils;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;

/**
 * Inspects the project classpath to determine the JSF version from known JSF classes.
 * 
 * @author Fred Bricon
 * @since 0.18.0
 */
public class ClasspathJSFFacetDetector extends AbstractFacetDetector {

	@Override
	public IProjectFacetVersion findFacetVersion(IProject project,
			MavenProject mavenProject, Map<?, ?> context, IProgressMonitor monitor) {
		String version = JSFUtils.getJSFVersionFromClasspath(project);
		return (version == null)?null:JSFUtils.getSafeJSFFacetVersion(version);
	}

}
