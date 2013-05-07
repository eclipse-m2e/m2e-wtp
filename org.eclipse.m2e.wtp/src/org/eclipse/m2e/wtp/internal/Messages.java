/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp.internal;

import org.eclipse.osgi.util.NLS;

/**
 * Messages
 *
 * @author Fred Bricon
 */
public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.m2e.wtp.internal.messages"; //$NON-NLS-1$
	public static String AbstractProjectFacetConversionEnabler_Error_Accessing_Project;
	public static String AppClientVersionChangeListener_Error_Notifying_Application_Client_Version_Change;
	public static String VersionChangeListener_Unreadeable_Project_Nature;
	public static String EarProjectConverter_Error_EAR_Root_Content_Folder;
	public static String EarVersionChangeListener_Error_Notifying_EAR_Version_Change;
	public static String EjbClientClassifierClasspathProvider_EJB_Client_Classpath_Provider;
	public static String ClassifierClasspathProvider_Error_Loading_Maven_Instance;
	public static String MavenPluginUtils_ArtifactId_Cant_Be_Null;
	public static String MavenPluginUtils_Error_Cant_Retrieve_Latest_Plugin_Version;
	public static String MavenPluginUtils_GroupId_Cant_Be_Null;
	public static String MavenWtpPreferencePage_EAR_Project_Preferences;
	public static String MavenWtpPreferencePage_Enable_Project_Specific_Settings;
	public static String MavenWtpPreferencePage_Generate_ApplicationXml_Under_Build_Dir;
	public static String MavenWtpPreferencePage_Generate_MavenArchiver_Files_Under_Build_Dir;
	public static String MavenWtpPreferencePage_JavaEE_Integration_Settings;
	public static String MavenWtpPreferencePage_Maven_JavaEE_Integration_Settings;
	public static String MavenWtpPreferencePage_Select_Active_JavaEE_Configurators;
	public static String MavenWtpPreferencePage_Update_Projects_After_Preference_Changes;
	public static String MavenWtpPreferencePage_Updating_Configuration_For_Project;
	public static String MavenWtpPreferencePage_Updating_Maven_Projects_Job;
	public static String MavenWtpPreferencePage_Updating_Maven_Projects_Monitor;
	public static String MavenWtpPreferencePage_Using_Build_Directory;
	public static String MavenWtpPreferencePage_WAR_Project_Preferences;
	public static String MavenWtpPreferencesManagerImpl_0;
	public static String ResourceFilteringBuildParticipant_Changed_Resources_Require_Clean_Build;
	public static String ResourceFilteringBuildParticipant_Cleaning_Filtered_Folder;
	public static String ResourceFilteringBuildParticipant_Error_While_Filtering_Resources;
	public static String ResourceFilteringBuildParticipant_Executing_Resource_Filtering;
	public static String WarClassesClassifierClasspathProvider_WAR_Classes_Classifier_Classpath_Provider;
	public static String WarVersionChangeListener_Error_Notifying_WebApp_Version_Change;
	public static String WTPResourcesContentProvider_Error_Getting_Pipelined_Children;
	public static String WTPResourcesImages_Error_Creating_ImageDescriptor;
	public static String WTPResourcesNode_Cant_Retrieve_Project_Facet;
	public static String WTPResourcesNode_Deployed_Resources_Label;
	public static String WTPResourcesNode_Error_Getting_WTP_Resources;
	public static String FacetDetectorExtensionReader_Error_Configuring_Facet_Detector;
	public static String AbstractProjectConfiguratorDelegate_Error_Inconsistent_Java_Configuration;
	public static String AbstractProjectConfiguratorDelegate_Unable_To_Configure_Project;
	public static String AcrPluginConfiguration_Error_Project_Not_appclient;
	public static String ArtifactHelper_Error_Artifact_Must_Not_Be_Null;
	public static String EarPluginConfiguration_Error_Reading_EAR_Version;
	public static String EarPluginConfiguration_Project_Must_Have_ear_Packaging;
	public static String EarProjectConfiguratorDelegate_Configuring_EAR_Project;
	public static String EjbPluginConfiguration_Project_Must_Have_ejb_Packaging;
	public static String Error_Cleaning_WTP_Files;
	public static String Error_Maven_Project_Cant_Be_Null;
	public static String Error_Reading_Project_Facet;
    public static String markers_inclusion_patterns_problem;
	public static String markers_unsupported_dependencies_warning;
	public static String markers_mavenarchiver_output_settings_ignored_warning;
	public static String MavenDeploymentDescriptorManagement_Error_Deleting_Temp_Folder;
	public static String MavenSessionHelper_Error_Component_Lookup;
	public static String RarPluginConfiguration_Project_Must_Have_rar_Packaging;
    public static String WebProjectConfiguratorDelegate_File_Copy_Failed;
	public static String WebProjectConfiguratorDelegate_Renamed_Dependencies_Will_Be_Copied;
	public static String WTPProjectsUtil_Actions_Cant_Be_Null;
	public static String WTPProjectsUtil_Facet_Version_Cant_Be_Null;
	public static String WTPProjectsUtil_Project_Cant_Be_Null;
	public static String WTPProjectsUtil_Unable_To_Add_ModuleCoreNature;
	public static String EarPluginException_Default_Message;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
