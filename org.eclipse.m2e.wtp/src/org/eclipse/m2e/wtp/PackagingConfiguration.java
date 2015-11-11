/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp;

import java.util.regex.Pattern;

import org.eclipse.m2e.wtp.internal.AntPathMatcher;


/**
 * Packaging configuration based on ANT and %regex[] patterns.
 * 
 * @provisional This class has been added as part of a work in progress. 
 * It is not guaranteed to work or remain the same in future releases. 
 * For more information contact <a href="mailto:m2e-wtp-dev@eclipse.org">m2e-wtp-dev@eclipse.org</a>.
 * 
 * @author Lars Ködderitzsch
 * @author Fred Bricon
 */
public class PackagingConfiguration implements IPackagingConfiguration {

  String[] packagingIncludes;

  String[] packagingExcludes;

  private AntPathMatcher matcher;

  private final static String REGEX_BEGIN = "%regex["; //$NON-NLS-1$
  private final static String REGEX_END = "]"; //$NON-NLS-1$

  public PackagingConfiguration(String[] packagingIncludes, String[] packagingExcludes) {
    this.packagingIncludes = toPortablePathArray(packagingIncludes);
    this.packagingExcludes = toPortablePathArray(packagingExcludes);
    matcher = new AntPathMatcher();
  }

  @Override
public boolean isPackaged(String virtualPath) {
    if (virtualPath == null) {
      return false;
    }
    virtualPath = toPortablePath(virtualPath);
    if (packagingExcludes != null) {
      for(String excl : packagingExcludes) {
        if(matches(excl, virtualPath)) {
          //stop here already, since exclusions have precedence over inclusions
          return false;
        }
      }
    }

    //so the path is not excluded, check if it is included into packaging
    if (packagingIncludes == null || packagingIncludes.length == 0) {
      return true;
    }
    for(String incl : packagingIncludes) {
      if(matches(incl, virtualPath)) {
        return true;
      }
    }

    //Definitely not included
    return false;
  }

  private String[] toPortablePathArray(String[] patterns) {
    if (patterns == null) {
      return null;
    }
    String[] newPatterns = new String[patterns.length];
    for (int i = 0; i < patterns.length; i++) {
      newPatterns[i] = toPortablePath(patterns[i]);
    }
    return newPatterns;
  }
  
  private String toPortablePath(String path) {
    return (path==null)?null:path.replace("\\", "/"); //$NON-NLS-1$ //$NON-NLS-2$
  }
  
  private boolean isRegex(String regEx){
	  return regEx.startsWith(REGEX_BEGIN) && regEx.endsWith(REGEX_END);
  }

  private boolean matches(String pattern, String input){
	  if (isRegex(pattern)){
		  //%regex[] pattern
		  return Pattern.matches(convertToJavaRegEx(pattern), input);
      }
	  return matcher.match(pattern, input);
  }

  private String convertToJavaRegEx(String regEx){
	  return regEx.substring(REGEX_BEGIN.length(), regEx.length()-REGEX_END.length());
  }
}
