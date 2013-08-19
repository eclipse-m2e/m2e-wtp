/*******************************************************************************
 * Copyright (c) 2011-2014 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Snjezana Peco (Red Hat, Inc.) - initial API and implementation
 *     Fred Bricon (Red Hat, Inc.)   - read infos from faces-config.xml
 ******************************************************************************/

package org.eclipse.m2e.wtp.jsf.internal.configurators;

import static org.eclipse.m2e.wtp.jsf.internal.MavenJSFConstants.JSF_FACET;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jst.common.project.facet.core.libprov.ILibraryProvider;
import org.eclipse.jst.common.project.facet.core.libprov.LibraryInstallDelegate;
import org.eclipse.jst.common.project.facet.core.libprov.LibraryProviderFramework;
import org.eclipse.jst.j2ee.project.facet.IJ2EEFacetConstants;
import org.eclipse.jst.jsf.core.internal.project.facet.IJSFFacetInstallDataModelProperties;
import org.eclipse.jst.jsf.core.internal.project.facet.JSFFacetInstallDataModelProvider;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.eclipse.m2e.wtp.MavenWtpPlugin;
import org.eclipse.m2e.wtp.ProjectUtils;
import org.eclipse.m2e.wtp.ResourceCleaner;
import org.eclipse.m2e.wtp.WTPProjectsUtil;
import org.eclipse.m2e.wtp.WarPluginConfiguration;
import org.eclipse.m2e.wtp.facets.FacetDetectorManager;
import org.eclipse.m2e.wtp.jsf.internal.MavenJSFConstants;
import org.eclipse.m2e.wtp.jsf.internal.Messages;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

/**
 * JSF maven project configurator.
 * <p>
 * This configurator adds the JSF facet to a project if it has or declares a faces-config.xml.
 * </p>
 * @author snjeza
 * @author Fred Bricon
 *
 */
public class JSFProjectConfigurator extends AbstractProjectConfigurator {
	
	private static final String M2E_JSF_ACTIVATION_PROPERTY = "m2e.jsf.activation"; //$NON-NLS-1$
	
	@Override
	public void configure(ProjectConfigurationRequest request,
			IProgressMonitor monitor) throws CoreException {
		configureInternal(request.getMavenProjectFacade(), monitor);
	}
	
	private void configureInternal(IMavenProjectFacade mavenProjectFacade,
			IProgressMonitor monitor) throws CoreException {
		MavenProject mavenProject = mavenProjectFacade.getMavenProject();
		IProject project = mavenProjectFacade.getProject();
		
		if (!"war".equals(mavenProject.getPackaging()))  {//$NON-NLS-1$
			return;
		}
		
		if (!isConfigurationEnabled(mavenProjectFacade, monitor)) {
			return;
		}
		
    	final IFacetedProject fproj = ProjectFacetsManager.create(project);
		if (fproj != null) {
			if (fproj.hasProjectFacet(JSF_FACET)) {
				//everything already installed. 
				//Since there's no support for version update -yet- we stop here
				return;
			}
			
			FacetDetectorManager facetDetectorManager = FacetDetectorManager.getInstance();
			IProjectFacetVersion jsfVersion = facetDetectorManager.findFacetVersion(mavenProjectFacade, JSF_FACET.getId(), monitor);
			if (fproj != null && jsfVersion != null) { 
				installJSFFacet(fproj, mavenProject, jsfVersion, monitor);
			}
		}
		
	}

	private boolean isConfigurationEnabled(IMavenProjectFacade facade, IProgressMonitor monitor) throws CoreException {
		if (WTPProjectsUtil.isM2eWtpDisabled(facade, monitor)) {
			return false;
		}
		
		Object pomActivationValue = facade.getMavenProject(monitor).getProperties().get(M2E_JSF_ACTIVATION_PROPERTY);
		boolean enabled;
		if (pomActivationValue == null) {
			enabled = MavenWtpPlugin.getDefault().getMavenWtpPreferencesManager().isEnabled(getId());
		} else {
			enabled = Boolean.parseBoolean(pomActivationValue.toString());
		}	
		return enabled;
	}
	
	private void installJSFFacet(IFacetedProject fproj, MavenProject mavenProject,
			IProjectFacetVersion facetVersion, IProgressMonitor monitor)
			throws CoreException {

		markerManager.deleteMarkers(fproj.getProject(), MavenJSFConstants.JSF_CONFIGURATION_ERROR_MARKER_ID);
				
		if (fproj.hasProjectFacet(IJ2EEFacetConstants.DYNAMIC_WEB_FACET) && !fproj.hasProjectFacet(JSF_FACET)) {
			IProject project = fproj.getProject();
			//JBIDE-10785 : refresh parent to prevent 
			// org.osgi.service.prefs.BackingStoreException: Resource '/parent/web/.settings' does not exist.
			ProjectUtils.refreshHierarchy(mavenProject.getBasedir(), 
										  IResource.DEPTH_INFINITE, 
										  new SubProgressMonitor(monitor, 1));
	
			WarPluginConfiguration warConfig = new WarPluginConfiguration(mavenProject, project);
			IFolder warSourceDir  = project.getFolder(warConfig.getWarSourceDirectory());

			//We don't want to generate any files automatically
			IPath facesConfigPath = new Path("WEB-INF/faces-config.xml"); //$NON-NLS-1$
			IFile defaultFacesConfig = warSourceDir.getFile(facesConfigPath);
			IFolder generatedWebResourcesFolder = ProjectUtils.getGeneratedWebResourcesFolder(mavenProject, project);
			IFile generatedFacesConfig = generatedWebResourcesFolder.getFile(facesConfigPath);
			
			ResourceCleaner cleaner = new ResourceCleaner(project);
			cleaner.addFolder(warSourceDir.getFolder("WEB-INF/lib")); //$NON-NLS-1$
			cleaner.addFiles(defaultFacesConfig, generatedFacesConfig);
			
			IStatus status = facetVersion.getConstraint().check(fproj.getProjectFacets());
			try {
				if (status.isOK()) {
					IDataModel model = createJSFDataModel(fproj,facetVersion);
					model.setBooleanProperty(IJSFFacetInstallDataModelProperties.CONFIGURE_SERVLET, false);
					fproj.installProjectFacet(facetVersion, model, monitor);
				} else {
					addErrorMarker(fproj.getProject(), NLS.bind(Messages.JSFProjectConfigurator_Marker_Facet_Version_Cant_Be_Installed, facetVersion, status.getMessage()));
					for (IStatus st : status.getChildren()) {
						addErrorMarker(fproj.getProject(), st.getMessage());
					}
				}
			} finally {
				cleaner.cleanUp();
			} 
		}
	}
	
	private void addErrorMarker(IProject project, String message) {
	    markerManager.addMarker(project, 
	    		MavenJSFConstants.JSF_CONFIGURATION_ERROR_MARKER_ID, 
	    		message
	    		,-1,  IMarker.SEVERITY_ERROR);
		
	}

	@SuppressWarnings("restriction")
	private IDataModel createJSFDataModel(IFacetedProject fproj, IProjectFacetVersion facetVersion) {
		IDataModel config = (IDataModel) new JSFFacetInstallDataModelProvider().create();
		LibraryInstallDelegate libraryDelegate = new LibraryInstallDelegate(fproj, facetVersion);
		ILibraryProvider provider = LibraryProviderFramework.getProvider("jsf-no-op-library-provider"); //$NON-NLS-1$
		libraryDelegate.setLibraryProvider(provider);
		config.setProperty(IJSFFacetInstallDataModelProperties.LIBRARY_PROVIDER_DELEGATE, libraryDelegate);
		config.setProperty(IJSFFacetInstallDataModelProperties.SERVLET_NAME, ""); //$NON-NLS-1$
		config.setProperty(IJSFFacetInstallDataModelProperties.SERVLET_URL_PATTERNS, new String[0]);
		
		return config;
	}
	
}
