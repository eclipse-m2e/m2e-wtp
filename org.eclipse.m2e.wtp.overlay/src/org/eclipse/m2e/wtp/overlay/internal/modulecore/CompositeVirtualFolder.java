/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.m2e.wtp.overlay.internal.modulecore;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.internal.flat.FlatVirtualComponent;
import org.eclipse.wst.common.componentcore.internal.flat.IFlatFile;
import org.eclipse.wst.common.componentcore.internal.flat.IFlatFolder;
import org.eclipse.wst.common.componentcore.internal.flat.IFlatResource;
import org.eclipse.wst.common.componentcore.internal.resources.VirtualArchiveComponent;
import org.eclipse.wst.common.componentcore.internal.resources.VirtualFile;
import org.eclipse.wst.common.componentcore.internal.resources.VirtualFolder;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualContainer;
import org.eclipse.wst.common.componentcore.resources.IVirtualFile;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.eclipse.wst.common.componentcore.resources.IVirtualReference;
import org.eclipse.wst.common.componentcore.resources.IVirtualResource;

/**
 * Virtual folder mapping a FlatVirtualComponent
 * 
 * @provisional This class has been added as part of a work in progress. 
 * It is not guaranteed to work or remain the same in future releases. 
 * For more information contact <a href="mailto:m2e-wtp-dev@eclipse.org">m2e-wtp-dev@eclipse.org</a>.
 * 
 * @author Fred Bricon
 * 
 */
@SuppressWarnings("restriction")
public class CompositeVirtualFolder implements IFilteredVirtualFolder {

	private FlatVirtualComponent flatVirtualComponent;
	private IPath runtimePath;
	private IProject project;
	private Set<IVirtualReference> references = new LinkedHashSet<IVirtualReference>();
	private IVirtualResource[] members;
	private IResourceFilter filter;
	
	public CompositeVirtualFolder(FlatVirtualComponent aFlatVirtualComponent, IPath aRuntimePath, IResourceFilter filter) {
		this.flatVirtualComponent = aFlatVirtualComponent;
		if (flatVirtualComponent != null && flatVirtualComponent.getComponent() != null) {
			project = flatVirtualComponent.getComponent().getProject();
		}
		this.runtimePath = aRuntimePath;
		this.filter = filter;
		try {
			treeWalk();
		} catch (CoreException e) {
			//TODO handle exception
			e.printStackTrace();
		}
	}

	public IProject getProject() {
		return project;
	}

	public IPath getRuntimePath() {
		return runtimePath;
	}

	public IVirtualResource[] members() throws CoreException {
		if (members == null) {
			members = new IVirtualResource[0]; 
		}
		return members;
	}
	
	public void treeWalk() throws CoreException {	 
		IFlatResource[] flatResources = flatVirtualComponent.fetchResources();
		List<IVirtualResource> membersList = new ArrayList<IVirtualResource>(flatResources.length);
		for (IFlatResource flatResource : flatResources) {
			IVirtualResource resource = convert(flatResource);
			if (resource != null) {
				membersList.add(resource);	
			}
		}
		members = new IVirtualResource[membersList.size()];
		membersList.toArray(members);
	}

	private IVirtualResource convert(IFlatResource flatResource) {
		IVirtualResource virtualResource = null;
		if (flatResource instanceof IFlatFolder) {
			virtualResource = convertFolder((IFlatFolder) flatResource);
		} else if (flatResource instanceof IFlatFile){
			virtualResource = convertFile((IFlatFile) flatResource);
		}
		
		return virtualResource;
	}

	private IVirtualFolder convertFolder(IFlatFolder flatFolder) {
		IFlatResource[] flatMembers = flatFolder.members();
		List<IVirtualResource> membersList = new ArrayList<IVirtualResource>(flatMembers.length);
		for (IFlatResource flatResource : flatMembers) {
			IVirtualResource resource = convert(flatResource);
			if (resource != null) {
				membersList.add(resource);	
			}
		}
		final IVirtualResource[] folderMembers = new IVirtualResource[membersList.size()];
		membersList.toArray(folderMembers);
		VirtualFolder vf = new VirtualFolder(project, flatFolder.getModuleRelativePath().append(flatFolder.getName())) {
			@Override
			public IVirtualResource[] members() throws CoreException {
				return folderMembers; 
			}
		}; 
		return vf;
		
	}

	private IVirtualFile convertFile(IFlatFile flatFile) {
		IFile f = (IFile)flatFile.getAdapter(IFile.class);
		VirtualFile vf = null;
		String filePath  = null;
		if (f == null) {
			//Not a workspace file, we assume it's an external reference
			File underlyingFile = (File)flatFile.getAdapter(File.class);
			if (underlyingFile != null && underlyingFile.exists()) {
				filePath = flatFile.getModuleRelativePath().toPortableString() + Path.SEPARATOR + underlyingFile.getName();
				if (filter == null || filter.accepts(filePath, true)) {
					IVirtualReference reference = createReference(underlyingFile, flatFile.getModuleRelativePath());
					references.add(reference);
				}
			}
		} else {
			final String fileName = f.getName(); 
			vf = new VirtualFile(project, flatFile.getModuleRelativePath(), f) {
				@Override
				public String getName() {
					return fileName;
				}
			};
			
			filePath = vf.getRuntimePath().toPortableString() + Path.SEPARATOR + fileName;
			if (filter == null || filter.accepts(filePath, true)) {
				return vf;
			}
			
		}
		return null;
	}
	
	private IVirtualReference createReference(File underlyingFile, IPath path) {
		VirtualArchiveComponent archive = new VirtualArchiveComponent(project, VirtualArchiveComponent.LIBARCHIVETYPE + Path.SEPARATOR + underlyingFile.getAbsolutePath(), path);
		IVirtualReference ref = ComponentCore.createReference(flatVirtualComponent.getComponent(), archive);
		ref.setArchiveName(archive.getArchivePath().lastSegment());
		ref.setRuntimePath(path);
		return ref;
	}

	public void create(int arg0, IProgressMonitor arg1) throws CoreException {
		// ignore
	}

	public boolean exists(IPath arg0) {
		// ignore
		return false;
	}

	public IVirtualResource findMember(String arg0) {
		// ignore
		return null;
	}

	public IVirtualResource findMember(IPath arg0) {
		// ignore
		return null;
	}

	public IVirtualResource findMember(String arg0, int arg1) {
		// ignore
		return null;
	}

	public IVirtualResource findMember(IPath arg0, int arg1) {
		// ignore
		return null;
	}

	public IVirtualFile getFile(IPath arg0) {
		// ignore
		return null;
	}

	public IVirtualFile getFile(String arg0) {
		// ignore
		return null;
	}

	public IVirtualFolder getFolder(IPath arg0) {
		// ignore
		return null;
	}

	public IVirtualFolder getFolder(String arg0) {
		// ignore
		return null;
	}

	public IVirtualResource[] getResources(String arg0) {
		// ignore
		return null;
	}

	public IVirtualResource[] members(int arg0) throws CoreException {
		// ignore
		return null;
	}

	public void createLink(IPath arg0, int arg1, IProgressMonitor arg2)
			throws CoreException {
		// ignore
	}

	public void delete(int arg0, IProgressMonitor arg1) throws CoreException {
		// ignore		
	}

	public boolean exists() {
		// ignore
		return false;
	}

	public IVirtualComponent getComponent() {
		// ignore
		return null;
	}

	public String getFileExtension() {
		// ignore
		return null;
	}

	public String getName() {
		// ignore
		return null;
	}

	public IVirtualContainer getParent() {
		// ignore
		return null;
	}

	public IPath getProjectRelativePath() {
		// ignore
		return null;
	}

	public String getResourceType() {
		// ignore
		return null;
	}

	public int getType() {
		// ignore
		return 0;
	}

	public IResource getUnderlyingResource() {
		// ignore
		return null;
	}

	public IResource[] getUnderlyingResources() {
		// ignore
		return null;
	}

	public IPath getWorkspaceRelativePath() {
		// ignore
		return null;
	}

	public boolean isAccessible() {
		// ignore
		return false;
	}

	public void removeLink(IPath arg0, int arg1, IProgressMonitor arg2)
			throws CoreException {
		// ignore
		
	}

	public void setResourceType(String arg0) {
		// ignore
		
	}

	public boolean contains(ISchedulingRule rule) {
		// ignore
		return false;
	}

	public boolean isConflicting(ISchedulingRule rule) {
		// ignore
		return false;
	}

	public Object getAdapter(Class adapter) {
		// ignore
		return null;
	}

	public IContainer getUnderlyingFolder() {
		// ignore
		return null;
	}

	public IContainer[] getUnderlyingFolders() {
		// ignore
		return null;
	}

	public IVirtualReference[] getReferences() {
		return references.toArray(new IVirtualReference[references.size()]);
	}
	
	public IResourceFilter getFilter() {
		return filter;
	}

	public void setFilter(IResourceFilter filter) {
		this.filter = filter;
	}
	
}
