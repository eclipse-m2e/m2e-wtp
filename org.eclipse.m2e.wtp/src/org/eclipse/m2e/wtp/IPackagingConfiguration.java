/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp;


/**
 * IPackagingOption
 *
 * @author Fred Bricon
 */
public interface IPackagingConfiguration {

  boolean isPackaged(String deployedFileName);
}
