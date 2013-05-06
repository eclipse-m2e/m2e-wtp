/*************************************************************************************
 * Copyright (c) 2011-2013 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Fred Bricon (Red Hat, Inc.) - initial API and implementation
 ************************************************************************************/
package org.eclipse.m2e.wtp.jaxrs.internal.configurators;

import static org.eclipse.m2e.wtp.WTPProjectsUtil.isWTPProject;
import static org.eclipse.m2e.wtp.jaxrs.internal.MavenJaxRsConstants.JAX_RS_FACET;
import static org.eclipse.m2e.wtp.jaxrs.internal.MavenJaxRsConstants.JAX_RS_FACET_1_0;
import static org.eclipse.m2e.wtp.jaxrs.internal.MavenJaxRsConstants.JAX_RS_FACET_1_1;
import static org.eclipse.m2e.wtp.jaxrs.internal.MavenJaxRsConstants.JAX_RS_FACET_2_0;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jst.common.project.facet.core.libprov.ILibraryProvider;
import org.eclipse.jst.common.project.facet.core.libprov.LibraryInstallDelegate;
import org.eclipse.jst.common.project.facet.core.libprov.LibraryProviderFramework;
import org.eclipse.jst.ws.jaxrs.core.internal.IJAXRSCoreConstants;
import org.eclipse.jst.ws.jaxrs.core.internal.project.facet.IJAXRSFacetInstallDataModelProperties;
import org.eclipse.jst.ws.jaxrs.core.internal.project.facet.JAXRSFacetInstallDataModelProvider;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.eclipse.m2e.wtp.MavenWtpPlugin;
import org.eclipse.m2e.wtp.ProjectUtils;
import org.eclipse.m2e.wtp.WTPProjectsUtil;
import org.eclipse.m2e.wtp.WarPluginConfiguration;
import org.eclipse.m2e.wtp.facets.FacetDetectorManager;
import org.eclipse.m2e.wtp.jaxrs.internal.MavenJaxRsConstants;
import org.eclipse.m2e.wtp.jaxrs.internal.Messages;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAX-RS maven project configurator.
 * <p>
 * This configurator adds the JAX-RS facet to a project if it has a dependency on the JAX-RS API.
 * </p>
 * 
 * @author Fred Bricon
 *
 */
public class JaxRsProjectConfigurator extends AbstractProjectConfigurator {

	private static final String WAR_PACKAGING = "war";
	
	private static final String M2E_JAXRS_ACTIVATION_PROPERTY = "m2e.jaxrs.activation";

	private static final Logger LOG = LoggerFactory.getLogger(JaxRsProjectConfigurator.class);
		
	@Override
	public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
		MavenProject mavenProject = request.getMavenProject();
		IProject project = request.getProject();
		configureInternal(mavenProject, project, monitor);
	}

	private void configureInternal(MavenProject mavenProject,IProject project,
			IProgressMonitor monitor) throws CoreException {

		if(!WAR_PACKAGING.equals(mavenProject.getPackaging())) { //$NON-NLS-1$
			return;
		}

		if (!isConfigurationEnabled(mavenProject)) {
			return;
		}
		
    	final IFacetedProject fproj = ProjectFacetsManager.create(project);
    	if (fproj == null) {
    		return;
    	}
    	
		if ((!fproj.hasProjectFacet(WTPProjectsUtil.DYNAMIC_WEB_FACET)) 
				|| fproj.hasProjectFacet(JAX_RS_FACET)) {
			//everything already installed. Since there's no support for version update -yet- we bail
			return;
		}
		
		FacetDetectorManager facetDetectorManager = FacetDetectorManager.getInstance();
		IProjectFacetVersion jaxRsVersion = facetDetectorManager.findFacetVersion(project, mavenProject, JAX_RS_FACET.getId(), monitor);
	    if (jaxRsVersion != null) {
	      installJaxRsFacet(fproj, jaxRsVersion, mavenProject, monitor);
	    }

	}

	private boolean isConfigurationEnabled(MavenProject mavenProject) {
		Object pomActivationValue = mavenProject.getProperties().get(M2E_JAXRS_ACTIVATION_PROPERTY);
		boolean enabled;
		if (pomActivationValue == null) {
			enabled = MavenWtpPlugin.getDefault().getMavenWtpPreferencesManager().isEnabled(getId());
		} else {
			enabled = Boolean.parseBoolean(pomActivationValue.toString());
		}	
		return enabled;
	}

	@SuppressWarnings("restriction")
	private void installJaxRsFacet(IFacetedProject fproj, IProjectFacetVersion facetVersion,
			MavenProject mavenProject, IProgressMonitor monitor) throws CoreException {

		markerManager.deleteMarkers(fproj.getProject(), MavenJaxRsConstants.JAXRS_CONFIGURATION_ERROR_MARKER_ID);

		IStatus status = facetVersion.getConstraint().check(fproj.getProjectFacets());
		if (status.isOK()) {
			// refreshing the project hierarchy to make sure that Eclipse "sees" the .settings folder and file, 
			// to be able to add the JAX-RS Facet. see https://issues.jboss.org/browse/JBIDE-10037
			ProjectUtils.refreshHierarchy(mavenProject.getBasedir(), 
					                      IResource.DEPTH_INFINITE, 
					                      new SubProgressMonitor(monitor, 1));
			IDataModel model = createJaxRsDataModel(fproj,facetVersion);
			WarPluginConfiguration warConfig = new WarPluginConfiguration(mavenProject, fproj.getProject());
			String warSourceDirectory = warConfig.getWarSourceDirectory();
			model.setProperty(IJAXRSFacetInstallDataModelProperties.WEBCONTENT_DIR, warSourceDirectory);
			model.setProperty(IJAXRSFacetInstallDataModelProperties.UPDATEDD, false);
			fproj.installProjectFacet(facetVersion, model, monitor);
		} else {
			String errorMessage = status.getMessage() == null ? "<unknown error>":status.getMessage();
			String markerMessage = NLS.bind(Messages.JaxrsProjectConfigurator_facet_cannot_be_installed, 
									facetVersion, errorMessage);
	        addErrorMarker(fproj.getProject(), markerMessage);
			for (IStatus st : status.getChildren()) {
		        addErrorMarker(fproj.getProject(), st.getMessage());
			}
		}
	}


	@SuppressWarnings("restriction")
	private IDataModel createJaxRsDataModel(IFacetedProject fproj,
			IProjectFacetVersion facetVersion) {
		IDataModel config = (IDataModel) new JAXRSFacetInstallDataModelProvider().create();
		LibraryInstallDelegate libraryDelegate = new LibraryInstallDelegate(fproj, facetVersion);
		ILibraryProvider provider = LibraryProviderFramework.getProvider(IJAXRSCoreConstants.NO_OP_LIBRARY_ID);
		libraryDelegate.setLibraryProvider(provider);
		config.setProperty(IJAXRSFacetInstallDataModelProperties.LIBRARY_PROVIDER_DELEGATE, libraryDelegate);
		return config;
	}

	@SuppressWarnings("restriction")
	private void addErrorMarker(IProject project, String message) {
	    markerManager.addMarker(project, 
	    		MavenJaxRsConstants.JAXRS_CONFIGURATION_ERROR_MARKER_ID, 
	    		message
	    		,-1,  IMarker.SEVERITY_ERROR);
	}
	
	@Override
	public void mavenProjectChanged(MavenProjectChangedEvent event,
			IProgressMonitor monitor) throws CoreException {
		IMavenProjectFacade facade = event.getMavenProject();
	    if(facade != null) {
	      IProject project = facade.getProject();
	      MavenProject mavenProject = facade.getMavenProject(monitor);
	      if(isWTPProject(project) && WAR_PACKAGING.equals(mavenProject.getPackaging())) {
		    IMavenProjectFacade oldFacade = event.getOldMavenProject();
		    if (oldFacade != null) {
		    	MavenProject oldProject = oldFacade.getMavenProject(monitor);
		    	if (oldProject != null && oldProject.getArtifacts().equals(mavenProject.getArtifacts())) {
		    		//Nothing changed since last build, no need to lookup for new Facets
		    		return;
		    	}
		    }
	        configureInternal(mavenProject, project, monitor);
	      }
	    }
	}

}
