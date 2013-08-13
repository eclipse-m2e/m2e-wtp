/*************************************************************************************
 * Copyright (c) 2011 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Red Hat, Inc. - Initial implementation.
 ************************************************************************************/
package org.eclipse.m2e.wtp.overlay.internal.modulecore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.m2e.wtp.overlay.internal.Messages;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.common.componentcore.internal.resources.VirtualFile;
import org.eclipse.wst.common.componentcore.internal.resources.VirtualFolder;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFile;
import org.eclipse.wst.common.componentcore.resources.IVirtualResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Virtual Folder traversing the {@link IResource}s members of a {@link IVirtualComponent} 
 * and mapping them as {@link IVirtualResource}.
 * 
 * @provisional This class has been added as part of a work in progress. 
 * It is not guaranteed to work or remain the same in future releases. 
 * For more information contact <a href="mailto:m2e-wtp-dev@eclipse.org">m2e-wtp-dev@eclipse.org</a>.
 * 
 * @author Fred Bricon
 *
 */
@SuppressWarnings("restriction")
public class ResourceListVirtualFolder extends VirtualFolder implements IFilteredVirtualFolder {

	private static final Logger LOG = LoggerFactory.getLogger(ResourceListVirtualFolder.class);

	private ArrayList<IResource> children;
	private ArrayList<IContainer> underlying;
	private IResourceFilter filter;
	
	public ResourceListVirtualFolder(
			IProject aComponentProject,
			IPath aRuntimePath) {
		super(aComponentProject, aRuntimePath);
		this.children = new ArrayList<IResource>();
		this.underlying = new ArrayList<IContainer>();
	}

	public ResourceListVirtualFolder(
			IProject aComponentProject,
			IPath aRuntimePath, IContainer[] underlyingContainers) {
		this(aComponentProject, aRuntimePath);
		addUnderlyingResource(underlyingContainers);
	}

	public ResourceListVirtualFolder(
			IProject aComponentProject,
			IPath aRuntimePath, IContainer[] underlyingContainers, 
			IResource[] looseResources) {
		this(aComponentProject, aRuntimePath, underlyingContainers);
		addChildren(looseResources);
	}

	public void setFilter(IResourceFilter filter) {
		this.filter = filter;
	}
	
	protected void addUnderlyingResource(IResource resource) {
		if( resource instanceof IContainer ) { 
			underlying.add((IContainer)resource);
			try {
				IResource[] newChildren = ((IContainer)resource).members();
				for( int i = 0; i < newChildren.length; i++ ) {
					children.add(newChildren[i]);
				}
			} catch( CoreException ce) {
				LOG.error(ce.getLocalizedMessage(), ce);
			}
		}
	}

	protected void addUnderlyingResource(IResource[] resources) {
		for( int i = 0; i < resources.length; i++ ) {
			addUnderlyingResource(resources[i]);
		}
	}
	
	protected void addChild(IResource resource) {
		this.children.add(resource);
	}

	protected void addChildren(IResource[] resources) {
		this.children.addAll(Arrays.asList(resources));
	}
	
	@Override
	public IResource getUnderlyingResource() {
		return getUnderlyingFolder();
	}
	
	@Override
	public IResource[] getUnderlyingResources() {
		return getUnderlyingFolders();
	}

	@Override
	public IContainer getUnderlyingFolder() { 
		return underlying.size() > 0 ? underlying.get(0) : null;
	}
	
	@Override
	public IContainer[] getUnderlyingFolders() {
		return (IContainer[]) underlying.toArray(new IContainer[underlying.size()]);
	}

	@Override
	public IVirtualResource[] members(int memberFlags) throws CoreException {
		HashMap<String, IVirtualResource> virtualResources = new HashMap<String, IVirtualResource>(); // result
		IResource[] resources = (IResource[]) this.children.toArray(new IResource[this.children.size()]);
		for( int i = 0; i < resources.length; i++ ) {
			handleResource(resources[i], virtualResources, memberFlags);
		}
		Collection<IVirtualResource> c = virtualResources.values();
		return (IVirtualResource[]) c.toArray(new IVirtualResource[c.size()]);
	}

	protected void handleResource(final IResource resource, HashMap<String, IVirtualResource> map, int memberFlags) throws CoreException {
		if (resource == null) {
			return;
		}
		boolean isFile =  resource instanceof IFile;
		String path = resource.getLocation().toPortableString();
		if( filter != null && !filter.accepts(path, isFile)) {
		  return;
		}
			
		if( isFile) {
			if( !map.containsKey(resource.getName()) ) {
				IVirtualFile virtFile = new VirtualFile(getProject(), 
						getRuntimePath().append(((IFile)resource).getName()), (IFile)resource) {
					
					@Override
					public IPath getWorkspaceRelativePath() {
						IPath wrp = resource.getFullPath(); 
						return wrp;
					}
				};
				map.put(resource.getName(), virtFile);
				return;
			} 
		}// end file
		else if( resource instanceof IContainer ) {
			IContainer realContainer = (IContainer) resource;
			IVirtualResource previousValue = map.get(resource.getName());
			if( previousValue != null && previousValue instanceof ResourceListVirtualFolder ) {
				((ResourceListVirtualFolder)previousValue).addUnderlyingResource(realContainer);
			} else if( previousValue == null ) {
				ResourceListVirtualFolder childFolder = 
					new ResourceListVirtualFolder(getProject(), getRuntimePath().append(resource.getName()));
				childFolder.addUnderlyingResource(realContainer);
				if( filter != null )
					childFolder.setFilter(filter);
				map.put(resource.getName(), childFolder);
			}
		} // end container
	}

	@Override
	public IResourceFilter getFilter() {
		return filter;
	}
	
	@Override
	public IVirtualResource findMember(IPath path, int searchFlags) {
		if (underlying == null || path == null) {
			return null;
		}
		if (path.isAbsolute()) {
			path = path.makeRelative();
		}
		for(IResource resource : underlying) {
			if (resource instanceof IContainer) {
				IContainer c = (IContainer) resource;
				IResource candidate = c.findMember(path, true);
				if (candidate != null && candidate.exists()) {
					HashMap<String, IVirtualResource> map = new HashMap<String, IVirtualResource>(1);
					try {
						handleResource(candidate, map, 0);
						if (!map.isEmpty()) {
							IVirtualResource vr = map.values().iterator().next();
							return vr;
						}
					} catch (CoreException e) {
						String message = NLS.bind(Messages.ResourceListVirtualFolder_Error_Finding_Member, path, candidate);
						LOG.error(message, e);
					}
				}
			}
		}
    	return null;
	}

}
