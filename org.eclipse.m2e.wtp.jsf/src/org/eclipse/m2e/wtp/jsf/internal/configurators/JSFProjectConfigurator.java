/*******************************************************************************
 * Copyright (c) 2011-2012 Red Hat, Inc.
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
import static org.eclipse.m2e.wtp.jsf.internal.MavenJSFConstants.JSF_FACET_VERSION_1_1;
import static org.eclipse.m2e.wtp.jsf.internal.MavenJSFConstants.JSF_FACET_VERSION_1_2;
import static org.eclipse.m2e.wtp.jsf.internal.MavenJSFConstants.JSF_FACET_VERSION_2_0;
import static org.eclipse.m2e.wtp.jsf.internal.MavenJSFConstants.JSF_FACET_VERSION_2_1;
import static org.eclipse.m2e.wtp.jsf.internal.MavenJSFConstants.JSF_VERSION_1_1;
import static org.eclipse.m2e.wtp.jsf.internal.MavenJSFConstants.JSF_VERSION_1_2;
import static org.eclipse.m2e.wtp.jsf.internal.MavenJSFConstants.JSF_VERSION_2_0;
import static org.eclipse.m2e.wtp.jsf.internal.MavenJSFConstants.JSF_VERSION_2_1;

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
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.eclipse.m2e.wtp.MavenWtpPlugin;
import org.eclipse.m2e.wtp.ProjectUtils;
import org.eclipse.m2e.wtp.ResourceCleaner;
import org.eclipse.m2e.wtp.WarPluginConfiguration;
import org.eclipse.m2e.wtp.jsf.internal.MavenJSFConstants;
import org.eclipse.m2e.wtp.jsf.internal.utils.JSFUtils;
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
	
	@Override
	public void configure(ProjectConfigurationRequest request,
			IProgressMonitor monitor) throws CoreException {
		MavenProject mavenProject = request.getMavenProject();
		IProject project = request.getProject();
		configureInternal(mavenProject,project, monitor);
	}
	
	private void configureInternal(MavenProject mavenProject,IProject project,
			IProgressMonitor monitor) throws CoreException {
		
		if (!"war".equals(mavenProject.getPackaging()))  {//$NON-NLS-1$
			return;
		}
		
		boolean enabled = MavenWtpPlugin.getDefault().getMavenWtpPreferencesManager().isEnabled(getId());
		if (!enabled) {
			return;
		}
		
    	final IFacetedProject fproj = ProjectFacetsManager.create(project);
		if (fproj != null) {
			if (fproj.hasProjectFacet(JSF_FACET)) {
				//everything already installed. 
				//Since there's no support for version update -yet- we stop here
				return;
			}
			
			IProjectFacetVersion jsfVersion = getJSFVersion(mavenProject, fproj);
			if (fproj != null && jsfVersion != null) { 
				installJSFFacet(fproj, mavenProject, jsfVersion, monitor);
			}
			
			
		}
		
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
			IPath facesConfigPath = new Path("WEB-INF/faces-config.xml");
			IFile defaultFacesConfig = warSourceDir.getFile(facesConfigPath);
			IFolder generatedWebResourcesFolder = ProjectUtils.getGeneratedWebResourcesFolder(mavenProject, project);
			IFile generatedFacesConfig = generatedWebResourcesFolder.getFile(facesConfigPath);
			
			ResourceCleaner cleaner = new ResourceCleaner(project);
			cleaner.addFolder(warSourceDir.getFolder("WEB-INF/lib"));
			cleaner.addFiles(defaultFacesConfig, generatedFacesConfig);
			
			IStatus status = facetVersion.getConstraint().check(fproj.getProjectFacets());
			try {
				if (status.isOK()) {
					IDataModel model = createJSFDataModel(fproj,facetVersion);
					model.setBooleanProperty(IJSFFacetInstallDataModelProperties.CONFIGURE_SERVLET, false);
					fproj.installProjectFacet(facetVersion, model, monitor);
				} else {
					addErrorMarker(fproj.getProject(), facetVersion + " can not be installed : "+ status.getMessage());
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

	private IProjectFacetVersion getJSFVersion(MavenProject mavenProject, IFacetedProject fproj) {
		String version = null;
		IProject project = fproj.getProject();
		version = JSFUtils.getVersionFromFacesconfig(project);
		
		if (version == null) {
			//JBIDE-9242 determine JSF version from classpath  
			version = JSFUtils.getJSFVersionFromClasspath(project);
		}
		
		if (version == null && hasFacesServletInWebXml(mavenProject, project)) {
			//No faces-config, no dependency on JSF, but uses faces-servlet
			//so we try to best guess the version depending on the installed web facet
			IProjectFacetVersion webVersion = fproj.getInstalledVersion(IJ2EEFacetConstants.DYNAMIC_WEB_FACET);
			if (webVersion.compareTo(IJ2EEFacetConstants.DYNAMIC_WEB_30) < 0) {
				version = JSF_VERSION_1_2;
			} else {
				version = JSF_VERSION_2_0;
			}
		}
		
		IProjectFacetVersion facetVersion = null;
		if (version != null) {
			if (version.startsWith(JSF_VERSION_1_1)) { 
				facetVersion = JSF_FACET_VERSION_1_1;
			}
			else if (version.startsWith(JSF_VERSION_1_2)) { 
				facetVersion = JSF_FACET_VERSION_1_2;	
			}
			else if (version.startsWith(JSF_VERSION_2_0)) { 
				facetVersion = JSF_FACET_VERSION_2_0;
			}
			else if (version.startsWith(JSF_VERSION_2_1)) { 
				facetVersion = JSF_FACET_VERSION_2_1;
			}			
		}

	    return facetVersion;
	}

	private boolean hasFacesServletInWebXml(MavenProject mavenProject, IProject project) {
		//We look for javax.faces.webapp.FacesServlet in web.xml
		//We should look for a custom web.xml at this point, but WTP would then crash on the JSF Facet installation
		//if it's not in a standard location, so we stick with the regular file.
		IFile webXml = ProjectUtils.getWebResourceFile(project, "WEB-INF/web.xml");
		return webXml != null && webXml.exists() && JSFUtils.hasFacesServlet(webXml);
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
