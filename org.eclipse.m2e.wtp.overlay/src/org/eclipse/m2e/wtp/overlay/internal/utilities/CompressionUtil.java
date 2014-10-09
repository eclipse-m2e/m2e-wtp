/*******************************************************************************
 * Copyright (c) 2006, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat, Inc. - extracted unzip method out of the original code
 *******************************************************************************/
package org.eclipse.m2e.wtp.overlay.internal.utilities;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.tools.ant.util.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.m2e.wtp.overlay.internal.Messages;
import org.eclipse.osgi.util.NLS;

/**
 * Compression utility class.
 * 
 * Most of the code is copied from <i>org.eclipse.gef.examples.ui.pde.internal.wizards.ProjectUnzipperNewWizard</i>
 *  
 * @author Fred Bricon
 *
 */
public class CompressionUtil {
	
	private final static int BUFFER = 1024*4;
	
	private CompressionUtil() {}

	/**
	 * Unzips the platform formatted zip file to specified folder
	 * 
	 * @param zipFile
	 *            The platform formatted zip file
	 * @param projectFolderFile
	 *            The folder where to unzip the archive
	 * @param monitor
	 *            Monitor to display progress and/or cancel operation
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws InterruptedException
	 */
	public static void unzip(File archive, File projectFolderFile,
			IProgressMonitor monitor) throws IOException,
			FileNotFoundException, InterruptedException {

		initialize(projectFolderFile);
		
		ZipFile zipFile = new ZipFile(archive);
		Enumeration<? extends ZipEntry> e = zipFile.entries();

		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		
		monitor.beginTask(Messages.CompressionUtil_Extracting_Task, zipFile.size());
		try {
			while (e.hasMoreElements()) {
				ZipEntry zipEntry = e.nextElement();
				File file = new File(projectFolderFile, zipEntry.getName());
				
				if (!zipEntry.isDirectory()) {
					monitor.subTask(zipEntry.getName());
								
					File parentFile = file.getParentFile();
					if (null != parentFile && !parentFile.exists()) {
						parentFile.mkdirs();
					}
					
					InputStream is = null;
					OutputStream os = null;
					
					try {
						is = zipFile.getInputStream(zipEntry);
						os = new FileOutputStream(file);
						
						byte[] buffer = new byte[BUFFER];
						while (true) {
							int len = is.read(buffer);
							if (len < 0)
								break;
							os.write(buffer, 0, len);
						}
					} finally {
						FileUtils.close(is);
						FileUtils.close(os);
					}
				}
				
				monitor.worked(1);
				
				if (monitor.isCanceled()) {
					throw new InterruptedException(NLS.bind(Messages.CompressionUtil_Unzipping_Interrupted, archive.getAbsolutePath(), projectFolderFile.getAbsolutePath()));
				}
			}
		} finally {
			if (zipFile != null) {
				try {
					zipFile.close();
				} catch (IOException ioe) {
					//ignore
				}
			}
		}
	}

	private static void initialize(File outputDirectory) throws IOException {
      // Create output directory if needed
      if (!outputDirectory.mkdirs() && !outputDirectory.exists())
      {
         throw new IOException(Messages.CompressionUtil_Unable_To_Create_Output_Dir + outputDirectory);
      }
      if (outputDirectory.isFile())
      {
         throw new IllegalArgumentException(NLS.bind(Messages.CompressionUtil_Unpacking_Unable,outputDirectory.getAbsolutePath()));
      }
	}
	
}
