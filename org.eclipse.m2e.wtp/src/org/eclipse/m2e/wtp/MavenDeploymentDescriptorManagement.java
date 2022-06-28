/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.wtp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Set;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.markers.IMavenMarkerManager;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.wtp.earmodules.EarModule;
import org.eclipse.m2e.wtp.internal.Messages;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonNavigator;


/**
 * Deployment Descriptor Management based on maven-ear-plugin invocation
 * 
 * @provisional This class has been added as part of a work in progress. 
 * It is not guaranteed to work or remain the same in future releases. 
 * For more information contact <a href="mailto:m2e-wtp-dev@eclipse.org">m2e-wtp-dev@eclipse.org</a>.
 * 
 * @author Fred Bricon
 * @author Snjezana Peco
 */
@SuppressWarnings("restriction")
public class MavenDeploymentDescriptorManagement implements DeploymentDescriptorManagement {

  private static final VersionRange VALID_EAR_PLUGIN_RANGE;
  static {
    try {
      VALID_EAR_PLUGIN_RANGE = VersionRange.createFromVersionSpec("[2.4.3,)"); //$NON-NLS-1$
    } catch(InvalidVersionSpecificationException ex) {
      //Can't happen
      throw new RuntimeException("Unable to create ear plugin version range from [2.4.3,)", ex); //$NON-NLS-1$
    }
  }

  /**
   * Executes ear:generate-application-xml goal to generate application.xml (and jboss-app.xml if needed). Existing
   * files will be overwritten.
   * 
   * @throws CoreException
   */

  @Override
public void updateConfiguration(IProject project, MavenProject mavenProject, EarPluginConfiguration plugin,
     boolean useBuildDirectory, IProgressMonitor monitor) throws CoreException {

    IMavenProjectRegistry projectManager = MavenPlugin.getMavenProjectRegistry();
    IMavenProjectFacade mavenFacade = projectManager.getProject(project);
    IMavenMarkerManager markerManager  = MavenPluginActivator.getDefault().getMavenMarkerManager();
    
    IFile pomResource = project.getFile(IMavenConstants.POM_FILE_NAME);
    markerManager.deleteMarkers(pomResource, MavenWtpConstants.WTP_MARKER_GENERATE_APPLICATIONXML_ERROR);

    IMavenExecutionContext executionContext = mavenFacade.createExecutionContext();
    //Create a maven request + session
    IMaven maven = MavenPlugin.getMaven();

    MavenExecutionPlan executionPlan = maven.calculateExecutionPlan(mavenProject, Collections.singletonList("ear:generate-application-xml"), true, monitor); //$NON-NLS-1$
    MojoExecution genConfigMojo = getExecution(executionPlan, "maven-ear-plugin", "generate-application-xml"); //$NON-NLS-1$ //$NON-NLS-2$
    if(genConfigMojo == null) {
      //TODO Better error management
      return;
    }
    
    //Let's force the generated config files location
    Xpp3Dom configuration = genConfigMojo.getConfiguration();
    if(configuration == null) {
      configuration = new Xpp3Dom("configuration"); //$NON-NLS-1$
      genConfigMojo.setConfiguration(configuration);
    }
    
    File tempDirectory;
    try {
      tempDirectory = getTempDirectory();
    } catch(IOException ex) {
      IStatus status = new Status(IStatus.ERROR, MavenWtpPlugin.ID, ex.getLocalizedMessage(), ex);
      throw new CoreException(status);
    }

    // Some old maven-ear-plugin have a dependency on an old plexus-util version that prevents
    // using workdirectory == generatedDescriptorLocation, so we keep them separated 
    File generatedDescriptorLocation = new File(tempDirectory, "generatedDescriptorLocation"); //$NON-NLS-1$
    File workDirectory = new File(tempDirectory, "workDirectory"); //$NON-NLS-1$
    
    Xpp3Dom workDirectoryDom = configuration.getChild("workDirectory"); //$NON-NLS-1$
    if(workDirectoryDom == null) {
      workDirectoryDom = new Xpp3Dom("workDirectory"); //$NON-NLS-1$
      configuration.addChild(workDirectoryDom);
    }
    workDirectoryDom.setValue(workDirectory.getAbsolutePath());

    Xpp3Dom genDescriptorLocationDom = configuration.getChild("generatedDescriptorLocation"); //$NON-NLS-1$
    if(genDescriptorLocationDom == null) {
      genDescriptorLocationDom = new Xpp3Dom("generatedDescriptorLocation"); //$NON-NLS-1$
      configuration.addChild(genDescriptorLocationDom);
    }
    genDescriptorLocationDom.setValue(generatedDescriptorLocation.getAbsolutePath());
    

    // Fix for http://jira.codehaus.org/browse/MEAR-116?focusedCommentId=232316&page=com.atlassian.jira.plugin.system.issuetabpanels%3Acomment-tabpanel#action_232316
    // affecting maven-ear-plugin version < 2.4.3
    if(!VALID_EAR_PLUGIN_RANGE.containsVersion(new DefaultArtifactVersion(genConfigMojo.getVersion()))) {
      overrideModules(configuration, plugin.getAllEarModules());
    }

    //Execute our modified mojo
    executionContext.execute(mavenFacade.getMavenProject(), genConfigMojo, monitor);
    
    MavenSession session = executionContext.getSession();
	if (session.getResult().hasExceptions()){
      markerManager.addMarkers(mavenFacade.getPom(), MavenWtpConstants.WTP_MARKER_GENERATE_APPLICATIONXML_ERROR, session.getResult());
    }
    
    //Copy generated files to their final location
    File[] files = generatedDescriptorLocation.listFiles();

    //MECLIPSEWTP-56 : application.xml should not be generated in the source directory
    
    IFolder targetFolder;
    IFolder earResourcesFolder = getEarResourcesDir(project, mavenProject, monitor); 
    if (useBuildDirectory) {
      targetFolder = earResourcesFolder;
    } else {
      targetFolder = project.getFolder(plugin.getEarContentDirectory(project));

      if (earResourcesFolder.exists() && earResourcesFolder.isAccessible()) {
        earResourcesFolder.delete(true, monitor);
      }
    }
    
    IFolder metaInfFolder = targetFolder.getFolder("/META-INF/"); //$NON-NLS-1$

    if (files != null && files.length > 0) {
    	for (File file:files) {
    		if (!metaInfFolder.exists()) {
    			ProjectUtils.createFolder(metaInfFolder, monitor);
    		}
    		InputStream is = null;
    		try {
    			is = new FileInputStream(file);
    			IFile targetResource = metaInfFolder.getFile(file.getName());
    			if (targetResource.exists()) {
    				targetResource.setContents(is, IResource.FORCE, monitor);
    			} else {
    				targetResource.create(is, true, monitor);
    			}
    		} catch (FileNotFoundException ex) {
    			IStatus status = new Status(IStatus.ERROR, MavenWtpPlugin.ID, IStatus.ERROR, ex.getMessage(), ex);
    	        throw new CoreException(status);
			} finally {
    			if (is != null) {
    				try {
						is.close();
					} catch (IOException ex) {
						// ignore
					}
    			}
    		}
    	}
    }
    
    deleteDirectory(generatedDescriptorLocation);
    
    Display.getDefault().asyncExec(new Runnable() {
		@Override
		public void run() {
			if (PlatformUI.isWorkbenchRunning()) {
				IViewPart view = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(IPageLayout.ID_PROJECT_EXPLORER);
				if (view instanceof CommonNavigator) {
					CommonNavigator navigator = (CommonNavigator) view;
					navigator.getCommonViewer().refresh();
				}
			}
		}
	});
  }

  private IFolder getEarResourcesDir(IProject project, MavenProject mavenProject, IProgressMonitor monitor)
      throws CoreException {
    String appResourcesDir = ProjectUtils.getM2eclipseWtpFolder(mavenProject, project).toPortableString()+Path.SEPARATOR+MavenWtpConstants.EAR_RESOURCES_FOLDER;
    IFolder appResourcesFolder = project.getFolder(appResourcesDir);
 
    if (!appResourcesFolder.exists()) {
      ProjectUtils.createFolder(appResourcesFolder, monitor);
    }
    if (!appResourcesFolder.isDerived()) {
      appResourcesFolder.setDerived(true, monitor);
    }
    return appResourcesFolder;
  }


  private void overrideModules(Xpp3Dom configuration, Set<EarModule> earModules) {
    Xpp3Dom modules = configuration.getChild("modules"); //$NON-NLS-1$
    if(modules == null) {
      modules = new Xpp3Dom("modules"); //$NON-NLS-1$
      configuration.addChild(modules);
    }
    //TODO find a more elegant way to clear the modules  
    while(modules.getChildCount() > 0) {
      modules.removeChild(0);
    }
    //Recreate the module's children, forcing the uri.
    for(EarModule earModule : earModules) {
      modules.addChild(earModule.getAsDom());
    }
  }

  private File getTempDirectory() throws IOException {
    File tempDir = new File(System.getProperty("java.io.tmpdir")); //$NON-NLS-1$
    File dir = new File(tempDir, ".mavenDeploymentDescriptorManagement"); //$NON-NLS-1$
    if(dir.exists()) {
      if(dir.isFile()) {
        if(!dir.delete()) {
          throw new IOException(NLS.bind(Messages.MavenDeploymentDescriptorManagement_Error_Deleting_Temp_Folder, dir.getAbsolutePath()));
        }
      } else if(!deleteDirectory(dir)) {
          throw new IOException(NLS.bind(Messages.MavenDeploymentDescriptorManagement_Error_Deleting_Temp_Folder, dir.getAbsolutePath())); 
      }
    }
    dir.mkdir();
    return dir;
  }

  private static boolean deleteDirectory(File path) {
    if(path.exists()) {
      File[] files = path.listFiles();
      for(int i = 0; i < files.length; i++ ) {
        if(files[i].isDirectory()) {
          deleteDirectory(files[i]);
        } else {
          files[i].delete();
        }
      }
    }
    return (path.delete());
  }

  private MojoExecution getExecution(MavenExecutionPlan executionPlan, String artifactId, String goal) throws CoreException {
    for(MojoExecution execution : executionPlan.getMojoExecutions()) {
      if(artifactId.equals(execution.getArtifactId()) && goal.equals(execution.getGoal())) {
        return execution;
      }
    }
    return null;
  }
}
