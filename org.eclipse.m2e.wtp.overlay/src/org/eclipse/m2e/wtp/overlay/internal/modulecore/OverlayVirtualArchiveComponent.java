/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp.overlay.internal.modulecore;

import java.io.File;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.m2e.wtp.overlay.internal.Messages;
import org.eclipse.m2e.wtp.overlay.modulecore.IOverlayVirtualComponent;
import org.eclipse.m2e.wtp.overlay.modulecore.UnpackArchiveJob;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.internal.resources.VirtualArchiveComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;

/**
 * Archive-based Overlay Virtual Component. 
 * 
 * @provisional This class has been added as part of a work in progress. 
 * It is not guaranteed to work or remain the same in future releases. 
 * For more information contact <a href="mailto:m2e-wtp-dev@eclipse.org">m2e-wtp-dev@eclipse.org</a>.
 * 
 * @author Fred Bricon
 */
@SuppressWarnings("restriction")
public class OverlayVirtualArchiveComponent extends VirtualArchiveComponent implements IOverlayVirtualComponent {

	protected IPath unpackDirPath;
	
	private Set<String> inclusions;
	
	private Set<String> exclusions;

	private ResourceListVirtualFolder cachedRoot;

	private long lastCacheUpdate;
	
	private static int MAX_CACHE = 2000;
	
	public OverlayVirtualArchiveComponent(IProject aComponentProject,
			String archiveLocation, IPath unpackDirPath, IPath aRuntimePath) {
		super(aComponentProject, archiveLocation, aRuntimePath);
		this.unpackDirPath = unpackDirPath;
	}

	@Override
	public void setInclusions(Set<String> inclusionPatterns) {
		this.inclusions = inclusionPatterns;
	}

	@Override
	public void setExclusions(Set<String> exclusionPatterns) {
		this.exclusions = exclusionPatterns;
	}
	
	@Override
	public IVirtualFolder getRootFolder() {
		IVirtualComponent component = ComponentCore.createComponent(getProject());
		File archive = getArchive();
		ResourceListVirtualFolder root =null;
		if (component != null && archive != null) {
			IFolder unpackedFolder = getUnpackedArchiveFolder(archive);
			if (isUnpackNeeded(archive, unpackedFolder)) {
			  Job[] currentJobs = Job.getJobManager().find(unpackedFolder);
			  if (currentJobs.length == 0) {
				  Job job = new UnpackArchiveJob(NLS.bind(Messages.OverlayVirtualArchiveComponent_Unpacking_Job,archive.getName()), archive, unpackedFolder);
				  job.schedule();
			  }
			  root = new ResourceListVirtualFolder(getProject(), getRuntimePath(), new IContainer[] {}); 	
			} else {
			  	
			  if (cachedRoot != null && (System.currentTimeMillis() - lastCacheUpdate) < MAX_CACHE){
					return cachedRoot;
			  } 
				
			  IContainer[] containers = new IContainer[] {unpackedFolder};
			  root = new ResourceListVirtualFolder(getProject(), getRuntimePath(), containers);
			  root.setFilter(new FileSystemResourceFilter(inclusions, exclusions, unpackedFolder.getLocation()));
			  
			  lastCacheUpdate = System.currentTimeMillis();
			  cachedRoot = root;
			}
		}
		return root;
	}

	protected IFolder getUnpackedArchiveFolder(File archive) {
		IFolder overlaysFolder =  getProject().getFolder(unpackDirPath);
		return overlaysFolder.getFolder(archive.getName());
	}

	private File getArchive() {
		File archive = (File)getAdapter(File.class);
		if (archive == null || !archive.exists() || !archive.canRead()) {
			return null;
			//TODO should raise an exception // throw new IOException("Unable to read "+ getArchivePath());
		}
		return archive;
	}
	
	private boolean isUnpackNeeded(File archive, IFolder unpackFolder) {
		if (!unpackFolder.exists()) {
		  return true;
		}
		long lastUnpacked = new File(unpackFolder.getLocation().toOSString()).lastModified();
		long lastModified = archive.lastModified();
		return lastModified > lastUnpacked;
	}

	public IPath getUnpackFolderPath() {
		return unpackDirPath;
	}

	@Override
	public Set<String> getExclusions() {
		return exclusions;
	}

	@Override
	public Set<String> getInclusions() {
		return inclusions;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((exclusions == null) ? 0 : exclusions.hashCode());
		result = prime * result
				+ ((inclusions == null) ? 0 : inclusions.hashCode());
		result = prime * result
				+ ((unpackDirPath == null) ? 0 : unpackDirPath.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		OverlayVirtualArchiveComponent other = (OverlayVirtualArchiveComponent) obj;
		if (!super.equals(obj)) {
			return false;
		}
		if (exclusions == null) {
			if (other.exclusions != null)
				return false;
		} else if (!exclusions.equals(other.exclusions))
			return false;
		if (inclusions == null) {
			if (other.inclusions != null)
				return false;
		} else if (!inclusions.equals(other.inclusions))
			return false;
		if (unpackDirPath == null) {
			if (other.unpackDirPath != null)
				return false;
		} else if (!unpackDirPath.equals(other.unpackDirPath))
			return false;
		return true;
	}
	
}
