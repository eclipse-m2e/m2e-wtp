/*******************************************************************************
 * Copyright (c) 2011-2012 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Snjezana Peco (Red Hat, Inc.) - initial API and implementation
 ******************************************************************************/
package org.eclipse.m2e.wtp.jsf.internal;

import org.eclipse.osgi.util.NLS;

/**
 * 
 * @author snjeza
 *
 */
public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.m2e.wtp.jsf.internal.messages"; //$NON-NLS-1$
	public static String JSFProjectConfigurator_The_project_does_not_contain_the_Web_Module_facet;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
