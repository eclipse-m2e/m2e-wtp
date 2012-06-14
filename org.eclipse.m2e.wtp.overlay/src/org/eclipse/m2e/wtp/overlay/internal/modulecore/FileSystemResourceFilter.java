/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.m2e.wtp.overlay.internal.modulecore;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.tools.ant.DirectoryScanner;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.m2e.wtp.overlay.internal.utilities.PathUtil;

public class FileSystemResourceFilter implements IResourceFilter {

	private SimpleScanner scanner;

	public FileSystemResourceFilter(Collection<String> inclusions, Collection<String> exclusions, IPath baseDirPath) {
		scanner = new SimpleScanner(baseDirPath);
		if (inclusions != null && !inclusions.isEmpty()) {
			scanner.setIncludes(inclusions.toArray(new String[inclusions.size()]));
		} else {
			scanner.setIncludes(new String[]{"**/**"});
		}
		if (exclusions != null && !exclusions.isEmpty()) {
			scanner.addExcludes(exclusions.toArray(new String[exclusions.size()]));
		}
		scanner.addDefaultExcludes();
		scanner.scan();
	}

	public boolean accepts(String resourcePath, boolean isFile) {
		return scanner.accepts(resourcePath, isFile);
	}
	
	class SimpleScanner extends DirectoryScanner {

		@Override
		public synchronized void setIncludes(String[] includes) {
			super.setIncludes(setFileSeparator(includes));
		}
		
		@Override
		public synchronized void setExcludes(String[] excludes) {
			super.setExcludes(setFileSeparator(excludes));
		}
		
		private String[] setFileSeparator(String[] patterns) {
			if (patterns != null) {
				for (int i = 0; i < patterns.length ; i++) {
					patterns[i] = PathUtil.useSystemSeparator(patterns[i]);
				}
			}
			return patterns;
		}

		private String baseDirAsString;
		private Set<String> includedFiles;
		private Set<String> excludedFiles;
		private Set<String> includedFolders;
		private Set<String> excludedFolders;
		
		public SimpleScanner(IPath baseDirPath) {
			this.baseDirAsString = baseDirPath.toOSString();
			setBasedir(baseDirAsString);
		}

		@Override
		public void scan() throws IllegalStateException {
			super.scan();
			//cache the included and excluded files (to avoid several array copies)
			includedFiles = new HashSet<String>(Arrays.asList(getIncludedFiles()));
			excludedFiles = new HashSet<String>(Arrays.asList(getExcludedFiles()));
			includedFolders =  new HashSet<String>(Arrays.asList(getIncludedDirectories()));
			excludedFolders =  new HashSet<String>(Arrays.asList(getExcludedDirectories()));
			
			completeIncludedFolders();
			//System.out.println(baseDirPath +" includes "+includedFiles.size() +" files");
		}
		
		private void completeIncludedFolders() {
			Set<String> missingParentFolders = new HashSet<String>();
			for(String folder : includedFolders) {
			  IPath filePath = new Path(folder);
			  IPath parentPath = filePath.removeLastSegments(1);
			  while (parentPath.segmentCount()>0) {
	    		String pathAsString = parentPath.toOSString(); 
	    		if (!includedFolders.contains(pathAsString)) {
	    		  missingParentFolders.add(pathAsString);
	    		}
	    		parentPath = parentPath.removeLastSegments(1);
	    	  }
			}
    		includedFolders.addAll(missingParentFolders);
    		
	    	for(String file : includedFiles) {
	    		//For /some/foo/bar/file.ext, we need to add 
	    		// /some/foo/bar/
	    		// /some/foo/
	    		// /some/
	    		// as included folders
	    		
	    		IPath filePath = new Path(file);
	    		IPath parentPath = filePath.removeLastSegments(1);
	    		while (parentPath.segmentCount()>0) {
	    			if (includedFolders.add(parentPath.toOSString())) {
	    				parentPath = parentPath.removeLastSegments(1);
	    			} else {
	    				//Parent hierarchy already added
	    				break;
	    			}
	    		}
	    	}
		}
		
		protected boolean accepts(String name, boolean isFile) {
			
			name = PathUtil.useSystemSeparator(name);
			if (name.startsWith(baseDirAsString)) {
				name = name.substring(baseDirAsString.length()+1);
			}
			
			boolean res;
			if (isFile) {
				res = includedFiles.contains(name) && !excludedFiles.contains(name);
			} else {
				res = includedFolders.contains(name) && !excludedFolders.contains(name);
			}
			//System.err.println(name + (res?" included": " excluded"));
			return res;
		}
	}
}
