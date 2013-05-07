package org.eclipse.m2e.wtp.overlay.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.m2e.wtp.overlay.internal.messages"; //$NON-NLS-1$
	public static String CompressionUtil_Unable_To_Create_Output_Dir;
	public static String CompressionUtil_Unpacking_Unable;
	public static String CompressionUtil_Unzipping_Interrupted;
	public static String OverlayVirtualArchiveComponent_Unpacking_Job;
	public static String OverlayReferenceResolver_Missing_Parameter;
	public static String OverlayReferenceResolver_Module_Name_Cant_Be_Inferred;
	public static String OverlayReferenceResolver_Not_Workspace_Project;
	public static String OverlayReferenceResolver_Unresolveable;
	public static String UnpackArchiveJob_Deleteing_was_cancelled;
	public static String UnpackArchiveJob_Error_Unpacking;
	public static String UnpackArchiveJob_Refreshing;
	public static String UnpackArchiveJob_Unpacking_Interrupted;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
