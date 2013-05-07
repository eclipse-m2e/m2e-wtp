/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.eclipse.m2e.wtp.overlay.internal.modulecore;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.tools.ant.types.selectors.SelectorUtils;
import org.eclipse.m2e.wtp.overlay.internal.utilities.PathUtil;

/**
 * Filter resources based on inclusion/exclusion patterns.
 * <br/>
 * This class is derived from ANT's {@link org.apache.tools.ant.DirectoryScanner}
 * 
 * @provisional This class has been added as part of a work in progress. 
 * It is not guaranteed to work or remain the same in future releases. 
 * For more information contact <a href="mailto:m2e-wtp-dev@eclipse.org">m2e-wtp-dev@eclipse.org</a>.
 * 
 */
public class DynamicResourceFilter implements IResourceFilter {

    protected static final String[] DEFAULTEXCLUDES = {
        // Miscellaneous typical temporary files
        "**/*~", //$NON-NLS-1$
        "**/#*#",//$NON-NLS-1$
        "**/.#*",//$NON-NLS-1$
        "**/%*%",//$NON-NLS-1$
        "**/._*",//$NON-NLS-1$

        // CVS
        "**/CVS",//$NON-NLS-1$
        "**/CVS/**",//$NON-NLS-1$
        "**/.cvsignore",//$NON-NLS-1$

        // SCCS
        "**/SCCS",//$NON-NLS-1$
        "**/SCCS/**",//$NON-NLS-1$

        // Visual SourceSafe
        "**/vssver.scc",//$NON-NLS-1$

        // Subversion
        "**/.svn",//$NON-NLS-1$
        "**/.svn/**",//$NON-NLS-1$

        // Mac
        "**/.DS_Store"//$NON-NLS-1$
    };
	
    /** The patterns for the files to be included. */
    protected String[] includes;

    /** The patterns for the files to be excluded. */
    protected String[] excludes;

    protected boolean isCaseSensitive = true;

    /**
     * Array of all include patterns that contain wildcards.
     *
     * <p>Gets lazily initialized on the first invocation of
     * isIncluded or isExcluded and cleared at the end of the scan
     * method (cleared in clearCaches, actually).</p>
     *
     * @since Ant 1.6.3
     */
    private String[] includePatterns;

    /**
     * Array of all exclude patterns that contain wildcards.
     *
     * <p>Gets lazily initialized on the first invocation of
     * isIncluded or isExcluded and cleared at the end of the scan
     * method (cleared in clearCaches, actually).</p>
     *
     * @since Ant 1.6.3
     */
    private String[] excludePatterns;

    /**
     * Have the non-pattern sets and pattern arrays for in- and
     * excludes been initialized?
     *
     * @since Ant 1.6.3
     */
    private boolean areNonPatternSetsReady = false;
    
    /**
     * Set of all include patterns that are full file names and don't
     * contain any wildcards.
     *
     * <p>If this instance is not case sensitive, the file names get
     * turned to lower case.</p>
     *
     * <p>Gets lazily initialized on the first invocation of
     * isIncluded or isExcluded and cleared at the end of the scan
     * method (cleared in clearCaches, actually).</p>
     *
     * @since Ant 1.6.3
     */
    private Set<String> includeNonPatterns = new HashSet<String>();

    /**
     * Set of all include patterns that are full file names and don't
     * contain any wildcards.
     *
     * <p>If this instance is not case sensitive, the file names get
     * turned to lower case.</p>
     *
     * <p>Gets lazily initialized on the first invocation of
     * isIncluded or isExcluded and cleared at the end of the scan
     * method (cleared in clearCaches, actually).</p>
     *
     * @since Ant 1.6.3
     */
    private Set<String> excludeNonPatterns = new HashSet<String>();

    
    public DynamicResourceFilter(Collection<String> inclusions, Collection<String> exclusions) {
		if (inclusions != null && !inclusions.isEmpty()) {
			setIncludes(inclusions.toArray(new String[inclusions.size()]));
		} else {
			setIncludes(new String[]{"**/**"}); //$NON-NLS-1$
		}
		if (exclusions != null && !exclusions.isEmpty()) {
			addExcludes(exclusions.toArray(new String[exclusions.size()]));
		}
		addExcludes(DEFAULTEXCLUDES);
	}
    
    public synchronized void setIncludes(String[] includes) {
        if (includes == null) {
            this.includes = null;
        } else {
            this.includes = new String[includes.length];
            for (int i = 0; i < includes.length; i++) {
                this.includes[i] = normalizePattern(includes[i]);
            }
        }
    }

    public synchronized void setExcludes(String[] excludes) {
        if (excludes == null) {
            this.excludes = null;
        } else {
            this.excludes = new String[excludes.length];
            for (int i = 0; i < excludes.length; i++) {
                this.excludes[i] = normalizePattern(excludes[i]);
            }
        }
    }    
  
    public synchronized void addExcludes(String[] excludes) {
        if (excludes != null && excludes.length > 0) {
            if (this.excludes != null && this.excludes.length > 0) {
                String[] tmp = new String[excludes.length
                                          + this.excludes.length];
                System.arraycopy(this.excludes, 0, tmp, 0,
                                 this.excludes.length);
                for (int i = 0; i < excludes.length; i++) {
                    tmp[this.excludes.length + i] =
                        normalizePattern(excludes[i]);
                }
                this.excludes = tmp;
            } else {
                setExcludes(excludes);
            }
        }
    }
    
    private static String normalizePattern(String p) {
        String pattern = PathUtil.useSystemSeparator(p);
        if (pattern.endsWith(File.separator)) {
            pattern += "**";//$NON-NLS-1$
        }
        return pattern;
    }
    
    /**
     * Ensure that the in|exclude &quot;patterns&quot;
     * have been properly divided up.
     *
     * @since Ant 1.6.3
     */
    private synchronized void ensureNonPatternSetsReady() {
        if (!areNonPatternSetsReady) {
            includePatterns = fillNonPatternSet(includeNonPatterns, includes);
            excludePatterns = fillNonPatternSet(excludeNonPatterns, excludes);
            areNonPatternSetsReady = true;
        }
    }

    /**
     * Add all patterns that are not real patterns (do not contain
     * wildcards) to the set and returns the real patterns.
     *
     * @param set Set to populate.
     * @param patterns String[] of patterns.
     * @since Ant 1.6.3
     */
    private String[] fillNonPatternSet(Set<String> set, String[] patterns) {
        ArrayList<String> al = new ArrayList<String>(patterns.length);
        for (int i = 0; i < patterns.length; i++) {
            if (!SelectorUtils.hasWildcards(patterns[i])) {
                set.add(isCaseSensitive() ? patterns[i]
                    : patterns[i].toUpperCase());
            } else {
                al.add(patterns[i]);
            }
        }
        return set.size() == 0 ? patterns
            : al.toArray(new String[al.size()]);
    }
    
    private boolean isCaseSensitive() {
		return isCaseSensitive;
	}

	/**
     * Test whether or not a name matches against at least one exclude
     * pattern.
     *
     * @param name The name to match. Must not be <code>null</code>.
     * @return <code>true</code> when the name matches against at least one
     *         exclude pattern, or <code>false</code> otherwise.
     */
    protected boolean isExcluded(String name) {
        ensureNonPatternSetsReady();

        if (isCaseSensitive()
            ? excludeNonPatterns.contains(name)
            : excludeNonPatterns.contains(name.toUpperCase())) {
            return true;
        }
        for (int i = 0; i < excludePatterns.length; i++) {
            if (matchPath(excludePatterns[i], name, isCaseSensitive())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Test whether or not a name matches against at least one include
     * pattern.
     *
     * @param name The name to match. Must not be <code>null</code>.
     * @return <code>true</code> when the name matches against at least one
     *         include pattern, or <code>false</code> otherwise.
     */
    protected boolean isIncluded(String name) {
        ensureNonPatternSetsReady();

        if (isCaseSensitive()
            ? includeNonPatterns.contains(name)
            : includeNonPatterns.contains(name.toUpperCase())) {
            return true;
        }
        for (int i = 0; i < includePatterns.length; i++) {
            if (matchPath(includePatterns[i], name, isCaseSensitive())) {
                return true;
            }
        }
        return false;
    }
    
    protected static boolean matchPath(String pattern, String str,
            boolean isCaseSensitive) {
    	return SelectorUtils.matchPath(pattern, str, isCaseSensitive);
    }

	public boolean accepts(String path, boolean isFile) {
		if (path == null) return false;
		path = PathUtil.useSystemSeparator(path);
		boolean isIncluded = isIncluded(path) && ! isExcluded(path);
		//System.err.println(path + (isIncluded?" included":" excluded"));
		return isIncluded;
	}

}
