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

/**
 * Identifies a JPA platform id (as defined in {@link JpaPlatformDescription}) from a given {@link XmlPersistenceUnit}
 * 
 * @author Fred Bricon
 *
 * @see JpaPlatformDescription
 * @see XmlPersistenceUnit
 */
public interface IPlatformIdentifier {

	/**
	 * Returns a {@link JpaPlatformDescription} id inferred from a given {@link XmlPersistenceUnit}
	 */
	String getPlatformId(XmlPersistenceUnit persistenceUnit);
}
