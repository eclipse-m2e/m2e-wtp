/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.m2e.wtp.overlay.modulecore;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.m2e.wtp.overlay.internal.Messages;
import org.eclipse.m2e.wtp.overlay.internal.OverlayPluginActivator;
import org.eclipse.m2e.wtp.overlay.internal.utilities.CompressionUtil;
import org.eclipse.osgi.util.NLS;

/**
 * Job unpacking a {@link File} to a destination {@link IFolder}.
 * 
 * @provisional This class has been added as part of a work in progress. 
 * It is not guaranteed to work or remain the same in future releases. 
 * For more information contact <a href="mailto:m2e-wtp-dev@eclipse.org">m2e-wtp-dev@eclipse.org</a>.
 * 
 * @author Fred Bricon
 *
 */
public class UnpackArchiveJob extends WorkspaceJob {

	private IFolder unpackFolder;
	private File archive;

	public UnpackArchiveJob(String name, File archive, IFolder unpackFolder) {
		super(name);
		assert unpackFolder != null;
		assert archive != null && archive.exists() && archive.canRead();
		this.unpackFolder = unpackFolder;
		this.archive = archive;
		setRule(unpackFolder);
	}
	
	@Override
	public boolean belongsTo(Object family) {
		return unpackFolder.equals(family);
	}

	@Override
	public IStatus runInWorkspace(IProgressMonitor monitor)
			throws CoreException {
		try {
			if (unpackFolder.exists()) {
	      		//delete members as deleting unpackFolder will use scheduling rule of its parent, so an IllegalArgumentException would be thrown otherwise
				final IResource[] members = unpackFolder.members(IContainer.INCLUDE_HIDDEN | IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS);
				for (final IResource member : members)
				{
					if (monitor.isCanceled()) {
						return new Status(IStatus.ERROR, OverlayPluginActivator.PLUGIN_ID, NLS.bind(Messages.UnpackArchiveJob_Deleteing_was_cancelled, member.getName()));
					}
					member.delete(true, monitor);
				}
			}
			unpack(archive, unpackFolder.getLocation().toOSString(), monitor);
		} catch (IOException e) {
			return new Status(IStatus.ERROR, OverlayPluginActivator.PLUGIN_ID, NLS.bind(Messages.UnpackArchiveJob_Error_Unpacking, archive.getName()), e);
		} catch (InterruptedException e) {
			return new Status(IStatus.ERROR, OverlayPluginActivator.PLUGIN_ID, NLS.bind(Messages.UnpackArchiveJob_Unpacking_Interrupted, archive.getName()) , e);
		}
		
		//will run in scheduling rule of parent of unpackfolder, so should be run in a different job
		new WorkspaceJob(NLS.bind(Messages.UnpackArchiveJob_Refreshing, unpackFolder.getLocation().toString())) {

			@Override
			public IStatus runInWorkspace(final IProgressMonitor monitor) throws CoreException
			{
				if (!monitor.isCanceled()) {
					unpackFolder.refreshLocal(IFolder.DEPTH_INFINITE, monitor);
				}
				return Status.OK_STATUS;
			}
		}.schedule();

		return Status.OK_STATUS;
	}

	protected void unpack(File archive, String unpackFolderPath, IProgressMonitor monitor) throws IOException, CoreException,
			InterruptedException {
		File unpackFolder = new File(unpackFolderPath);
		CompressionUtil.unzip(archive, unpackFolder, monitor);
		unpackFolder.setLastModified(archive.lastModified());
	}
}
