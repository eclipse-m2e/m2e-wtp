/*************************************************************************************
 * Copyright (c) 2011-2013 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Fred Bricon (Red Hat, Inc.) - initial API and implementation
 ************************************************************************************/
package org.eclipse.m2e.wtp.jsf.internal.configurators;

import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
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
	public IProjectFacetVersion findFacetVersion(IMavenProjectFacade mavenProjectFacade, Map<?, ?> context, IProgressMonitor monitor) {
		IProject project = mavenProjectFacade.getProject();
		if (project == null) {
			return null;
		}

		String version = JSFUtils.getJSFVersionFromClasspath(project);
		return (version == null)?null:JSFUtils.getSafeJSFFacetVersion(version);
	}

}
