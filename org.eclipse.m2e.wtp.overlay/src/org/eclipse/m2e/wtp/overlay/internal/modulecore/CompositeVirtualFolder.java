/*******************************************************************************
 * Copyright (c) 2011-2015 Sonatype, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.m2e.wtp.overlay.internal.modulecore;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Queue;
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
import org.eclipse.m2e.wtp.overlay.internal.Messages;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.internal.flat.FlatVirtualComponent;
import org.eclipse.wst.common.componentcore.internal.flat.IChildModuleReference;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private static final Logger LOG = LoggerFactory.getLogger(CompositeVirtualFolder.class);

	private FlatVirtualComponent flatVirtualComponent;
	private IPath runtimePath;
	private IProject project;
	private Set<IVirtualReference> references = new LinkedHashSet<>();
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
			LOG.error(Messages.CompositeVirtualFolder_Error_Scanning, e);
		}
	}

	@Override
	public IProject getProject() {
		return project;
	}

	@Override
	public IPath getRuntimePath() {
		return runtimePath;
	}

	@Override
	public IVirtualResource[] members() throws CoreException {
		if (members == null) {
			members = new IVirtualResource[0]; 
		}
		return members;
	}
	
	public void treeWalk() throws CoreException {	 
		IFlatResource[] flatResources = flatVirtualComponent.fetchResources();
		
		List<IVirtualResource> membersList = new ArrayList<>(flatResources.length);
		for (IFlatResource flatResource : flatResources) {
			IVirtualResource resource = convert(flatResource);
			if (resource != null) {
				membersList.add(resource);	
			}
		}
		for (IChildModuleReference childModule : flatVirtualComponent.getChildModules()) {
			IVirtualReference reference = childModule.getReference();
			if (reference != null) {
				String filePath = getFilePath(reference);
				if (filter == null || filter.accepts(filePath, true)) {
					references.add(reference);
				}
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
		List<IVirtualResource> membersList = new ArrayList<>(flatMembers.length);
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
		final IFile f = (IFile)flatFile.getAdapter(IFile.class);
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
			IPath ffRuntimePath = flatFile.getModuleRelativePath();
			filePath = ffRuntimePath.toPortableString() + Path.SEPARATOR + fileName;
			if (filter == null || filter.accepts(filePath, true)) {
				return new VirtualFile(project, ffRuntimePath, f) {
					@Override
					public String getName() {
						return fileName;
					}
					
					@Override
					public IPath getWorkspaceRelativePath() {
						return f.getFullPath();
					}
					
					@Override
					public IFile getUnderlyingFile() {
						IFile f = super.getUnderlyingFile();
						return f;
					}
				};
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

	@Override
	public void create(int arg0, IProgressMonitor arg1) throws CoreException {
		// ignore
	}

	@Override
	public boolean exists(IPath arg0) {
		return false;
	}
	
    @Override
	public IVirtualResource findMember(String sPath) {
        return findMember(new Path(sPath), 0);
    }

    @Override
	public IVirtualResource findMember(String sPath, int searchFlags) {
        return findMember(new Path(sPath), searchFlags);
    }

    @Override
	public IVirtualResource findMember(IPath path) {
        return findMember(path, 0);
    }

    @Override
	public IVirtualResource findMember(IPath path, int theSearchFlags) {
    	if (path == null) {
    		return null;
    	}
    	Queue<String> segments = new ArrayDeque<String>(path.segmentCount());
    	for (String s : path.segments()) {
    		segments.add(s);
    	}
    	try {
    		return findMember(segments, members);
    	} catch (CoreException ce) {
    		LOG.error(Messages.CompositeVirtualFolder_Error_Finding_Member, ce);
    	}
    	return null;
    }
    
    private static IVirtualResource findMember(Queue<String> segments, IVirtualResource[] members) throws CoreException {
    	if (segments.isEmpty()) {
    		return null;
    	}
    	String segment = segments.poll();
    	boolean finalResource = segments.isEmpty();
    	for (IVirtualResource m : members) {
    		if (m.getName().equals(segment)) {
    			if (finalResource) {
    				return m;
    			}
    			if (m instanceof IVirtualFolder) {
    				return findMember(segments, ((IVirtualFolder)m).members());
    			}
    		}
    	}
        return null;
    }
    
    
    
	@Override
	public IVirtualFile getFile(IPath arg0) {
		// ignore
		return null;
	}

	@Override
	public IVirtualFile getFile(String arg0) {
		// ignore
		return null;
	}

	@Override
	public IVirtualFolder getFolder(IPath arg0) {
		// ignore
		return null;
	}

	@Override
	public IVirtualFolder getFolder(String arg0) {
		// ignore
		return null;
	}

	@Override
	public IVirtualResource[] getResources(String arg0) {
		// ignore
		return null;
	}

	@Override
	public IVirtualResource[] members(int arg0) throws CoreException {
		// ignore
		return null;
	}

	@Override
	public void createLink(IPath arg0, int arg1, IProgressMonitor arg2)
			throws CoreException {
		// ignore
	}

	@Override
	public void delete(int arg0, IProgressMonitor arg1) throws CoreException {
		// ignore		
	}

	@Override
	public boolean exists() {
		// ignore
		return false;
	}

	@Override
	public IVirtualComponent getComponent() {
		// ignore
		return null;
	}

	@Override
	public String getFileExtension() {
		// ignore
		return null;
	}

	@Override
	public String getName() {
		// ignore
		return null;
	}

	@Override
	public IVirtualContainer getParent() {
		// ignore
		return null;
	}

	@Override
	public IPath getProjectRelativePath() {
		// ignore
		return null;
	}

	@Override
	public String getResourceType() {
		// ignore
		return null;
	}

	@Override
	public int getType() {
		// ignore
		return 0;
	}

	@Override
	public IResource getUnderlyingResource() {
		// ignore
		return null;
	}

	@Override
	public IResource[] getUnderlyingResources() {
		// ignore
		return null;
	}

	@Override
	public IPath getWorkspaceRelativePath() {
		// ignore
		return null;
	}

	@Override
	public boolean isAccessible() {
		// ignore
		return false;
	}

	@Override
	public void removeLink(IPath arg0, int arg1, IProgressMonitor arg2)
			throws CoreException {
		// ignore
		
	}

	@Override
	public void setResourceType(String arg0) {
		// ignore
		
	}

	@Override
	public boolean contains(ISchedulingRule rule) {
		// ignore
		return false;
	}

	@Override
	public boolean isConflicting(ISchedulingRule rule) {
		// ignore
		return false;
	}

	@Override
	public Object getAdapter(Class adapter) {
		// ignore
		return null;
	}

	@Override
	public IContainer getUnderlyingFolder() {
		// ignore
		return null;
	}

	@Override
	public IContainer[] getUnderlyingFolders() {
		// ignore
		return null;
	}

	public IVirtualReference[] getReferences() {
		return references.toArray(new IVirtualReference[references.size()]);
	}
	
	@Override
	public IResourceFilter getFilter() {
		return filter;
	}

	@Override
	public void setFilter(IResourceFilter filter) {
		this.filter = filter;
	}
	
	
	private String getFilePath(IVirtualReference reference) {
		StringBuilder path = new StringBuilder();
		String prefix = reference.getRuntimePath().makeRelative().toString();
		path.append(prefix);
		if (prefix.length() > 0 && prefix.charAt(prefix.length() - 1)!= '/') {
			path.append("/"); //$NON-NLS-1$
		}
		String archiveName = reference.getArchiveName();
		if (archiveName == null || archiveName.isEmpty()) {
			archiveName = reference.getReferencedComponent().getDeployedName() + ".jar"; //$NON-NLS-1$
		}
		path.append(archiveName);
		return path.toString();
	}
}
