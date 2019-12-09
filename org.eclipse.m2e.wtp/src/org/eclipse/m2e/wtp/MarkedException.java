/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.wtp;

/**
 * Represents an {@link Exception} that should be turned into an Error Marker.
 * 
 * @provisional This class has been added as part of a work in progress. 
 * It is not guaranteed to work or remain the same in future releases. 
 * For more information contact <a href="mailto:m2e-wtp-dev@eclipse.org">m2e-wtp-dev@eclipse.org</a>.
 * 
 * @author Fred Bricon
 */
public class MarkedException extends Exception {

  private static final long serialVersionUID = 7182756387257983149L;

  public MarkedException() {
  }

  public MarkedException(String message) {
    super(message);
  }

  public MarkedException(Throwable cause) {
    super(cause);
  }

  public MarkedException(String message, Throwable cause) {
    super(message, cause);
  }

}
