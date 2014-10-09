/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
  *     Fred Bricon (Red Hat, Inc.) - initial API and implementation
 ******************************************************************************/

package org.eclipse.m2e.wtp.internal.webfragment;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.wtp.WTPProjectsUtil;

/**
 * Utility class for WebFragment type projects
 * 
 * @author Fred Bricon
 * @since 1.2.0
 */
public class WebFragmentUtil {

	  /**
	   * Checks if a given {@link IMavenProjectFacade} qualifies as a Web Fragment project, 
	   * i.e, has a META-INF/web-fragment.xml file
	   * @param facade an {@link IMavenProjectFacade}
	   * @return true if a META-INF/web-fragment.xml file can be found in the project build directory or source folder.
	   */
	  public static boolean isQualifiedAsWebFragment(IMavenProjectFacade facade) {
	    return getWebFragment(facade) != null;
	  }
	  
	  /**
	   * Gets a {@link IMavenProjectFacade}' META-INF/web-fragment.xml, if it exists.
	   * @param facade an {@link IMavenProjectFacade}
	   * @return an {@link IFile} pointing to web-fragment.xml if it exists, <code>null</code> otherwise.
	   */
	  public static IFile getWebFragment(IMavenProjectFacade facade) {
	      IFile webFragment = null;
	      IFolder classes = WTPProjectsUtil.getClassesFolder(facade);
	      if (classes != null) {
	    	  webFragment = classes.getFile("META-INF/web-fragment.xml");//$NON-NLS-1$
	    	  if (webFragment.exists()) { 
	    		  return webFragment;
	    	  } 
	      }
	      
	      //No processed/filtered web-fragment.xml found 
	      //fall back : iterate over the resource folders
	      IProject project = facade.getProject();
	      for (IPath resourceFolderPath : facade.getResourceLocations()) {
	        if (resourceFolderPath != null) {
	        	webFragment = project.getFile(resourceFolderPath.append("META-INF/web-fragment.xml")); //$NON-NLS-1$
	        	if (webFragment.exists()) {
	        		return webFragment;
	        	}
	        }
	      }
	    return null;
	  }
}
