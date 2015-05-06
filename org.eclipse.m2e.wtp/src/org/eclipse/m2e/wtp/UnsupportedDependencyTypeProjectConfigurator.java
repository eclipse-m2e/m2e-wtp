/*******************************************************************************
 * Copyright (c) 2012-2014 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp;

import static org.eclipse.m2e.wtp.MavenWtpConstants.WTP_MARKER_UNSUPPORTED_DEPENDENCY_PROBLEM;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.internal.markers.SourceLocation;
import org.eclipse.m2e.core.internal.markers.SourceLocationHelper;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ILifecycleMappingConfiguration;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.eclipse.m2e.wtp.internal.Messages;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.common.componentcore.ModuleCoreNature;


/**
 * Configurator used to display warnings on projects depending on unsupported Maven packages.
 * 
 * @provisional This class has been added as part of a work in progress. 
 * It is not guaranteed to work or remain the same in future releases. 
 * For more information contact <a href="mailto:m2e-wtp-dev@eclipse.org">m2e-wtp-dev@eclipse.org</a>.
 *
 * @author Fred Bricon
 * 
 */
public class UnsupportedDependencyTypeProjectConfigurator extends AbstractProjectConfigurator {

  private static final Set<String> UNSUPPORTED_DEPENDENCY_TYPES;

  static {
    UNSUPPORTED_DEPENDENCY_TYPES = new HashSet<>(Arrays.asList(new String[] {"ejb-client", "test-jar"})); //$NON-NLS-1$ //$NON-NLS-2$
  }

  @Override
  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
	  checkUnsupportedWorkspaceDependency(monitor, request.getMavenProjectFacade());
  }

  @Override
  public void mavenProjectChanged(MavenProjectChangedEvent event, IProgressMonitor monitor) throws CoreException {
    checkUnsupportedWorkspaceDependency(monitor, event.getMavenProject());
  }

/**
 * @param monitor
 * @param facade
 * @throws CoreException
 */
private void checkUnsupportedWorkspaceDependency(IProgressMonitor monitor,
		IMavenProjectFacade facade) throws CoreException {
	if(facade == null) {
      return;
    }
    
    clearWarnings(facade.getPom());

    if (WTPProjectsUtil.isM2eWtpDisabled(facade, monitor) || !ModuleCoreNature.isFlexibleProject(facade.getProject())) {
      //Not a WTP project
      return;
    }

    //Constraints only applies when workspace project resolution is active
    if(!facade.getResolverConfiguration().shouldResolveWorkspaceProjects()) {
      return;
    }

    for(Artifact a : facade.getMavenProject().getArtifacts()) {
      String type = a.getType();
      if(isUnsupported(type)) {
        IMavenProjectFacade workspaceDependency = getWorkspaceProject(a);
        if(workspaceDependency != null) {
          Dependency dependency = getDependency(a, facade.getMavenProject().getDependencies());
          int lineNumber = -1;
          if(dependency != null) {
            SourceLocation location = SourceLocationHelper.findLocation(facade.getMavenProject(), dependency);
            lineNumber = location.getLineNumber();
          }
          addWarning(facade.getPom(), workspaceDependency, type, lineNumber);
        }
      }
    }
}

  private void clearWarnings(IResource resource) throws CoreException {
    markerManager.deleteMarkers(resource, WTP_MARKER_UNSUPPORTED_DEPENDENCY_PROBLEM);
  }

  private boolean isUnsupported(String type) {
    return UNSUPPORTED_DEPENDENCY_TYPES.contains(type);
  }

  private IMavenProjectFacade getWorkspaceProject(Artifact artifact) {
    return projectManager.getMavenProject(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
  }

  private Dependency getDependency(Artifact a, List<Dependency> dependencies) {
    for(Dependency d : dependencies) {
      if(StringUtils.equals(a.getArtifactId(), d.getArtifactId()) && StringUtils.equals(a.getGroupId(), d.getGroupId())
          && StringUtils.equals(a.getVersion(), d.getVersion())
          && StringUtils.equals(a.getClassifier(), a.getClassifier())
        ) {
        String dType = (StringUtils.isBlank(d.getType()))?"jar":d.getType(); //$NON-NLS-1$
        String aType = (StringUtils.isBlank(a.getType()))?"jar":a.getType(); //$NON-NLS-1$
        if (aType.equals(dType)) {
          return d;
        }
      }
    }
    return null;
  }

  private void addWarning(IResource resource, IMavenProjectFacade workspaceDependency, String type, int lineNumber) {
    String warning = NLS.bind(Messages.markers_unsupported_dependencies_warning, workspaceDependency.getProject().getName(), type);
    markerManager.addMarker(resource, WTP_MARKER_UNSUPPORTED_DEPENDENCY_PROBLEM, warning, lineNumber,
        IMarker.SEVERITY_WARNING);
  }

  @Override
  public boolean hasConfigurationChanged(IMavenProjectFacade newFacade,
			ILifecycleMappingConfiguration oldProjectConfiguration,
			MojoExecutionKey key, IProgressMonitor monitor) {
	return false;
  }
  
}
