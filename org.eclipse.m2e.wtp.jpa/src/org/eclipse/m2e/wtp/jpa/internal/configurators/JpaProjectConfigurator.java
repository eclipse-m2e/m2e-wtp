/*************************************************************************************
 * Copyright (c) 2012-2014 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Fred Bricon (Red Hat, Inc.) - initial API and implementation
 ************************************************************************************/
package org.eclipse.m2e.wtp.jpa.internal.configurators;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jpt.common.core.resource.xml.JptXmlResource;
import org.eclipse.jpt.jpa.core.JpaPlatform;
import org.eclipse.jpt.jpa.core.JpaProject;
import org.eclipse.jpt.jpa.core.JpaWorkspace;
import org.eclipse.jpt.jpa.core.internal.facet.JpaFacetDataModelProperties;
import org.eclipse.jpt.jpa.core.internal.facet.JpaFacetInstallDataModelProperties;
import org.eclipse.jpt.jpa.core.internal.facet.JpaFacetInstallDataModelProvider;
import org.eclipse.jpt.jpa.core.internal.resource.persistence.PersistenceXmlResourceProvider;
import org.eclipse.jpt.jpa.core.platform.JpaPlatformManager;
import org.eclipse.jpt.jpa.core.resource.persistence.XmlPersistenceUnit;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.common.project.facet.core.internal.JavaFacetUtil;
import org.eclipse.jst.common.project.facet.core.libprov.ILibraryProvider;
import org.eclipse.jst.common.project.facet.core.libprov.LibraryInstallDelegate;
import org.eclipse.jst.common.project.facet.core.libprov.LibraryProviderFramework;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ILifecycleMappingConfiguration;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.eclipse.m2e.wtp.MavenWtpPlugin;
import org.eclipse.m2e.wtp.ProjectUtils;
import org.eclipse.m2e.wtp.ResourceCleaner;
import org.eclipse.m2e.wtp.WTPProjectsUtil;
import org.eclipse.m2e.wtp.facets.FacetDetectorManager;
import org.eclipse.m2e.wtp.jpa.PlatformIdentifierManager;
import org.eclipse.m2e.wtp.jpa.internal.MavenJpaActivator;
import org.eclipse.m2e.wtp.jpa.internal.util.JptUtils;
import org.eclipse.wst.common.componentcore.ModuleCoreNature;
import org.eclipse.wst.common.componentcore.datamodel.properties.IFacetDataModelProperties;
import org.eclipse.wst.common.componentcore.internal.util.IModuleConstants;
import org.eclipse.wst.common.frameworks.datamodel.DataModelFactory;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IFacetedProject.Action;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

/**
 * JPA Project configurator. Will install the JPA facet on a maven project containing a persistence.xml.
 *
 * @author Fred Bricon
 */
@SuppressWarnings("restriction")
public class JpaProjectConfigurator extends AbstractProjectConfigurator {

	private static final String JPA_NO_OP_LIBRARY_PROVIDER = "jpa-no-op-library-provider"; //$NON-NLS-1$
	
	private static final String M2E_JPA_ACTIVATION_PROPERTY = "m2e.jpa.activation"; //$NON-NLS-1$
	
	static final String PERSISTENCE_XML_KEY = "persistencexml";  //$NON-NLS-1$
	
	@Override
	public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
		
		if(!canConfigure(request.getMavenProjectFacade(), monitor)) {
			return;
		}
		IProject project = request.getProject();
		MavenProject mavenProject = request.getMavenProject();

		IFile persistenceXml = getPersistenceXml(request.getMavenProjectFacade());
		if (persistenceXml == null || !persistenceXml.exists()) {
			//No persistence.xml => not a JPA project 
			return;
		}
		
		IFacetedProject facetedProject = ProjectFacetsManager.create(project, true, monitor);
		if (facetedProject != null) {
			//Refresh parent in multi-module setups, or Dali throws an exception 
			ProjectUtils.refreshHierarchy(mavenProject.getBasedir(), 
										  IResource.DEPTH_INFINITE, 
										  new SubProgressMonitor(monitor, 1));
			
			//Configurators should *never* create files in the user's source folders
			ResourceCleaner cleaner = new ResourceCleaner(facetedProject.getProject());
			addFoldersToClean(cleaner, request.getMavenProjectFacade());
			try {
				configureFacets(facetedProject, request.getMavenProjectFacade(), persistenceXml, monitor);
			} finally {
				cleaner.cleanUp();
			}
		} 
	}

	private IFile getPersistenceXml(IMavenProjectFacade mavenProjectFacade) {
		MavenResourceLocator resourceLocator = new MavenResourceLocator();
		IPath path = resourceLocator.lookupMavenResources(mavenProjectFacade, new Path("META-INF/persistence.xml")); //$NON-NLS-1$
		IFile persistenceXml = null;
		if (path != null) {
			persistenceXml = ResourcesPlugin.getWorkspace().getRoot().getFile(path);		
		}
		return persistenceXml;
	}

	private void configureFacets(IFacetedProject facetedProject, IMavenProjectFacade mavenProjectFacade, IFile persistenceXml, IProgressMonitor monitor)
			throws CoreException {
		
		//Need to refresh the persistence.xml as the resource provider might crash badly on some occasions
		//if it finds the file is out-of-sync
		persistenceXml.refreshLocal(IResource.DEPTH_ZERO, null);
		
		PersistenceXmlResourceProvider provider = PersistenceXmlResourceProvider.getXmlResourceProvider(persistenceXml);
		
		JptXmlResource jpaXmlResource = provider.getXmlResource(); 
		Map<?,?> context = Collections.singletonMap(PERSISTENCE_XML_KEY, jpaXmlResource);
		FacetDetectorManager facetDetectorManager = FacetDetectorManager.getInstance();
		
		IProjectFacetVersion version = facetDetectorManager.findFacetVersion(mavenProjectFacade, JpaProject.FACET.getId(), context, monitor);
		if (version == null) {
			return;
		}
		
		JpaPlatform.Config platform = getPlatform(jpaXmlResource, version);
		
		IDataModel dataModel = getDataModel(facetedProject, version, platform);

		Set<Action> actions = new LinkedHashSet<>();
		installJavaFacet(actions, facetedProject.getProject(), facetedProject);
		actions.add(new IFacetedProject.Action(IFacetedProject.Action.Type.INSTALL, 
								                version, 
								                dataModel));
		facetedProject.modify(actions, monitor);
	}

	
	private JpaPlatform.Config getPlatform(JptXmlResource persistenceXml, IProjectFacetVersion facetVersion) {
		XmlPersistenceUnit xmlPersistenceUnit = JptUtils.getFirstXmlPersistenceUnit(persistenceXml);
		if (xmlPersistenceUnit == null) {
			return null;
		}
		PlatformIdentifierManager identifierManager = MavenJpaActivator.getDefault().getPlatformIdentifierManager();
		String platformType = identifierManager.identify(xmlPersistenceUnit);
		JpaPlatformManager platformManager = getPlatformManager();
		if (platformType != null) {
			for (JpaPlatform.Config platform : platformManager.getJpaPlatformConfigs(facetVersion)) {
				if (platform.getId().contains(platformType)) {
					return platform;
				}
			}
		}
		//If no adequate platform found, Dali will use a default one.
		return null;
	}
	
	private JpaPlatformManager getPlatformManager() {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		JpaWorkspace jpaWorkspace = (JpaWorkspace) workspace.getAdapter(JpaWorkspace.class);
		return jpaWorkspace.getJpaPlatformManager();
	}

	private IDataModel getDataModel(IFacetedProject facetedProject,
									IProjectFacetVersion version, 
									JpaPlatform.Config platformConfig) {
		
		IDataModel dm = DataModelFactory.createDataModel(new JpaFacetInstallDataModelProvider()); 

		dm.setProperty(IFacetDataModelProperties.FACET_VERSION_STR, version.getVersionString()); 
		dm.setProperty(JpaFacetDataModelProperties.PLATFORM, platformConfig); 
		//Gone in Kepler M7 dm.setProperty(JpaFacetInstallDataModelProperties.CREATE_ORM_XML, false);
		dm.setProperty(JpaFacetInstallDataModelProperties.DISCOVER_ANNOTATED_CLASSES, true);
		LibraryInstallDelegate libraryInstallDelegate = getNoOpLibraryProvider(facetedProject, version);
		dm.setProperty(JpaFacetInstallDataModelProperties.LIBRARY_PROVIDER_DELEGATE, libraryInstallDelegate);
		
		return dm;
	}

	private boolean canConfigure(IMavenProjectFacade facade, IProgressMonitor monitor) throws CoreException {
		if (!WTPProjectsUtil.isLastConfigurator(facade, getClass(), getId())) {
			return false;
		}
		boolean enabled = isConfigurationEnabled(facade, monitor);
		if (!enabled) {
			return false;
		}
		IProject project = facade.getProject();
		// Bug 430178 : If imported project has modulecore nature without the component file, 
		// Dali's ModuleResourceLocator#getRootFolder will NPE (ex: it.cosenonjaviste:jsf2-spring4-jpa2-archetype:1.0.3)
		if (!project.hasNature(JavaCore.NATURE_ID) || 
				(project.hasNature(IModuleConstants.MODULE_NATURE_ID) && !ModuleCoreNature.componentResourceExists(project))) {
			return false;
		}
		
		IFacetedProject fProj = ProjectFacetsManager.create(project);
		return  fProj == null || !fProj.hasProjectFacet(JpaProject.FACET);
	}

	private boolean isConfigurationEnabled(IMavenProjectFacade facade, IProgressMonitor monitor) throws CoreException {
		if (WTPProjectsUtil.isM2eWtpDisabled(facade, monitor)) {
			return false;
		}
		
		Object pomActivationValue = facade.getMavenProject(monitor).getProperties().get(M2E_JPA_ACTIVATION_PROPERTY);
		boolean enabled;
		if (pomActivationValue == null) {
			enabled = MavenWtpPlugin.getDefault().getMavenWtpPreferencesManager().isEnabled(getId());
		} else {
			enabled = Boolean.parseBoolean(pomActivationValue.toString());
		}	
		return enabled;
	}
	
	private LibraryInstallDelegate getNoOpLibraryProvider(IFacetedProject facetedProject, IProjectFacetVersion facetVersion) {
		LibraryInstallDelegate libraryDelegate = new LibraryInstallDelegate(facetedProject, facetVersion);
		ILibraryProvider provider = LibraryProviderFramework.getProvider(JPA_NO_OP_LIBRARY_PROVIDER); 
		libraryDelegate.setLibraryProvider(provider);
		return libraryDelegate;
	}
	
	@Override
	public boolean hasConfigurationChanged(IMavenProjectFacade newFacade,
			ILifecycleMappingConfiguration oldProjectConfiguration,
			MojoExecutionKey key, IProgressMonitor monitor) {
		//Changes to maven-compiler-plugin in pom.xml don't make it "dirty" wrt JPA config
		return false;
	}
	
	private void installJavaFacet(Set<Action> actions, IProject project, IFacetedProject facetedProject) {
	    IProjectFacetVersion javaFv = JavaFacet.FACET.getVersion(JavaFacetUtil.getCompilerLevel(project));
	    if(!facetedProject.hasProjectFacet(JavaFacet.FACET)) {
	      actions.add(new IFacetedProject.Action(IFacetedProject.Action.Type.INSTALL, javaFv, null));
	    } else if(!facetedProject.hasProjectFacet(javaFv)) {
	      actions.add(new IFacetedProject.Action(IFacetedProject.Action.Type.VERSION_CHANGE, javaFv, null));
	    } 
	}
	
	 protected void addFoldersToClean(ResourceCleaner fileCleaner, IMavenProjectFacade facade) {
		    for (IPath p : facade.getCompileSourceLocations()) {
		      if (p != null) {
		        fileCleaner.addFiles(p.append("META-INF/persistence.xml")); //$NON-NLS-1$
		        fileCleaner.addFiles(p.append("META-INF/orm.xml")); //$NON-NLS-1$
				fileCleaner.addFolder(p);
		      }
		    }
		    for (IPath p : facade.getResourceLocations()) {
		      if (p != null) {
			    fileCleaner.addFiles(p.append("META-INF/persistence.xml")); //$NON-NLS-1$
			    fileCleaner.addFiles(p.append("META-INF/orm.xml")); //$NON-NLS-1$
		        fileCleaner.addFolder(p);
		      }
		    }
		    for (IPath p : facade.getTestCompileSourceLocations()) {
		      if (p != null) {
				fileCleaner.addFolder(p);
			}
		    }
		    for (IPath p : facade.getTestResourceLocations()) {
		      if (p != null) {
				fileCleaner.addFolder(p);
			}
		    }
		  }
}
