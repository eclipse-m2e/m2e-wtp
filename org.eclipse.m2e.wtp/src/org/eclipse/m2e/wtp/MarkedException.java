/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp;

/**
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
