/*******************************************************************************
 * Copyright (c) 2007, 2021 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 */
package org.eclipse.tm.internal.terminal.textcanvas;

public interface IPollingTextCanvasModel {

	void stopPolling();

	void startPolling();

}