/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp;

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.eclipse.m2e.jdt.IClasspathDescriptor;
import org.eclipse.m2e.jdt.IJavaProjectConfigurator;
import org.eclipse.m2e.wtp.internal.filtering.EarResourceBuildParticipant;
import org.eclipse.m2e.wtp.internal.filtering.ResourceFilteringBuildParticipant;
import org.eclipse.wst.common.componentcore.ModuleCoreNature;
import org.eclipse.wst.validation.ValidationFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Project configurator for WTP projects. Specific project configuration is delegated to the
 * IProjectConfiguratorDelegate bound to a maven packaging type.
 *
 * @provisional This class has been added as part of a work in progress. 
 * It is not guaranteed to work or remain the same in future releases. 
 * For more information contact <a href="mailto:m2e-wtp-dev@eclipse.org">m2e-wtp-dev@eclipse.org</a>.
 * 
 * @author Igor Fedorenko
 * @author Fred Bricon
 */
public class WTPProjectConfigurator extends AbstractProjectConfigurator implements IJavaProjectConfigurator {

  private static final Logger LOG = LoggerFactory.getLogger(WTPProjectConfigurator.class); 
      
  @Override
  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor)
      throws CoreException {
    MavenProject mavenProject = request.getMavenProject();
    //Lookup the project configurator 
    IProjectConfiguratorDelegate configuratorDelegate = ProjectConfiguratorDelegateFactory
        .getProjectConfiguratorDelegate(mavenProject.getPackaging());
    if(configuratorDelegate != null) {
      IProject project = request.getProject();
      if (project.getResourceAttributes().isReadOnly()){
        return;
      }

      try {
        configuratorDelegate.configureProject(project, mavenProject, monitor);
      } catch(MarkedException ex) {
        LOG.error(ex.getMessage(), ex);
      }
      
      IFolder buildFolder = project.getFolder(ProjectUtils.getBuildFolder(mavenProject, project));
      ValidationFramework.getDefault().disableValidation(buildFolder);
    }
  }

  @Override
  public void mavenProjectChanged(MavenProjectChangedEvent event, IProgressMonitor monitor) throws CoreException {
    IMavenProjectFacade facade = event.getMavenProject();

    if(facade != null) {
      IProject project = facade.getProject();
      if (project.getResourceAttributes().isReadOnly()){
        return;
      }

      if(isWTPProject(project)) {
        MavenProject mavenProject = facade.getMavenProject(monitor);
        IProjectConfiguratorDelegate configuratorDelegate = ProjectConfiguratorDelegateFactory
            .getProjectConfiguratorDelegate(mavenProject.getPackaging());
        if(configuratorDelegate != null) {
          configuratorDelegate.setModuleDependencies(project, mavenProject, monitor);
        }
      }
    }
  }

  protected static boolean isWTPProject(IProject project) {
    return ModuleCoreNature.isFlexibleProject(project);
  }

  @Override
public void configureClasspath(IMavenProjectFacade facade, IClasspathDescriptor classpath, IProgressMonitor monitor)
      throws CoreException {
    MavenProject mavenProject = facade.getMavenProject(monitor);
    //Lookup the project configurator 
    IProjectConfiguratorDelegate configuratorDelegate = ProjectConfiguratorDelegateFactory
        .getProjectConfiguratorDelegate(mavenProject.getPackaging());
    if(configuratorDelegate != null) {
      IProject project = facade.getProject();
      try {
        configuratorDelegate.configureClasspath(project, mavenProject, classpath, monitor);
      } catch(CoreException ex) {
        LOG.error(ex.getMessage(), ex);
      }
    }
  }

  @Override
public void configureRawClasspath(ProjectConfigurationRequest request, IClasspathDescriptor classpath,
      IProgressMonitor monitor) throws CoreException {
    // we do not change raw project classpath, do we? 
  }

  @Override
  public AbstractBuildParticipant getBuildParticipant(IMavenProjectFacade projectFacade, MojoExecution execution,
      IPluginExecutionMetadata executionMetadata) {
    
    //FIXME should refactor that by removing the project configurator delegates
      if ("maven-war-plugin".equals(execution.getArtifactId()) && "war".equals(execution.getGoal())  //$NON-NLS-1$ //$NON-NLS-2$
        || "maven-acr-plugin".equals(execution.getArtifactId()) && "acr".equals(execution.getGoal())) { //$NON-NLS-1$ //$NON-NLS-2$
        return new ResourceFilteringBuildParticipant(); 
      } else if ("maven-ear-plugin".equals(execution.getArtifactId()) && "generate-application-xml".equals(execution.getGoal())) { //$NON-NLS-1$ //$NON-NLS-2$
    	return new EarResourceBuildParticipant(); 
      }
      return null;
  }
  
}
