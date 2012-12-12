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

import java.lang.reflect.Method;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jpt.common.core.JptCommonCorePlugin;
import org.eclipse.jpt.common.core.resource.ResourceLocator;
import org.eclipse.jpt.jpa.core.JpaFacet;
import org.eclipse.jpt.jpa.core.resource.persistence.XmlPersistence;
import org.eclipse.jpt.jpa.core.resource.persistence.XmlPersistenceUnit;
import org.eclipse.jpt.jpa.core.resource.xml.JpaXmlResource;
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
	public static XmlPersistenceUnit getFirstXmlPersistenceUnit(JpaXmlResource persistenceXml) {
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
	public static  IProjectFacetVersion getVersion(JpaXmlResource persistenceXml) {
		if (persistenceXml == null) {
			return null;
		}
		String version = persistenceXml.getVersion();
		if (version == null || version.trim().length() == 0) {
			return JpaFacet.FACET.getDefaultVersion();
		}
		return JpaFacet.FACET.getVersion(version);
	}


	/**
	 * Returns the {@link ResourceLocator} for a given project.
	 * <p>
	 * This method was introduced to workaround a breaking change in Dali's (provisional) API : {@link JptCommonCorePlugin}.getResourceLocator  
	 * was removed in favor of {@link org.eclipse.jpt.common.core.internal.resource.ResourceLocatorManager}.getResourceLocator),
	 * </p>
	 */
	public static ResourceLocator getResourceLocator(IProject project) {
	  Method getResourceLocator;
	  try {
		getResourceLocator = JptCommonCorePlugin.class.getMethod("getResourceLocator", IProject.class);
		if(getResourceLocator!=null) {
			return (ResourceLocator)getResourceLocator.invoke(null, project);
		}
      } catch (NoSuchMethodException e) {
		try {
			Class<?> resourceLocatorManagerClass = Class.forName("org.eclipse.jpt.common.core.internal.resource.ResourceLocatorManager");
			Object resourceLocatorManager = resourceLocatorManagerClass.getMethod("getInstance", null).invoke(null, null);
			getResourceLocator = resourceLocatorManagerClass.getMethod("getResourceLocator", IProject.class);
			if(getResourceLocator!=null) {
				return (ResourceLocator)getResourceLocator.invoke(resourceLocatorManager, project);
			} ;
		} catch (Exception e1) {
			LOG.error("Unable to get a ResourceLocator for "+project, e1);
		}
	  } catch (Exception e) {
		LOG.error("Unable to get a ResourceLocator for "+project, e);
	  }
	  return null;  
	}
	
}
