/*******************************************************************************
 * Copyright (c) 2003, 2021 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tm.internal.terminal.emulator;

import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tm.internal.terminal.control.ICommandInputField;
import org.eclipse.tm.internal.terminal.control.ITerminalMouseListener;
import org.eclipse.tm.internal.terminal.control.ITerminalViewControl;
import org.eclipse.tm.internal.terminal.control.impl.ITerminalControlForText;
import org.eclipse.tm.internal.terminal.provisional.api.ITerminalConnector;
import org.eclipse.tm.internal.terminal.provisional.api.ITerminalControl;
import org.eclipse.tm.internal.terminal.provisional.api.TerminalState;

public class VT100TerminalControl2 implements ITerminalControlForText, ITerminalControl, ITerminalViewControl {

	@Override
	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setFont(String fontName) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setInvertedColors(boolean invert) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isInvertedColors() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Font getFont() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Control getControl() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Control getRootControl() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isDisposed() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void selectAll() {
		// TODO Auto-generated method stub

	}

	@Override
	public void clearTerminal() {
		// TODO Auto-generated method stub

	}

	@Override
	public void copy() {
		// TODO Auto-generated method stub

	}

	@Override
	public void paste() {
		// TODO Auto-generated method stub

	}

	@Override
	public String getSelection() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Clipboard getClipboard() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void disconnectTerminal() {
		// TODO Auto-generated method stub

	}

	@Override
	public void disposeTerminal() {
		// TODO Auto-generated method stub

	}

	@Override
	public String getSettingsSummary() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ITerminalConnector[] getConnectors() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setConnector(ITerminalConnector connector) {
		// TODO Auto-generated method stub

	}

	@Override
	public void connectTerminal() {
		// TODO Auto-generated method stub

	}

	@Override
	public void sendKey(char arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean pasteString(String string) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isConnected() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setCommandInputField(ICommandInputField inputField) {
		// TODO Auto-generated method stub

	}

	@Override
	public ICommandInputField getCommandInputField() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getBufferLineLimit() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setBufferLineLimit(int bufferLineLimit) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isScrollLock() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setScrollLock(boolean on) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addMouseListener(ITerminalMouseListener listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeMouseListener(ITerminalMouseListener listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getHoverSelection() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setupTerminal(Composite parent) {
		// TODO Auto-generated method stub

	}

	@Override
	public Shell getShell() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setEncoding(String encoding) throws UnsupportedEncodingException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setCharset(Charset charset) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getEncoding() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Charset getCharset() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void displayTextInTerminal(String text) {
		// TODO Auto-generated method stub

	}

	@Override
	public OutputStream getRemoteToTerminalOutputStream() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setMsg(String msg) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setConnectOnEnterIfClosed(boolean on) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isConnectOnEnterIfClosed() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setVT100LineWrapping(boolean enable) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isVT100LineWrapping() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public TerminalState getState() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setState(TerminalState state) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setTerminalTitle(String title) {
		// TODO Auto-generated method stub

	}

	@Override
	public ITerminalConnector getTerminalConnector() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OutputStream getOutputStream() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void enableApplicationCursorKeys(boolean enable) {
		// TODO Auto-generated method stub

	}

}
