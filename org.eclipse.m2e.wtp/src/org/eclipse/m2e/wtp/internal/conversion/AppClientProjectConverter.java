/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.wtp.internal.conversion;

import static org.eclipse.m2e.wtp.DomUtils.getOrCreateChildNode;

import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.m2e.wtp.WTPProjectsUtil;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;

/**
 * Converts Eclipse WTP Application Client project settings into maven-acr-plugin configuration 
 *
 * @author Fred Bricon
 */
public class AppClientProjectConverter extends AbstractWtpProjectConversionParticipant {

  private static final String MAIN_CLASS = "Main-Class"; //$NON-NLS-1$
  
  @Override
public void convert(IProject project, Model model, IProgressMonitor monitor) throws CoreException {
    if (!accept(project) || !"app-client".equals(model.getPackaging())) { //$NON-NLS-1$
      return;
    }
    IVirtualComponent component = ComponentCore.createComponent(project);
    if (component == null) {
      return;
    }

    setAcrPlugin(component, model);
  }

  private void setAcrPlugin(IVirtualComponent component, Model model) {
    Build build = getCloneOrCreateBuild(model);
    String pluginVersion = getAcrPluginVersion();
    Plugin acrPlugin = setPlugin(build, "org.apache.maven.plugins", "maven-acr-plugin", pluginVersion); //$NON-NLS-1$ //$NON-NLS-2$
    acrPlugin.setExtensions(true);
    
    String mainClass = getMainClass(component.getProject());
    if (mainClass != null) {
      configureManifest(acrPlugin, mainClass);
    }
    
    model.setBuild(build);
  }

  private void configureManifest(Plugin acrPlugin, String mainClass) {
    Xpp3Dom archiver = getArchiver(acrPlugin);
    
    Xpp3Dom manifestEntriesDom = getOrCreateChildNode(archiver, "manifestEntries"); //$NON-NLS-1$
    
    for (Xpp3Dom entry : manifestEntriesDom.getChildren()) {
      if (MAIN_CLASS.equals(entry.getName()) && StringUtils.isNotEmpty(entry.getValue())) {
        //Main already set, no need to go further
        return;
      }
    }
    
    Xpp3Dom  main = new Xpp3Dom(MAIN_CLASS);
    main.setValue(mainClass);
    manifestEntriesDom.addChild(main);
  }
  
  private Xpp3Dom getArchiver(Plugin plugin) {
    Xpp3Dom config = getOrCreateConfiguration(plugin);
    plugin.setConfiguration(config);
    return getOrCreateChildNode(config, "archive"); //$NON-NLS-1$
  }
  
  private String getMainClass(IProject project) {
    //Get Main attribute from existing MANIFEST.MF
    IJavaProject javaProject = JavaCore.create(project);
    if (javaProject == null) {
      return null;
    }
    IFile manifest = null;
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    IClasspathEntry[] classpath;
    try {
      classpath = javaProject.getRawClasspath();
      for (IClasspathEntry cpe : classpath) {
        if (IClasspathEntry.CPE_SOURCE == cpe.getEntryKind()) {
          IFile sourceManifest = root.getFile(cpe.getPath().append("META-INF/MANIFEST.MF"));   //$NON-NLS-1$
          if (sourceManifest.exists()) {
            manifest = sourceManifest;
            break;
          }
        }
      }
    } catch(JavaModelException ex) {
      // TODO proper logging
      ex.printStackTrace();
    }
    
    if (manifest == null) {
      return null;
    }
    
    InputStream is = null;
    try {
      is = manifest.getContents();
      Manifest mf = new Manifest(is);
      Attributes mainAttributes = mf.getMainAttributes();
      return mainAttributes.getValue(MAIN_CLASS);
    } catch (Exception ex) {
      ex.printStackTrace();//TODO proper logging
    } finally {
      IOUtil.close(is);
    }
    return null;
  }

  @Override
  protected IProjectFacet getRequiredFaced() {
    return WTPProjectsUtil.APP_CLIENT_FACET;
  }

  private String getAcrPluginVersion() {
	 //For test purposes only, must not be considered API behavior.
	 String version = System.getProperty("org.eclipse.m2e.wtp.conversion.acrplugin.version");//$NON-NLS-1$
	 if(version != null) {
	   return version;
	 }
	 return MavenPluginUtils.getMostRecentPluginVersion("org.apache.maven.plugins", "maven-acr-plugin", "3.1.0"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
  }
}
