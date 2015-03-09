/*******************************************************************************
 * Copyright (c) 2008-2014 Sonatype, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.war.Overlay;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.ILifecycleMappingConfiguration;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.eclipse.m2e.jdt.IClasspathDescriptor;
import org.eclipse.m2e.wtp.internal.StringUtils;
import org.eclipse.m2e.wtp.overlay.modulecore.IOverlayVirtualComponent;
import org.eclipse.m2e.wtp.overlay.modulecore.OverlayComponentCore;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualReference;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

/**
 * Configurator for war overlays.
 * 
 * @provisional This class has been added as part of a work in progress. 
 * It is not guaranteed to work or remain the same in future releases. 
 * For more information contact <a href="mailto:m2e-wtp-dev@eclipse.org">m2e-wtp-dev@eclipse.org</a>.
 * 
 * @author Fred Bricon
 */
public class OverlayConfigurator extends WTPProjectConfigurator {

  @Override
  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor)
      throws CoreException {
	  mavenProjectChanged(request.getMavenProjectFacade(), monitor);
  }

  @Override
  public void mavenProjectChanged(MavenProjectChangedEvent event, IProgressMonitor monitor) throws CoreException {
	  mavenProjectChanged(event.getMavenProject(), monitor);
  }
  
  private void mavenProjectChanged(IMavenProjectFacade facade, IProgressMonitor monitor) throws CoreException {
    if(facade == null) { return; }
    IProject project = facade.getProject();
    if (WTPProjectsUtil.isM2eWtpDisabled(facade, monitor) || project.getResourceAttributes().isReadOnly()){
      return;
    }

    IFacetedProject facetedProject = ProjectFacetsManager.create(project, true, monitor);
    if(!facetedProject.hasProjectFacet(WebFacetUtils.WEB_FACET)) {
      return;
    }

    MavenProject mavenProject = facade.getMavenProject(monitor);
    try {
      markerManager.deleteMarkers(facade.getPom(), MavenWtpConstants.WTP_MARKER_OVERLAY_ERROR);
      setModuleDependencies(project, mavenProject, monitor);
    } catch(Exception ex) {
      markerManager.addErrorMarkers(facade.getPom(), MavenWtpConstants.WTP_MARKER_OVERLAY_ERROR,ex);
    }
  }

  /**
   * @param project
   * @param mavenProject
   * @param monitor
   * @throws CoreException 
   */
  private void setModuleDependencies(IProject project, MavenProject mavenProject, IProgressMonitor monitor) throws CoreException {

    IVirtualComponent warComponent = ComponentCore.createComponent(project);
    if (warComponent == null) {
      return;
    }
    
    Set<IVirtualReference> newOverlayRefs = new LinkedHashSet<>();
    MavenSessionHelper helper = new MavenSessionHelper(mavenProject);
    try {
      helper.ensureDependenciesAreResolved("maven-war-plugin", "war:war"); //$NON-NLS-1$ //$NON-NLS-2$
      
      MavenPlugin.getMaven();
      
    WarPluginConfiguration config = new WarPluginConfiguration(mavenProject, project);
    
    List<Overlay> overlays = config.getOverlays();
    //1 overlay = current project => no overlay component needed
    if (overlays.size() > 1) {

      //Component order must be inverted to follow maven's overlay order behaviour 
      //as in WTP, last components supersede the previous ones
      Collections.reverse(overlays);
      for(Overlay overlay : overlays) {

        if (overlay.shouldSkip()) {
          continue;
        }
        
        Artifact artifact = overlay.getArtifact();
        IOverlayVirtualComponent overlayComponent = null;
        IMavenProjectFacade workspaceDependency = projectManager.getMavenProject(
            artifact.getGroupId(), 
            artifact.getArtifactId(),
            artifact.getVersion());

        if(workspaceDependency != null) {
          //artifact dependency is a workspace project && dependency resolution is on
          IProject overlayProject = workspaceDependency.getProject();

          if (overlayProject.equals(project)) {
            overlayComponent = OverlayComponentCore.createSelfOverlayComponent(project);
          } else if (workspaceDependency.getFullPath(artifact.getFile()) != null){
            overlayComponent = OverlayComponentCore.createOverlayComponent(overlayProject);
          } else {
            //Dependency resolution is off
            overlayComponent = createOverlayArchiveComponent(project, mavenProject, overlay);
          }
        } else {
          overlayComponent = createOverlayArchiveComponent(project, mavenProject, overlay);
        }

        if (overlayComponent != null) {
          
          overlayComponent.setInclusions(new LinkedHashSet<>(Arrays.asList(overlay.getIncludes())));
          overlayComponent.setExclusions(new LinkedHashSet<>(Arrays.asList(overlay.getExcludes())));
          
          IVirtualReference depRef = ComponentCore.createReference(warComponent, overlayComponent);
          String targetPath = StringUtils.nullOrEmpty(overlay.getTargetPath())?"/":overlay.getTargetPath(); //$NON-NLS-1$
          depRef.setRuntimePath(new Path(targetPath));
          newOverlayRefs.add(depRef);
        }
      }
      
    }
    
    IVirtualReference[] oldOverlayRefs = WTPProjectsUtil.extractHardReferences(warComponent, true);
    
    IVirtualReference[] updatedOverlayRefs = newOverlayRefs.toArray(new IVirtualReference[newOverlayRefs.size()]);
    
    if (WTPProjectsUtil.hasChanged2(oldOverlayRefs, updatedOverlayRefs)){
      //Only write in the .component file if necessary 
      IVirtualReference[] nonOverlayRefs = WTPProjectsUtil.extractHardReferences(warComponent, false);
      IVirtualReference[] allRefs = new IVirtualReference[nonOverlayRefs.length + updatedOverlayRefs.length];
      System.arraycopy(nonOverlayRefs, 0, allRefs, 0, nonOverlayRefs.length);
      System.arraycopy(updatedOverlayRefs, 0, allRefs, nonOverlayRefs.length, updatedOverlayRefs.length);
      warComponent.setReferences(allRefs);
    }

    } finally {
      helper.dispose();
    }

  }

  private IOverlayVirtualComponent createOverlayArchiveComponent(IProject project, MavenProject mavenProject, Overlay overlay) throws CoreException {
    IPath m2eWtpFolder = ProjectUtils.getM2eclipseWtpFolder(mavenProject, project);
    IPath unpackDirPath = new Path(m2eWtpFolder.toOSString()+"/overlays"); //$NON-NLS-1$
    String archiveLocation = ArtifactHelper.getM2REPOVarPath(overlay.getArtifact());
    String targetPath = StringUtils.nullOrEmpty(overlay.getTargetPath())?"/":overlay.getTargetPath(); //$NON-NLS-1$
    IOverlayVirtualComponent component = OverlayComponentCore.createOverlayArchiveComponent(
                                                                project, 
                                                                archiveLocation, 
                                                                unpackDirPath, 
                                                                new Path(targetPath));
    return component;
  }
  
  
  @Override
  public void configureClasspath(IMavenProjectFacade facade, IClasspathDescriptor classpath, IProgressMonitor monitor)
      throws CoreException {
  }
  
  @Override
  public AbstractBuildParticipant getBuildParticipant(IMavenProjectFacade projectFacade, MojoExecution execution,
      IPluginExecutionMetadata executionMetadata) {
    return null;
  }
  
  @Override
  public boolean hasConfigurationChanged(IMavenProjectFacade newFacade,
			ILifecycleMappingConfiguration oldProjectConfiguration,
			MojoExecutionKey key, IProgressMonitor monitor) {
	//Changes to maven-war-plugin in pom.xml don't make it "dirty" 
	//wrt Overlay configuration (i.e. no need to invoke configure(request, monitor))
	return false;
  }
  
}
