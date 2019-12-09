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
package org.eclipse.m2e.wtp.jpa.internal.configurators;

import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jpt.common.core.resource.xml.JptXmlResource;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.wtp.facets.AbstractFacetDetector;
import org.eclipse.m2e.wtp.jpa.internal.util.JptUtils;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;

/**
 * Inspects the persistence.xml resource to determine the JPA facet.
 * 
 * @author Fred Bricon
 * @since 0.18.0
 */
public class PersistenceXmlJpaFacetDetector extends AbstractFacetDetector {

	@Override
	public IProjectFacetVersion findFacetVersion(IMavenProjectFacade mavenProjectFacade, Map<?, ?> context, IProgressMonitor monitor) {
		IProjectFacetVersion version = null; 
		if (context != null) {
			JptXmlResource jpaXmlResource = (JptXmlResource)context.get(JpaProjectConfigurator.PERSISTENCE_XML_KEY);
			version = JptUtils.getVersion(jpaXmlResource);
		}
		return version;
	}

}
