/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp.internal.conversion;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.m2e.core.project.conversion.AbstractProjectConversionParticipant;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

/**
 * AbstractWtpProjectConversionParticipant
 *
 * @author Fred Bricon
 */
public abstract class AbstractWtpProjectConversionParticipant extends AbstractProjectConversionParticipant {

  protected abstract IProjectFacet getRequiredFaced();
  
  protected static final String CONFIGURATION_KEY = "configuration"; //$NON-NLS-1$

  @Override
  public boolean accept(IProject project) throws CoreException {
    IFacetedProject fp = ProjectFacetsManager.create(project);
    if (fp != null && fp.hasProjectFacet(getRequiredFaced()))  {
      return true;
    }
    return false;
  }

  /**
   * @return a clone of the model's {@link Build} if it exists or a new instance.
   */
  protected Build getCloneOrCreateBuild(Model model) {
    Build build;
    if (model.getBuild() == null) {
      build = new Build();
    } else {
      build = model.getBuild().clone();
    }
    return build;
  }
  
  protected Plugin setPlugin(Build build, String pluginGroupId, String pluginArtifactId, String pluginVersion) {
    build.flushPluginMap();//We need to force the re-generation of the plugin map as it may be stale
    Plugin plugin = build.getPluginsAsMap().get(pluginGroupId+":"+pluginArtifactId); //$NON-NLS-1$  
    if (plugin == null) {
      plugin = build.getPluginsAsMap().get(pluginArtifactId);
    }
    if (plugin == null) {
      plugin = new Plugin();
      plugin.setGroupId(pluginGroupId);
      plugin.setArtifactId(pluginArtifactId);
      plugin.setVersion(pluginVersion);
      build.addPlugin(plugin);
    }
    
    return plugin;
  }
  
  protected Xpp3Dom getOrCreateConfiguration(Plugin plugin) {
    Xpp3Dom configuration = (Xpp3Dom)plugin.getConfiguration();
    if (configuration == null) {
      configuration = new Xpp3Dom(CONFIGURATION_KEY);
    }
    return configuration;
  }

}
