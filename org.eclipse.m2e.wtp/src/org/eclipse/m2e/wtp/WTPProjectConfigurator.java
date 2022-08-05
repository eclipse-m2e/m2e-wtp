/*******************************************************************************
 * Copyright (c) 2008-2015 Sonatype, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.wtp;

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ILifecycleMappingConfiguration;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.eclipse.m2e.jdt.IClasspathDescriptor;
import org.eclipse.m2e.jdt.IJavaProjectConfigurator;
import org.eclipse.m2e.wtp.internal.build.WarProjectBuildParticipant;
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

    IProject project = request.mavenProjectFacade().getProject();
    if (!isMutable(project) 
    		|| WTPProjectsUtil.isM2eWtpDisabled(request.mavenProjectFacade(), monitor)) {
      return;
    }

    MavenProject mavenProject = request.mavenProject();
    //Lookup the project configurator
    IProjectConfiguratorDelegate configuratorDelegate = ProjectConfiguratorDelegateFactory
        .getProjectConfiguratorDelegate(mavenProject.getPackaging());
    if(configuratorDelegate != null) {
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
      if (!isMutable(project) 
    		  || !isWTPProject(project) || WTPProjectsUtil.isM2eWtpDisabled(facade, monitor)){
        return;
      }

      MavenProject mavenProject = facade.getMavenProject(monitor);
      IProjectConfiguratorDelegate configuratorDelegate = ProjectConfiguratorDelegateFactory
          .getProjectConfiguratorDelegate(mavenProject.getPackaging());
      if(configuratorDelegate != null) {
        configuratorDelegate.setModuleDependencies(project, mavenProject, monitor);
      }
    }
  }

  protected static boolean isWTPProject(IProject project) {
    return ModuleCoreNature.isFlexibleProject(project);
  }

  @Override
  public void configureClasspath(IMavenProjectFacade facade, IClasspathDescriptor classpath, IProgressMonitor monitor)
      throws CoreException {
    IProject project = facade.getProject();
    if (!isMutable(project) || WTPProjectsUtil.isM2eWtpDisabled(facade, monitor)) {
      return;
    }

    MavenProject mavenProject = facade.getMavenProject(monitor);
    //Lookup the project configurator
    IProjectConfiguratorDelegate configuratorDelegate = ProjectConfiguratorDelegateFactory
        .getProjectConfiguratorDelegate(mavenProject.getPackaging());
    if(configuratorDelegate != null) {
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

    if (!isMutable(projectFacade.getProject()) || WTPProjectsUtil.isM2eWtpDisabled(projectFacade, new NullProgressMonitor())) {
      return null;
    }

    //FIXME should refactor that by removing the project configurator delegates
    if ("maven-war-plugin".equals(execution.getArtifactId()) && "war".equals(execution.getGoal())) {//$NON-NLS-1$ //$NON-NLS-2$
    	return new WarProjectBuildParticipant();
    } else if ("maven-acr-plugin".equals(execution.getArtifactId()) && "acr".equals(execution.getGoal())) { //$NON-NLS-1$ //$NON-NLS-2$
      return new ResourceFilteringBuildParticipant();
    } else if ("maven-ear-plugin".equals(execution.getArtifactId()) && "generate-application-xml".equals(execution.getGoal())) { //$NON-NLS-1$ //$NON-NLS-2$
      return new EarResourceBuildParticipant();
    }
    return null;
  }


  @Override
  public boolean hasConfigurationChanged(IMavenProjectFacade newFacade,
      ILifecycleMappingConfiguration oldProjectConfiguration,
      MojoExecutionKey key, IProgressMonitor monitor) {

    if (WTPProjectsUtil.isM2eWtpDisabled(newFacade, monitor)) {
      return false;
    }

    return super.hasConfigurationChanged(newFacade, oldProjectConfiguration, key, monitor);
  }
  
  protected boolean isMutable(IProject project) {
	  return project != null && project.isAccessible() && project.getResourceAttributes() != null && !project.getResourceAttributes().isReadOnly();
  }
}
