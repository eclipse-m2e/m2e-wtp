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
package org.eclipse.m2e.wtp.jpa;

import org.eclipse.jpt.jpa.core.resource.persistence.XmlPersistenceUnit;
import org.eclipse.jpt.jpa.core.resource.persistence.XmlProperties;
import org.eclipse.jpt.jpa.core.resource.persistence.XmlProperty;

/**
 * Base class for identifying platform ids from a {@link XmlPersistenceUnit}
 *  
 * @author Fred Bricon
 *
 */
public abstract class AbstractPlatformIdentifier implements IPlatformIdentifier {

	
	@Override
	public String getPlatformId(XmlPersistenceUnit xmlPersistenceUnit) {
		if (xmlPersistenceUnit == null) {
			return null;
		}
		
		String platformId = identifyProvider(xmlPersistenceUnit.getProvider());
		if (platformId != null) {
			return platformId;
		}
		
		XmlProperties properties = xmlPersistenceUnit.getProperties();
		if (properties != null && properties.getProperties() != null) {
			for (XmlProperty property : properties.getProperties()){
				platformId = identifyProperty(property);
				if (platformId != null) {
					return platformId;
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Returns the platformId associated with a given provider 
	 * or <code>null</code> if the provider doesn't match. 
	 */
	protected abstract String identifyProvider(String provider); 

	/**
	 * Returns the platformId from a given {@link XmlProperty} 
	 * or <code>null</code> if the property doesn't match  
	 */
	protected abstract String identifyProperty(XmlProperty property);

}
