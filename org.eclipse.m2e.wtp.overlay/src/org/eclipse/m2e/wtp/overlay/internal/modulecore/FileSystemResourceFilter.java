/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.m2e.wtp.overlay.internal.modulecore;

import java.io.Externalizable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.tools.ant.DirectoryScanner;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.m2e.wtp.overlay.internal.OverlayPluginActivator;
import org.eclipse.m2e.wtp.overlay.internal.utilities.PathUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filter for FileSystem resources.
 * 
 * @provisional This class has been added as part of a work in progress. It is
 *              not guaranteed to work or remain the same in future releases.
 *              For more information contact <a
 *              href="mailto:m2e-wtp-dev@eclipse.org"
 *              >m2e-wtp-dev@eclipse.org</a>.
 * 
 * @author Fred Bricon
 * 
 */
public class FileSystemResourceFilter implements IResourceFilter {

	private static final Logger LOG = LoggerFactory.getLogger(FileSystemResourceFilter.class);
	
	private SimpleScanner scanner;

	public FileSystemResourceFilter(Collection<String> inclusions,
			Collection<String> exclusions, IPath baseDirPath) {
		scanner = new SimpleScanner(baseDirPath);
		if (inclusions != null && !inclusions.isEmpty()) {
			scanner.setIncludes(inclusions.toArray(new String[inclusions.size()]));
		} else {
			scanner.setIncludes(new String[] { "**/**" }); //$NON-NLS-1$
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

	static class SimpleScanner extends DirectoryScanner {

		private ScanResult scanResult;

		private String[] includePatterns;

		private String[] excludePatterns;

		@Override
		public synchronized void setIncludes(String[] includes) {
			includePatterns = setFileSeparator(includes);
			super.setIncludes(includePatterns);
		}

		public String[] getIncludePatterns() {
			return includePatterns;
		}

		public String[] getExcludePatterns() {
			return excludePatterns;
		}

		public boolean accepts(String resourcePath, boolean isFile) {
			if (scanResult != null) {
				return scanResult.accepts(resourcePath, isFile);
			}
			return false;
		}

		@Override
		public synchronized void setExcludes(String[] excludes) {
			excludePatterns = setFileSeparator(excludes);
			super.setExcludes(excludePatterns);
		}

		private String[] setFileSeparator(String[] patterns) {
			if (patterns != null) {
				for (int i = 0; i < patterns.length; i++) {
					patterns[i] = PathUtil.useSystemSeparator(patterns[i]);
				}
			}
			return patterns;
		}

		private String baseDirAsString;

		private long folderTimestamp;

		public SimpleScanner(IPath baseDirPath) {
			this.baseDirAsString = baseDirPath.toOSString();
			setBasedir(baseDirAsString);
			// If folder timestamp changed, gen'd scanId will change (different
			// hashcode)
			folderTimestamp = basedir.lastModified();
		}

		@Override
		public void scan() throws IllegalStateException {
			String scanId = Integer.toString(hashCode());
			scanResult = ScanResult.read(scanId);
			if (scanResult == null) {
				super.scan();
				scanResult = new ScanResult(scanId, baseDirAsString,
						getIncludedFiles(), getExcludedFiles(),
						getIncludedDirectories(), getExcludedDirectories());
				ScanResult.write(scanResult);
			}

		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime
					* result
					+ ((baseDirAsString == null) ? 0 : baseDirAsString
							.hashCode());
			result = prime * result + Arrays.hashCode(excludePatterns);
			result = prime * result + Arrays.hashCode(includePatterns);
			result = prime * result
					+ (int) (folderTimestamp ^ (folderTimestamp >>> 32));
			return result;
		}

	}

	static class ScanResult implements Externalizable {

		private static final String COLLECTION_SEPARATOR = ";"; //$NON-NLS-1$

		private static final long serialVersionUID = 1L;

		private String baseDirAsString;

		private String scanId;

		private Set<String> includedFiles;
		private Set<String> excludedFiles;
		private Set<String> includedFolders;
		private Set<String> excludedFolders;

		public ScanResult() {
			includedFiles = new HashSet<String>(0);
			excludedFiles = new HashSet<String>(0);
			includedFolders = new HashSet<String>(0);
			excludedFolders = new HashSet<String>(0);
		}

		public ScanResult(String scanId, String baseDirAsString,
				String[] incFiles, String[] excFiles, String[] incDirs,
				String[] excDirs) {
			this.scanId = scanId;
			this.baseDirAsString = baseDirAsString;
			includedFiles = new HashSet<String>(Arrays.asList(incFiles));
			excludedFiles = new HashSet<String>(Arrays.asList(excFiles));
			includedFolders = new HashSet<String>(Arrays.asList(incDirs));
			excludedFolders = new HashSet<String>(Arrays.asList(excDirs));
			completeIncludedFolders();
		}

		public String getBaseDirAsString() {
			return baseDirAsString;
		}

		public void setBaseDirAsString(String baseDirAsString) {
			this.baseDirAsString = baseDirAsString;
		}

		public Set<String> getIncludedFiles() {
			return includedFiles;
		}

		public void setIncludedFiles(Set<String> includedFiles) {
			this.includedFiles = includedFiles;
		}

		public Set<String> getExcludedFiles() {
			return excludedFiles;
		}

		public void setExcludedFiles(Set<String> excludedFiles) {
			this.excludedFiles = excludedFiles;
		}

		public Set<String> getIncludedFolders() {
			return includedFolders;
		}

		public void setIncludedFolders(Set<String> includedFolders) {
			this.includedFolders = includedFolders;
		}

		public Set<String> getExcludedFolders() {
			return excludedFolders;
		}

		public void setExcludedFolders(Set<String> excludedFolders) {
			this.excludedFolders = excludedFolders;
		}

		private String getId() {
			return scanId;
		}

		static File getScanResultFile(String scanResultId) {
			IPath location = OverlayPluginActivator.getWorkspacePluginPath();
			File root = location.toFile();
			File scanFile = new File(root, scanResultId + ".scan"); //$NON-NLS-1$
			return scanFile;
		}

		boolean accepts(String name, boolean isFile) {

			name = PathUtil.useSystemSeparator(name);
			if (name.startsWith(baseDirAsString)) {
				name = name.substring(baseDirAsString.length() + 1);
			}

			boolean res;
			if (isFile) {
				res = includedFiles.contains(name)
						&& !excludedFiles.contains(name);
			} else {
				res = includedFolders.contains(name)
						&& !excludedFolders.contains(name);
			}
			return res;
		}

		private void completeIncludedFolders() {
			Set<String> missingParentFolders = new HashSet<String>();
			for (String folder : includedFolders) {
				IPath filePath = new Path(folder);
				IPath parentPath = filePath.removeLastSegments(1);
				while (parentPath.segmentCount() > 0) {
					String pathAsString = parentPath.toOSString();
					if (!includedFolders.contains(pathAsString)) {
						missingParentFolders.add(pathAsString);
					}
					parentPath = parentPath.removeLastSegments(1);
				}
			}
			includedFolders.addAll(missingParentFolders);

			for (String file : includedFiles) {
				// For /some/foo/bar/file.ext, we need to add
				// /some/foo/bar/
				// /some/foo/
				// /some/
				// as included folders

				IPath filePath = new Path(file);
				IPath parentPath = filePath.removeLastSegments(1);
				while (parentPath.segmentCount() > 0) {
					if (includedFolders.add(parentPath.toOSString())) {
						parentPath = parentPath.removeLastSegments(1);
					} else {
						// Parent hierarchy already added
						break;
					}
				}
			}
		}

		public void writeExternal(ObjectOutput out) throws IOException {
			out.writeUTF(scanId);
			out.writeUTF(baseDirAsString);
			out.writeObject(toString(includedFiles));
			out.writeObject(toString(excludedFiles));
			out.writeObject(toString(includedFolders));
			out.writeObject(toString(excludedFolders));
		}

		public void readExternal(ObjectInput in) throws IOException,
				ClassNotFoundException {
			this.scanId = in.readUTF();
			this.baseDirAsString = in.readUTF();
			this.includedFiles = toSet((String)in.readObject());
			this.excludedFiles = toSet((String)in.readObject());
			this.includedFolders = toSet((String)in.readObject());
			this.excludedFolders = toSet((String)in.readObject());
		}

		private Set<String> toSet(String arrayAsString) {
			if (arrayAsString == null) {
				return Collections.emptySet();
			}
			return new HashSet<String>(Arrays.asList(arrayAsString.split(COLLECTION_SEPARATOR)));
		}

		private String toString(Set<String> stringSet) {
			if (stringSet == null || stringSet.isEmpty()) {
				return ""; //$NON-NLS-1$
			}
			StringBuilder sb = new StringBuilder();
			for (String s : stringSet) {
				sb.append(s).append(COLLECTION_SEPARATOR);	
			}
		    int lastSeparatorIdx = sb.lastIndexOf(COLLECTION_SEPARATOR);
		    if (lastSeparatorIdx > -1) {
		    	sb.deleteCharAt(lastSeparatorIdx);
		    }
			return sb.toString();
		}

		public static void write(ScanResult scanResult) {
			File scanResultFile = getScanResultFile(scanResult.getId());
			ObjectOutputStream oos = null;
			try {
				FileOutputStream fos = new FileOutputStream(scanResultFile);
				oos = new ObjectOutputStream(fos);
				scanResult.writeExternal(oos);
				oos.flush();
			} catch (Exception ex) {
				LOG.error("Unable to serialize scan results", ex); //$NON-NLS-1$
			} finally {
				try {
					if (oos != null) {
						oos.close();
					}
				} catch (Exception ignore) {
				}
			}
		}

		public static ScanResult read(String scanResultId) {
			File scanResultFile = getScanResultFile(scanResultId);
			if (!scanResultFile.isFile()) {
				return null;
			}
			ScanResult scanResult = null;
			ObjectInputStream ois = null;
			try {
				FileInputStream fis = new FileInputStream(scanResultFile);
				ois = new ObjectInputStream(fis);
				scanResult = new ScanResult();
				scanResult.readExternal(ois);

			} catch (Exception ex) {
				LOG.error("Unable to read scan results", ex); //$NON-NLS-1$
				scanResultFile.delete();
				scanResult = null;
			} finally {
				try {
					if (ois != null) {
						ois.close();
					}
				} catch (Exception ignore) {
				}
			}
			return scanResult;
		}

	}

}
