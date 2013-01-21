/*************************************************************************************
 * Copyright (c) 2012 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Fred Bricon (Red Hat, Inc.) - initial API and implementation
 ************************************************************************************/
package org.eclipse.m2e.wtp.jpa.internal.util;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jpt.common.core.JptWorkspace;
import org.eclipse.jpt.common.core.resource.ResourceLocator;
import org.eclipse.jpt.common.core.resource.ResourceLocatorManager;
import org.eclipse.jpt.common.core.resource.xml.JptXmlResource;
import org.eclipse.jpt.jpa.core.JpaProject;
import org.eclipse.jpt.jpa.core.resource.persistence.XmlPersistence;
import org.eclipse.jpt.jpa.core.resource.persistence.XmlPersistenceUnit;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class related to the Java Persistence Toolkit
 * 
 * @author Fred Bricon
 */
public class JptUtils {

	private static final Logger LOG = LoggerFactory.getLogger(JptUtils.class);

	/**
	 * @return the IFile persitence.xml handle. Can be null. 
	 */
	public static  IFile getPersistenceXml(IProject project) {
		ResourceLocator resourceLocator = getResourceLocator(project);
		if (resourceLocator == null) {
			return null;
		}
		IPath path = resourceLocator.getResourcePath(project, new Path("META-INF/persistence.xml"));
		IFile persistenceXml = null;
		if (path != null) {
			persistenceXml = ResourcesPlugin.getWorkspace().getRoot().getFile(path);		
		}
		return persistenceXml;
	}
	
	/**
	 * @return the first {@link XmlPersistenceUnit} found in persistenceXml
	 */
	public static XmlPersistenceUnit getFirstXmlPersistenceUnit(JptXmlResource persistenceXml) {
		if (persistenceXml != null && persistenceXml.getRootObject() instanceof XmlPersistence) {
			XmlPersistence xmlPersistence = (XmlPersistence)persistenceXml.getRootObject();
			List<XmlPersistenceUnit> persistenceUnits  = xmlPersistence.getPersistenceUnits();
			if (persistenceUnits != null && !persistenceUnits.isEmpty()) {
				return persistenceUnits.get(0);
			}
		}
		return null;
	}
	
	/**
	 * @return the JPA Facet version corresponding to the version attribute of a {@link JpaXmlResource}
	 */
	public static  IProjectFacetVersion getVersion(JptXmlResource persistenceXml) {
		if (persistenceXml == null) {
			return null;
		}
		String version = persistenceXml.getVersion();
		if (version != null && version.trim().length() > 0) {
			try {
				return JpaProject.FACET.getVersion(version);
			} catch (Exception e) {
				LOG.error("Can not get JPA Facet version "+version, e);
				try {
					//We assume the detected version is not supported *yet* so take the latest.
					return JpaProject.FACET.getLatestVersion();
				} catch(CoreException cex) {
					LOG.error("Can not get Latest JPA Facet version", cex);
				}
			}
		}
		return JpaProject.FACET.getDefaultVersion();
	}


	/**
	 * Returns the {@link ResourceLocator} for a given project.
	 */
	public static ResourceLocator getResourceLocator(IProject project) {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		JptWorkspace jptWorkspace = (JptWorkspace) workspace.getAdapter(JptWorkspace.class);
		ResourceLocatorManager rlm = jptWorkspace.getResourceLocatorManager();
		return (rlm==null)?null:rlm.getResourceLocator(project);
	}
	
}
