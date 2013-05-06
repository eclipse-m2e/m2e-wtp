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
package org.eclipse.m2e.wtp.jaxrs.internal.configurators;

import static org.eclipse.m2e.wtp.jaxrs.internal.MavenJaxRsConstants.JAX_RS_FACET_1_0;
import static org.eclipse.m2e.wtp.jaxrs.internal.MavenJaxRsConstants.JAX_RS_FACET_1_1;
import static org.eclipse.m2e.wtp.jaxrs.internal.MavenJaxRsConstants.JAX_RS_FACET_2_0;

import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.wtp.facets.AbstractFacetDetector;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Inspects the project classpath to detect JAX-RS classes.  
 * 
 * @author Fred Bricon
 *
 */
public class ClasspathJaxRsFacetDetector extends AbstractFacetDetector {

	private static final Logger LOG = LoggerFactory.getLogger(ClasspathJaxRsFacetDetector.class);

	@Override
	public IProjectFacetVersion findFacetVersion(IMavenProjectFacade mavenProjectFacade, Map<?, ?> context, IProgressMonitor monitor) {
		IProject project = mavenProjectFacade.getProject();
		if (project == null) {
			return null;
		}
		
		IJavaProject javaProject = JavaCore.create(project);
		if (javaProject != null) {
			IType type = null;
			try {
				type = javaProject.findType("javax.ws.rs.client.Client");
				if (type != null) {
					return JAX_RS_FACET_2_0;
				}

				type = javaProject.findType("javax.ws.rs.ApplicationPath");//$NON-NLS-1$ 
				if (type != null) {
					return JAX_RS_FACET_1_1;
				}

				type = javaProject.findType("javax.ws.rs.Path");//$NON-NLS-1$ 
				if (type != null) {
					return JAX_RS_FACET_1_0;
				}
			} catch (JavaModelException e) {
				LOG.error("Unable to determine JAX-RS version", e);
			}
		}
		return null;
	}

}
