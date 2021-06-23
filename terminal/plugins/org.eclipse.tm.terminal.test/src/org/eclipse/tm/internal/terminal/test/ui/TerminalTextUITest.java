/*******************************************************************************
 * Copyright (c) 2007, 2018 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Michael Scharf (Wind River) - initial API and implementation
 *******************************************************************************/
package org.eclipse.tm.internal.terminal.test.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.tm.internal.terminal.textcanvas.ITextCanvasModel;
import org.eclipse.tm.internal.terminal.textcanvas.ITextCanvasModelListener;
import org.eclipse.tm.internal.terminal.textcanvas.PollingTextCanvasModel;
import org.eclipse.tm.internal.terminal.textcanvas.StyleMap;
import org.eclipse.tm.internal.terminal.textcanvas.TextCanvas;
import org.eclipse.tm.internal.terminal.textcanvas.TextLineRenderer;
import org.eclipse.tm.terminal.model.ITerminalTextData;
import org.eclipse.tm.terminal.model.ITerminalTextDataReadOnly;
import org.eclipse.tm.terminal.model.ITerminalTextDataSnapshot;
import org.eclipse.tm.terminal.model.LineSegment;
import org.eclipse.tm.terminal.model.TerminalTextDataFactory;

/**
 * adjust columns when table gets resized....
 *
 */
public class TerminalTextUITest {
	static TextCanvas fgTextCanvas;
	static ITextCanvasModel fgModel;
	static ITerminalTextData fTerminalModel;
	static Label fStatusLabel;
	static volatile int fHeight;
	static volatile int fWidth;
	static DataReader fDataReader;
	static List fDataReaders = new ArrayList();
	private static Text heightText;
	private static StyledText fStyledText;

	static class Status implements IStatus {
		@Override
		public void setStatus(final String s) {
			if (!fStatusLabel.isDisposed())
				Display.getDefault().asyncExec(() -> {
					if (!fStatusLabel.isDisposed())
						fStatusLabel.setText(s);
				});
		}

	}

	public static void main(String[] args) {
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setLayout(new GridLayout());
		Composite composite = new Composite(shell, SWT.NONE);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		RowLayout layout = new RowLayout(SWT.HORIZONTAL);
		layout.wrap = true;
		layout.fill = false;
		fTerminalModel = TerminalTextDataFactory.makeTerminalTextData();
		fHeight = 24;
		fWidth = 80;
		fTerminalModel.setDimensions(fHeight, fWidth);
		fTerminalModel.setMaxHeight(fHeight);
		ITerminalTextDataSnapshot snapshot = fTerminalModel.makeSnapshot();
		// TODO how to get the initial size correctly!
		snapshot.updateSnapshot(false);
		fgModel = new PollingTextCanvasModel(snapshot);
		if (false) {
			fgTextCanvas = new TextCanvas(shell, fgModel, SWT.NONE, new TextLineRenderer(fgModel));
			fgTextCanvas.setLayoutData(new GridData(GridData.FILL_BOTH));
		} else {
			ITerminalTextDataReadOnly terminalText = fgModel.getTerminalText();
			StyleMap styleMap = new StyleMap();
			fStyledText = new StyledText(shell, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.WRAP);
			fStyledText.setLayoutData(new GridData(GridData.FILL_BOTH));
			fStyledText.setText(("X".repeat(60) + "\n").repeat(10));
			//			fStyledText.addLineStyleListener(new LineStyleListener() {
			//
			//				@Override
			//				public void lineGetStyle(LineStyleEvent event) {
			//					int lineAtOffset = fStyledText.getLineAtOffset(event.lineOffset);
			//
			//					LineSegment[] lineSegments = terminalText.getLineSegments(lineAtOffset, 0, terminalText.getWidth());
			//					StyleRange[] ranges = new StyleRange[lineSegments.length];
			//					for (int i = 0; i < lineSegments.length; i++) {
			//						RGB foregrondColor = styleMap.getForegrondRGB(lineSegments[i].getStyle());
			//						RGB backgroundColor = styleMap.getBackgroundRGB(lineSegments[i].getStyle());
			//
			//						Font f = styleMap.getFont(lineSegments[i].getStyle());
			//						ranges[i] = new StyleRange(event.lineOffset + lineSegments[i].getColumn(),
			//								lineSegments[i].getText().length(), new Color(foregrondColor),
			//								new Color(backgroundColor));
			//						ranges[i].font = f;
			//
			//					}
			//					event.styles = ranges;
			//				}
			//			});

			fgModel.addCellCanvasModelListener(new ITextCanvasModelListener() {
				@Override
				public void rangeChanged(int col, int line, int width, int height) {
					if (fStyledText.isDisposed())
						return;
					StringBuilder sb = new StringBuilder();
					List<StyleRange> ranges = new ArrayList<>();
					synchronized (terminalText) {
						for (int i = 0; i < terminalText.getHeight(); i++) {
							LineSegment[] lineSegments = terminalText.getLineSegments(i, 0, terminalText.getWidth());
							int lineOffset = sb.length();
							for (int j = 0; j < lineSegments.length; j++) {

								RGB foregrondColor = styleMap.getForegrondRGB(lineSegments[j].getStyle());
								RGB backgroundColor = styleMap.getBackgroundRGB(lineSegments[j].getStyle());

								Font f = styleMap.getFont(lineSegments[j].getStyle());
								StyleRange range = new StyleRange(lineOffset + lineSegments[j].getColumn(),
										lineSegments[j].getText().length(), new Color(foregrondColor),
										new Color(backgroundColor));
								range.font = f;

								sb.append(lineSegments[j].getText().replace('\0', ' '));
								ranges.add(range);
							}
							sb.append("\n");
						}
						sb.deleteCharAt(sb.length() - 1);
					}
					fStyledText.setText(sb.toString());
					fStyledText.setStyleRanges(ranges.toArray(StyleRange[]::new));
					fStyledText.redraw();

				}

				@Override
				public void dimensionsChanged(int cols, int rows) {
					//					if (isDisposed())
					//						return;
					//					calculateGrid();
				}

				@Override
				public void terminalDataChanged() {
					//					if (isDisposed())
					//						return;
					//
					//					// scroll to end (unless scroll lock is active)
					//					if (!fResizing) {
					//						calculateGrid();
					//						scrollToEnd();
					//					}
				}
			});
		}

		composite.setLayout(layout);
		addAutorevealCursorButton(composite);
		Text maxHeightText = addMaxHeightInput(composite);
		addHeightInput(composite, maxHeightText);
		addWidthText(composite);
		Text throttleText = addThrottleText(composite);

		IStatus status = new Status();
		DataReader reader = new DataReader("Line Count", fTerminalModel, new LineCountingDataSource(), status);
		addDataReader(composite, reader);
		reader = new DataReader("Fast", fTerminalModel, new FastDataSource(), status);
		addDataReader(composite, reader);
		reader = new DataReader("Colors", fTerminalModel, new VT100DataSource(), status);
		addDataReader(composite, reader);
		reader = new DataReader("Random", fTerminalModel, new RandomDataSource(), status);
		addDataReader(composite, reader);
		for (int i = 0; i < args.length; i++) {
			File file = new File(args[i]);
			reader = new DataReader(file.getName(), fTerminalModel, new VT100DataSource(args[i]), status);
			addDataReader(composite, reader);
		}
		addStopAllButton(composite, reader);

		fStatusLabel = new Label(shell, SWT.NONE);
		fStatusLabel.setLayoutData(new GridData(250, 15));
		throttleText.setText("100");
		setThrottleForAll(100);

		if (args.length == 0)
			addLabel(composite, "[Files can be added via commandline]");
		shell.setSize(600, 300);
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
	}

	private static Text addMaxHeightInput(Composite composite) {
		addLabel(composite, "maxHeight:");
		final Text maxHeightText = new Text(composite, SWT.BORDER);
		setLayoutData(maxHeightText, 30);
		maxHeightText.addModifyListener(e -> {
			synchronized (fTerminalModel) {
				int height = textToInt(maxHeightText);
				if (height < 1)
					return;
				if (fTerminalModel.getHeight() > height) {
					fTerminalModel.scroll(0, fTerminalModel.getHeight(), height - fTerminalModel.getHeight());
					fTerminalModel.setDimensions(height, fTerminalModel.getWidth());
					heightText.setText(height + "");
				}
				fTerminalModel.setMaxHeight(height);
			}
		});
		maxHeightText.setText(fHeight + "");
		return maxHeightText;
	}

	private static void addHeightInput(Composite composite, final Text maxHeightText) {
		addLabel(composite, "heigth:");
		heightText = new Text(composite, SWT.BORDER);
		setLayoutData(heightText, 30);
		heightText.addModifyListener(e -> {
			synchronized (fTerminalModel) {
				int height = textToInt(heightText);
				if (height < 1)
					return;
				maxHeightText.setText("" + height);
				fTerminalModel.setDimensions(height, fTerminalModel.getWidth());
				fTerminalModel.setMaxHeight(height);
			}
		});
		heightText.setText(fHeight + "");
	}

	private static Text addWidthText(Composite composite) {
		addLabel(composite, "width:");
		final Text widthText = new Text(composite, SWT.BORDER);
		setLayoutData(widthText, 30);
		widthText.addModifyListener(e -> {
			synchronized (fTerminalModel) {
				int width = textToInt(widthText);
				if (width > 1)
					fTerminalModel.setDimensions(fTerminalModel.getHeight(), width);
			}
		});
		widthText.setText(fWidth + "");
		return widthText;
	}

	private static Text addThrottleText(Composite composite) {
		addLabel(composite, "throttle:");
		final Text throttleText = new Text(composite, SWT.BORDER);
		setLayoutData(throttleText, 30);
		throttleText.addModifyListener(e -> {
			synchronized (fTerminalModel) {
				int throttle = textToInt(throttleText);
				setThrottleForAll(throttle);
			}
		});
		return throttleText;
	}

	private static void addStopAllButton(Composite composite, DataReader reader) {
		final Button stopAllButton = new Button(composite, SWT.CHECK);
		stopAllButton.setText("Stop ALL");
		stopAllButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean stop = stopAllButton.getSelection();
				for (Iterator iterator = fDataReaders.iterator(); iterator.hasNext();) {
					DataReader reader = (DataReader) iterator.next();
					reader.setStop(stop);

				}
			}
		});
		stopAllButton.setSelection(reader.isStart());
	}

	private static void addAutorevealCursorButton(Composite composite) {
		if (fgTextCanvas == null) {
			return;
		}
		final Button button = new Button(composite, SWT.CHECK);
		button.setText("ScrollLock");
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean scrollLock = button.getSelection();
				fgTextCanvas.setScrollLock(scrollLock);
			}
		});
		button.setSelection(fgTextCanvas.isScrollLock());
	}

	private static void addLabel(Composite composite, String message) {
		Label label;
		label = new Label(composite, SWT.NONE);
		label.setText(message);
	}

	private static void addDataReader(Composite composite, final DataReader reader) {
		fDataReaders.add(reader);
		final Button button = new Button(composite, SWT.CHECK);
		button.setText(reader.getName());
		button.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				reader.setStart(button.getSelection());
			}
		});
		button.setSelection(reader.isStart());

	}

	static private void setThrottleForAll(int throttle) {
		for (Iterator iterator = fDataReaders.iterator(); iterator.hasNext();) {
			DataReader reader = (DataReader) iterator.next();
			reader.setThrottleTime(throttle);
		}
	}

	static void setLayoutData(Control c, int width) {
		c.setLayoutData(new RowData(width, -1));
	}

	static int textToInt(Text text) {
		try {
			return Integer.valueOf(text.getText()).intValue();
		} catch (Exception ex) {
			return 0;
		}
	}
}
