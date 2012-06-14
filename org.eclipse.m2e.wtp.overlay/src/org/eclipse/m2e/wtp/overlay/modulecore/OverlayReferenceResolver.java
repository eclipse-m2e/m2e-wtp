/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp.overlay.modulecore;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.emf.common.util.URI;
import org.eclipse.m2e.wtp.overlay.internal.modulecore.OverlaySelfComponent;
import org.eclipse.m2e.wtp.overlay.internal.modulecore.OverlayVirtualArchiveComponent;
import org.eclipse.m2e.wtp.overlay.internal.modulecore.OverlayVirtualComponent;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.internal.ComponentcorePackage;
import org.eclipse.wst.common.componentcore.internal.DependencyType;
import org.eclipse.wst.common.componentcore.internal.ReferencedComponent;
import org.eclipse.wst.common.componentcore.resolvers.IReferenceResolver;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualReference;

/**
 * Overlay Reference Resolver
 *
 * @author Fred Bricon
 */
@SuppressWarnings("restriction")
public class OverlayReferenceResolver implements IReferenceResolver {

  public static final String PROTOCOL = "module:/overlay/";
  
  public static final String PROJECT_PROTOCOL = PROTOCOL+"prj/";

  public static final String VAR_ARCHIVE_PROTOCOL = PROTOCOL+"var/";

  public static final String SELF_PROTOCOL = PROTOCOL+"slf/";

  private static final String UNPACK_FOLDER = "unpackFolder";
  
  private static final String INCLUDES = "includes";

  private static final String EXCLUDES = "excludes";

  public boolean canResolve(IVirtualComponent component, ReferencedComponent referencedComponent) {
    URI uri = referencedComponent.getHandle();
    return (uri.segmentCount() > 2) && (uri.segment(0).equals("overlay"));
  }

  public IVirtualReference resolve(IVirtualComponent component, ReferencedComponent referencedComponent) {
	String type = referencedComponent.getHandle().segment(1); 
    IOverlayVirtualComponent comp = null;
	String url = referencedComponent.getHandle().toString();
	Map<String, String> parameters = ModuleURIUtil.parseUri(url);
	
	String moduleName = ModuleURIUtil.extractModuleName(url);
	if (moduleName == null || moduleName.trim().length() == 0) {
		throw new IllegalArgumentException("module name can not be inferred from "+url);
	}
	
	if ("prj".equals(type)) {
		comp = createProjectComponent(component, moduleName.substring(PROJECT_PROTOCOL.length()));
	} else if ("var".equals(type)) {
		String unpackFolder = parameters.get(UNPACK_FOLDER);
		if (unpackFolder == null || unpackFolder.trim().length() == 0) {
			throw new IllegalArgumentException(url + " is missing the "+UNPACK_FOLDER+" parameter");
		}
		comp = createArchivecomponent(component, 
									  moduleName.substring(PROTOCOL.length()), 
									  unpackFolder, 
									  referencedComponent.getRuntimePath());
		
	} else if ("slf".equals(type)){
		comp = createSelfComponent(component);
	}
	if (comp == null) {
		throw new IllegalArgumentException(referencedComponent.getHandle() + " could not be resolved");
	}
	
	comp.setInclusions(getPatternSet(parameters.get(INCLUDES)));
	comp.setExclusions(getPatternSet(parameters.get(EXCLUDES)));

	IVirtualReference ref = ComponentCore.createReference(component, comp);
    ref.setArchiveName(referencedComponent.getArchiveName());
    ref.setRuntimePath(referencedComponent.getRuntimePath());
    ref.setDependencyType(referencedComponent.getDependencyType().getValue());
    return ref;
  }

  private Set<String> getPatternSet(String patterns) {
	if (patterns == null || patterns.trim().length() == 0) {
		return Collections.emptySet();
	}
	Set<String> patternSet = new LinkedHashSet<String>();
	for (String pattern : patterns.split(";")) {
		patternSet.add(pattern);
	}
	return patternSet;
  }

  private IOverlayVirtualComponent createSelfComponent(IVirtualComponent component) {
	  return new OverlaySelfComponent(component.getProject());
  }

  private IOverlayVirtualComponent createArchivecomponent(IVirtualComponent component, String url, String targetPath, IPath runtimePath) {
  	return new OverlayVirtualArchiveComponent(component.getProject(), 
  			url, 
  			component.getProject().getFolder(targetPath).getProjectRelativePath(), 
  			runtimePath);
  }

  private IOverlayVirtualComponent createProjectComponent(IVirtualComponent component, String name) {
    IProject p = null;   
	if("".equals(name)) {
      p = component.getProject();
    } else {
      p = ResourcesPlugin.getWorkspace().getRoot().getProject(name);    	
    }
	if (p == null) {
		throw new IllegalArgumentException(name + " is not a workspace project");
	}
	return new OverlayVirtualComponent(p);
  }

  public boolean canResolve(IVirtualReference reference) {
    return  reference != null && reference.getReferencedComponent() instanceof IOverlayVirtualComponent;
  }

  public ReferencedComponent resolve(IVirtualReference reference) {
    if(canResolve(reference)) {
      IOverlayVirtualComponent comp = (IOverlayVirtualComponent)reference.getReferencedComponent();
      ReferencedComponent rc = ComponentcorePackage.eINSTANCE.getComponentcoreFactory().createReferencedComponent();
      rc.setArchiveName(reference.getArchiveName());
      rc.setRuntimePath(reference.getRuntimePath());
      URI handle;
      Map<String, String> parameters = new LinkedHashMap<String, String>(3);
      if (comp instanceof OverlayVirtualArchiveComponent) {
    	  OverlayVirtualArchiveComponent archivecomp = (OverlayVirtualArchiveComponent) comp;
    	  handle = URI.createURI(VAR_ARCHIVE_PROTOCOL+archivecomp.getArchivePath().toPortableString());
    	  parameters.put(UNPACK_FOLDER, archivecomp.getUnpackFolderPath().toPortableString());
      } else {
    	  IProject p = comp.getProject();
    	  if (p.equals(reference.getEnclosingComponent().getProject())) {
        	  handle = URI.createURI(SELF_PROTOCOL);
    	  } else {
        	  handle = URI.createURI(PROJECT_PROTOCOL+p.getName());
    	  }
      }
      parameters.put(INCLUDES, flatten(comp.getInclusions()));
      parameters.put(EXCLUDES, flatten(comp.getExclusions()));
      handle = URI.createURI(ModuleURIUtil.appendToUri(handle.toString(), parameters));
	  rc.setHandle(handle); 
      rc.setDependencyType(DependencyType.CONSUMES_LITERAL);
      return rc;
    }
    return null;
  }

	private String flatten(Set<String> patterns) {
		StringBuilder sb = new StringBuilder();
		if (patterns != null) {
			boolean initialized = false;
			for(String pattern : patterns) {
				if (initialized) {
					sb.append(";");
				} else {
					initialized = true;
				}
				sb.append(pattern);
			}
		}
		return sb.toString();
	}

}
