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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.wtp.facets.AbstractFacetDetector;
import org.eclipse.m2e.wtp.jsf.internal.utils.JSFUtils;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;

/**
 * Inspects the project faces-config to determine the JSF Facet version.
 * 
 * @author Fred Bricon
 * @since 0.18.0
 */
public class FacesConfigJSFFacetDetector extends AbstractFacetDetector {

	@Override
	public IProjectFacetVersion findFacetVersion(IMavenProjectFacade mavenProjectFacade, Map<?, ?> context, IProgressMonitor monitor) {
		IProject project = mavenProjectFacade.getProject();
		if (project == null) {
			return null;
		}
		String version = JSFUtils.getVersionFromFacesconfig(project);
		return version == null ? null : JSFUtils.getSafeJSFFacetVersion(version);
	}

}
