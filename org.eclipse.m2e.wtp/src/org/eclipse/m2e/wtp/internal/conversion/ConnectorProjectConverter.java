/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp.internal.conversion;

import static org.eclipse.m2e.wtp.internal.conversion.MavenPluginUtils.configure;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.wtp.WTPProjectsUtil;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.eclipse.wst.common.componentcore.resources.IVirtualResource;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;

/**
 * Converts Eclipse WTP Connector project settings into maven-rar-plugin configuration 
 *
 * @author Fred Bricon
 */
public class ConnectorProjectConverter extends AbstractWtpProjectConversionParticipant {

  private static final String DEFAULT_RA_XML = "src/main/rar/META-INF/ra.xml";

  public void convert(IProject project, Model model, IProgressMonitor monitor) throws CoreException {
    if (!accept(project) || !"rar".equals(model.getPackaging())) {
      return;
    }
    IVirtualComponent component = ComponentCore.createComponent(project);
    if (component == null) {
      return;
    }

    //Setting the maven-jar-plugin is necessary in order to embed generated class files 
    setJarPlugin(component, model);
    
    setRarPlugin(component, model);
  }

  private void setJarPlugin(IVirtualComponent component, Model model) {
    Build build = getCloneOrCreateBuild(model);
    //maven-jar-plugin 2.4 is not supported by the mavenarchiver plugin
    //see https://github.com/sonatype/m2eclipse-extras/issues/10
    Plugin jarPlugin = setPlugin(build, "org.apache.maven.plugins", "maven-jar-plugin", "2.3.2");

    PluginExecution jarExecution = new PluginExecution();
    jarExecution.setId("build_jar");
    //Tell maven to package the project classes as a jar
    jarExecution.addGoal("jar");
    //The .jar must be created before the rar is packaged.
    jarExecution.setPhase("process-classes");
    jarPlugin.addExecution(jarExecution);

    model.setBuild(build);
  }

  private void setRarPlugin(IVirtualComponent component, Model model) throws CoreException {
    Build build = getCloneOrCreateBuild(model);
    String pluginVersion = MavenPluginUtils.getMostRecentPluginVersion("org.apache.maven.plugins", "maven-rar-plugin", "2.2");
    Plugin rarPlugin = setPlugin(build, "org.apache.maven.plugins", "maven-rar-plugin", pluginVersion);
    IFile raXml = findRaXml(component);
    if (raXml != null) {
      String raXmlPath = raXml.getProjectRelativePath().toPortableString();
      if (!DEFAULT_RA_XML.equals(raXmlPath)) {
        //Failing to set up non default ra.xml would make maven-rar-plugin crash
        configure(rarPlugin, "raXmlFile", raXmlPath);
        model.setBuild(build);
      }
    }
    
  }

  protected IProjectFacet getRequiredFaced() {
    return WTPProjectsUtil.JCA_FACET;
  }

  private IFile findRaXml(IVirtualComponent component) throws CoreException {
    for (IVirtualResource vr : component.getRootFolder().members()) {
      if (vr instanceof IVirtualFolder) {
        IFolder f = (IFolder)((IVirtualFolder) vr).getUnderlyingFolder();
        if ("META-INF".equals(f.getName())) {
          IFile ra = f.getFile("ra.xml");
          if (ra.isAccessible()) {
            return ra;
          }
        }
      }
    }
    return null;
  }
}
