/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.wtp.internal;

import org.eclipse.ui.IMemento;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.ICommonLabelProvider;

/**
 * WTPResourcesLabelProvider
 *
 * @author Eugene Kuleshov
 */
public class WTPResourcesLabelProvider extends WorkbenchLabelProvider implements ICommonLabelProvider {

  @Override
public void init(ICommonContentExtensionSite config) {
  }

  @Override
public void restoreState(IMemento memento) {
  }

  @Override
public void saveState(IMemento memento) {
  }

  @Override
public String getDescription(Object element) {
    if(element instanceof WTPResourcesNode) {
      return ((WTPResourcesNode)element).getLabel();
    }
    return null;
  }
}
