/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * StringUtils
 *
 * @author dyocum
 */
public class StringUtils {
  public static boolean nullOrEmpty(String s){
    return s == null || s.length() == 0;
  }
  
  
  /**
   * Tokenize the given String into a String array via a StringTokenizer.
   * Trims tokens and omits empty tokens.
   * <p>The given delimiters string is supposed to consist of any number of
   * delimiter characters. Each of those characters can be used to separate
   * tokens. A delimiter is always a single character; for multi-character
   * delimiters, consider using <code>delimitedListToStringArray</code>
   * @param str the String to tokenize
   * @param delimiters the delimiter characters, assembled as String
   * (each of those characters is individually considered as delimiter).
   * @return an array of the tokens
   * @see java.util.StringTokenizer
   * @see java.lang.String#trim()
   * @see #delimitedListToStringArray
   */
  public static String[] tokenizeToStringArray(String str, String delimiters) {
    return tokenizeToStringArray(str, delimiters, true, true);
  }

  /**
   * Tokenize the given String into a String array via a StringTokenizer.
   * <p>The given delimiters string is supposed to consist of any number of
   * delimiter characters. Each of those characters can be used to separate
   * tokens. A delimiter is always a single character; for multi-character
   * delimiters, consider using <code>delimitedListToStringArray</code>
   * @param str the String to tokenize
   * @param delimiters the delimiter characters, assembled as String
   * (each of those characters is individually considered as delimiter)
   * @param trimTokens trim the tokens via String's <code>trim</code>
   * @param ignoreEmptyTokens omit empty tokens from the result array
   * (only applies to tokens that are empty after trimming; StringTokenizer
   * will not consider subsequent delimiters as token in the first place).
   * @return an array of the tokens (<code>null</code> if the input String
   * was <code>null</code>)
   * @see java.util.StringTokenizer
   * @see java.lang.String#trim()
   * @see #delimitedListToStringArray
   */
  public static String[] tokenizeToStringArray(
      String str, String delimiters, boolean trimTokens, boolean ignoreEmptyTokens) {

    if (str == null) {
      return null;
    }
    StringTokenizer st = new StringTokenizer(str, delimiters);
    List<String> tokens = new ArrayList<>();
    while (st.hasMoreTokens()) {
      String token = st.nextToken();
      if (trimTokens) {
        token = token.trim();
      }
      if (!ignoreEmptyTokens || token.length() > 0) {
        tokens.add(token);
      }
    }
    return tokens.toArray(new String[tokens.size()]);
  }
  
  /**
   * Joins a String[] to a single String using a comma delimiter
   * @param someArrays
   * @return a non-null String
   */
  public static String joinAsString(String[] ... someArrays) {
    Set<String> stringSet = new LinkedHashSet<>();
    if (someArrays != null) {
      for (String[] strings : someArrays)
        if (strings != null) {
          stringSet.addAll(Arrays.asList(strings));
        }
    }
    return org.codehaus.plexus.util.StringUtils.join(stringSet.iterator(), ","); //$NON-NLS-1$
  }
}
