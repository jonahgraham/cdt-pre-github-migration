package org.eclipse.tm.internal.terminal.textcanvas;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.tm.terminal.model.ITerminalTextDataReadOnly;
import org.eclipse.tm.terminal.model.LineSegment;

public class TextCanvasViewer implements ITextCanvasModelListener {
	private StyleMap styleMap;
	private ITerminalTextDataReadOnly terminalText;
	private boolean scrollLock = false;
	private StyledText styledText;

	public TextCanvasViewer() {
	}

	public Control createControl(Composite parent) {
		styledText = new StyledText(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.READ_ONLY | SWT.MULTI);
		styledText.setAlwaysShowScrollBars(false);
		return styledText;
	}

	public void setInput(ITextCanvasModel fgModel) {
		this.styleMap = new StyleMap();
		ITerminalTextDataReadOnly terminalText = fgModel.getTerminalText();
		this.terminalText = terminalText;
	}

	@Override
	public void rangeChanged(int col, int line, int width, int height) {
		if (styledText.isDisposed())
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
							lineSegments[j].getText().length(), new Color(foregrondColor), new Color(backgroundColor));
					range.font = f;

					sb.append(lineSegments[j].getText().replace('\0', ' '));
					ranges.add(range);
				}
				sb.append("\n");
			}
			sb.deleteCharAt(sb.length() - 1);
		}
		String string = sb.toString();
		StyleRange[] array = ranges.toArray(StyleRange[]::new);
		try {
			styledText.getDisplay().asyncExec(() -> {
				if (styledText.isDisposed())
					return;
				//							fStyledText.rep;
				styledText.setText(string);
				styledText.setStyleRanges(array);
				styledText.setTopIndex(styledText.getLineCount() - 1);
				styledText.redraw();
			});
		} catch (SWTException e) {
			// disposed
		}

	}

	@Override
	public void dimensionsChanged(int cols, int rows) {
		//					if (isDisposed())
		//						return;
		//					calculateGrid();
	}

	@Override
	public void terminalDataChanged() {
		if (styledText.isDisposed())
			return;
		if (!scrollLock) {
			try {
				styledText.getDisplay().asyncExec(() -> {
					if (styledText.isDisposed())
						return;
					styledText.setTopIndex(styledText.getLineCount() - 1);
				});
			} catch (SWTException e) {
				// disposed
			}
		}
	}

	public StyledText getControl() {
		return styledText;
	}

	public StyleMap getStyleMap() {
		return styleMap;
	}
}