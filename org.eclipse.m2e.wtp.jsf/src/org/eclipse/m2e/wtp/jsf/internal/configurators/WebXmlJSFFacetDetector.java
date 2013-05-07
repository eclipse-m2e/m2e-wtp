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
package org.eclipse.m2e.wtp.jsf.internal.configurators;

import java.util.Map;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jst.j2ee.project.facet.IJ2EEFacetConstants;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.wtp.ProjectUtils;
import org.eclipse.m2e.wtp.facets.AbstractFacetDetector;
import org.eclipse.m2e.wtp.jsf.internal.MavenJSFConstants;
import org.eclipse.m2e.wtp.jsf.internal.Messages;
import org.eclipse.m2e.wtp.jsf.internal.utils.JSFUtils;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Inspects the project web.xml descriptor for a servlet facet declaration.
 * 
 * @author Fred Bricon
 * @since 0.18.0
 */
public class WebXmlJSFFacetDetector extends AbstractFacetDetector {

	private static final Logger LOG = LoggerFactory.getLogger(WebXmlJSFFacetDetector.class);

	@Override
	public IProjectFacetVersion findFacetVersion(IMavenProjectFacade mavenProjectFacade, Map<?, ?> context, IProgressMonitor monitor) throws CoreException {
		IProject project = mavenProjectFacade.getProject();
		if (project == null) {
			return null;
		}
		MavenProject mavenProject = mavenProjectFacade.getMavenProject(monitor);
		if (mavenProject == null) {
			return null;
		}

		IProjectFacetVersion version = null;
		if (hasFacesServletInWebXml(mavenProject, project)) {
			//Uses faces-servlet so we try to best guess the version depending on the installed web facet
			IFacetedProject fproj;
			try {
				fproj = ProjectFacetsManager.create(project);
				if (fproj != null) {
					IProjectFacetVersion webVersion = fproj .getInstalledVersion(IJ2EEFacetConstants.DYNAMIC_WEB_FACET);
					if (webVersion.compareTo(IJ2EEFacetConstants.DYNAMIC_WEB_30) < 0) {
						version = MavenJSFConstants.JSF_FACET_VERSION_1_2;
					} else {
						version = MavenJSFConstants.JSF_FACET_VERSION_2_0;
					}
				}
			} catch (CoreException e) {
				LOG.error(Messages.WebXmlJSFFacetDetector_Error_Cant_Detect_JSF_From_WebXml, e);
			}
		}
		return version;
	}
	
	private boolean hasFacesServletInWebXml(MavenProject mavenProject, IProject project) {
		//We look for javax.faces.webapp.FacesServlet in web.xml
		//We should look for a custom web.xml at this point, but WTP would then crash on the JSF Facet installation
		//if it's not in a standard location, so we stick with the regular file.
		IFile webXml = ProjectUtils.getWebResourceFile(project, "WEB-INF/web.xml"); //$NON-NLS-1$
		return webXml != null && webXml.exists() && JSFUtils.hasFacesServlet(webXml);
	}

}
