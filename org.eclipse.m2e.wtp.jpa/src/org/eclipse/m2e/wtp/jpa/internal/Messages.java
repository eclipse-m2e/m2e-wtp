package org.eclipse.m2e.wtp.jpa.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.m2e.wtp.jpa.internal.messages"; //$NON-NLS-1$
	public static String JptUtils_Error_Cant_Get_JPA_Version;
	public static String JptUtils_Error_Cant_Get_Latest_JPA_Version;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
