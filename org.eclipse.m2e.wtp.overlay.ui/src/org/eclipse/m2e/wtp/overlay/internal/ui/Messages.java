package org.eclipse.m2e.wtp.overlay.internal.ui;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.m2e.wtp.overlay.internal.ui.messages"; //$NON-NLS-1$
	public static String OverlayPublishingPreferencePage_Automatically_Republish_Servers_On_Overlay_Modifications;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
