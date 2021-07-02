/*******************************************************************************
 * Copyright (c) 2021 Kichwa Coders Canada Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 */
package org.eclipse.tm.internal.terminal.textcanvas;

import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.tm.terminal.model.ITerminalTextDataSnapshot;

/**
 * A polling model that does the polling not on the main/UI thread.
 */
public class PollingNonUITextCanvasModel extends AbstractTextCanvasModel implements IPollingTextCanvasModel {
	private static final int DEFAULT_POLL_INTERVAL = 50;
	private Timer timer;
	private TimerTask timerTask;

	public PollingNonUITextCanvasModel(ITerminalTextDataSnapshot snapshot) {
		super(snapshot);

		boolean isDaemon = true;
		timer = new Timer(isDaemon);
		startPolling();
	}

	@Override
	public void stopPolling() {
		if (timerTask != null) {
			timerTask.cancel();
			timerTask = null;
		}
	}

	@Override
	public void startPolling() {
		if (timerTask != null) {
			timerTask.cancel();
		}
		timerTask = new TimerTask() {
			@Override
			public void run() {
				update();
			}
		};
		timer.schedule(timerTask, DEFAULT_POLL_INTERVAL, DEFAULT_POLL_INTERVAL);
	}
}
