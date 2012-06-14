/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
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
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.common.componentcore.ModuleCoreNature;


/**
 * @author Fred Bricon
 */
public class UnsupportedDependencyTypeProjectConfigurator extends AbstractProjectConfigurator {

  private static final Set<String> UNSUPPORTED_DEPENDENCY_TYPES;

  static {
    UNSUPPORTED_DEPENDENCY_TYPES = new HashSet<String>(Arrays.asList(new String[] {"ejb-client", "test-jar"}));
  }

  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
    //Nothing to configure
  }

  public void mavenProjectChanged(MavenProjectChangedEvent event, IProgressMonitor monitor) throws CoreException {
    IMavenProjectFacade facade = event.getMavenProject();
    if(facade == null) {
      return;
    }
    
    clearWarnings(facade.getPom());

    if (!ModuleCoreNature.isFlexibleProject(facade.getProject())) {
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
        String dType = (StringUtils.isBlank(d.getType()))?"jar":d.getType();
        String aType = (StringUtils.isBlank(a.getType()))?"jar":a.getType();
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

}
