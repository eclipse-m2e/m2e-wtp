/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.lifecycle.internal.DependencyContext;
import org.apache.maven.lifecycle.internal.MojoExecutor;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.internal.embedder.MavenImpl;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;

/**
 * MavenSessionHelper
 *
 * @author Fred Bricon
 */
public class MavenSessionHelper {
  
  private final MavenProject project;

  private Set<Artifact> artifacts;

  private Set<Artifact> dependencyArtifacts; 

  public MavenSessionHelper(MavenProject mavenProject) {
    if (mavenProject == null) {
      throw new IllegalArgumentException("MavenProject can not be null");
    } 
    this.project = mavenProject;
  }
  
  public void ensureDependenciesAreResolved(String pluginId, String goal) throws CoreException {
    artifacts = project.getArtifacts();
    dependencyArtifacts = project.getDependencyArtifacts();
    IProgressMonitor monitor = new NullProgressMonitor();
    MavenSession session = getSession(monitor);
          
    MavenExecutionPlan executionPlan = MavenPlugin.getMaven().calculateExecutionPlan(session, 
                                                                                     project, 
                                                                                     Collections.singletonList(goal), 
                                                                                     true, 
                                                                                     monitor);
    
    MojoExecution execution = getExecution(executionPlan, pluginId);
    
    ensureDependenciesAreResolved(session, execution, monitor);
  }
  
  public void ensureDependenciesAreResolved(MavenSession session, MojoExecution execution, IProgressMonitor monitor) throws CoreException {
    artifacts = project.getArtifacts();
    dependencyArtifacts = project.getDependencyArtifacts();
    try {
               
      MojoExecutor mojoExecutor = lookup(MojoExecutor.class);
      DependencyContext dependencyContext = mojoExecutor.newDependencyContext(session,
          Collections.singletonList(execution));

      // workaround for http://jira.codehaus.org/browse/MNG-5141
      // use reflection until we can get maven 3.0.4+, which has MNG-5141 fixed
      Method ensureDependenciesAreResolved = mojoExecutor.getClass().getDeclaredMethod("ensureDependenciesAreResolved",
                                                                                        MojoDescriptor.class, 
                                                                                        MavenSession.class, 
                                                                                        DependencyContext.class);
      ensureDependenciesAreResolved.setAccessible(true);
      ensureDependenciesAreResolved.invoke(mojoExecutor, execution.getMojoDescriptor(), session, dependencyContext);

    } catch(Exception ex) {
      dispose();
    }
  }

  private MavenSession getSession(IProgressMonitor monitor) throws CoreException {
    IMavenProjectRegistry projectManager = MavenPlugin.getMavenProjectRegistry();
    IMavenProjectFacade mavenFacade = projectManager.getMavenProject(project.getGroupId(), 
                                                                     project.getArtifactId(), 
                                                                     project.getVersion());

    MavenExecutionRequest request = projectManager.createExecutionRequest(mavenFacade.getPom(), 
                                                                          mavenFacade.getResolverConfiguration(), 
                                                                          monitor);
    
    MavenSession session = MavenPlugin.getMaven().createSession(request, project);
    return session;
  }
  
  public void dispose() {
    project.setArtifactFilter(null);
    project.setResolvedArtifacts(null);
    project.setArtifacts(artifacts);
    project.setDependencyArtifacts(dependencyArtifacts);
  }
  
  private <T> T lookup(Class<T> clazz) throws CoreException {
    try {
      return ((MavenImpl)MavenPlugin.getMaven()).getPlexusContainer().lookup(clazz);
    } catch(ComponentLookupException ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1,
          Messages.MavenImpl_error_lookup, ex));
    }
  }

  public static MojoExecution getExecution(MavenExecutionPlan executionPlan, String artifactId) throws CoreException {
    if (executionPlan == null) return null;
    for(MojoExecution execution : executionPlan.getMojoExecutions()) {
      if(artifactId.equals(execution.getArtifactId()) ) {
        return execution;
      }
    }
    return null;
  }
}
