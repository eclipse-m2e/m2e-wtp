/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp;

import static org.eclipse.m2e.wtp.WTPProjectsUtil.installJavaFacet;
import static org.eclipse.m2e.wtp.WTPProjectsUtil.removeTestFolderLinks;
import static org.eclipse.m2e.wtp.WTPProjectsUtil.removeWTPClasspathContainer;
import static org.eclipse.m2e.wtp.WTPProjectsUtil.setNonDependencyAttributeToContainer;

import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IFacetedProject.Action;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.validation.ValidationFramework;

/**
 * Utility Project Configurator. Allows to update the WTP configuration of Utility projects
 *  on Maven Update project configuration.<br/>  
 *  This configurator is secondary to the JavaConfigurator.
 *  
 * @provisional This class has been added as part of a work in progress. 
 * It is not guaranteed to work or remain the same in future releases. 
 * For more information contact <a href="mailto:m2e-wtp-dev@eclipse.org">m2e-wtp-dev@eclipse.org</a>.
 * 
 * @author Fred Bricon
 */
public class UtilityProjectConfigurator extends AbstractProjectConfigurator {

  @Override
public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {

    IProject project = request.getProject();
    if (!project.isAccessible()) {
      return;
    }
    MavenProject mavenProject = request.getMavenProject();
    
    IFacetedProject facetedProject = ProjectFacetsManager.create(project);

    // Only reconfigure utility projects 
    if(facetedProject != null && facetedProject.hasProjectFacet(WTPProjectsUtil.UTILITY_FACET)) {
      
      Set<Action> actions = new LinkedHashSet<Action>();
      installJavaFacet(actions, project, facetedProject);
      if(!actions.isEmpty()) {
        facetedProject.modify(actions, monitor);
      }

      removeWTPClasspathContainer(project);
      
      removeTestFolderLinks(project, mavenProject, monitor, "/");  //$NON-NLS-1$

      //MECLIPSEWTP-125 Remove "MAVEN2_CLASSPATH_CONTAINER will not be exported or published" warning.
      setNonDependencyAttributeToContainer(project, monitor);
      
      IFolder buildFolder = project.getFolder(ProjectUtils.getBuildFolder(mavenProject, project));
      ValidationFramework.getDefault().disableValidation(buildFolder);
    }
    
  }

}
