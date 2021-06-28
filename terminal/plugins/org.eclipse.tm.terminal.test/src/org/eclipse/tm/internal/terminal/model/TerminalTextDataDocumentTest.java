package org.eclipse.tm.internal.terminal.model;

import java.util.BitSet;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.tm.terminal.model.ITerminalTextData;
import org.eclipse.tm.terminal.model.ITerminalTextDataSnapshot;
import org.eclipse.tm.terminal.model.LineSegment;
import org.eclipse.tm.terminal.model.TerminalStyle;

public class TerminalTextDataDocumentTest extends AbstractITerminalTextDataTest {

	private static final class ITerminalTextDataImplementation implements ITerminalTextData {

		private Document document = new Document();
		private int width = 0;
		private int height = 0;
		private int maxHeight;
		private int cursorLine;
		private int cursorColumn;
		final private BitSet wrappedLines = new BitSet();

		/**
		 * This is used in asserts to throw an {@link RuntimeException}.
		 * This is useful for tests.
		 * @return never -- throws an exception
		 */
		private boolean throwRuntimeException() {
			throw new RuntimeException();
		}

		public ITerminalTextDataImplementation() {

		}

		@Override
		public ITerminalTextDataSnapshot makeSnapshot() {
			throw new UnsupportedOperationException("makeSnapshot");
		}

		@Override
		public boolean isWrappedLine(int line) {
			return wrappedLines.get(line);
		}

		@Override
		public int getWidth() {
			// TODO Auto-generated method stub
			return width;
		}

		@Override
		public TerminalStyle[] getStyles(int line) {
			throw new UnsupportedOperationException();
		}

		@Override
		public TerminalStyle getStyle(int line, int column) {
			throw new UnsupportedOperationException();

		}

		@Override
		public LineSegment[] getLineSegments(int line, int startCol, int numberOfCols) {
			throw new UnsupportedOperationException();

		}

		@Override
		public int getHeight() {
			return height;
		}

		@Override
		public int getCursorLine() {
			return cursorLine;

		}

		@Override
		public int getCursorColumn() {
			return cursorColumn;
		}

		@Override
		public char[] getChars(int line) {
			if (line < 0 || line >= height)
				throwRuntimeException();
			try {
				int lineOffset = document.getLineOffset(line);
				int length = document.getLineLength(line) - document.getLineDelimiter(line).length();
				String string = document.get(lineOffset, length);
				return string.toCharArray();
			} catch (BadLocationException e) {
				throw new RuntimeException(e);
			}

		}

		@Override
		public void setChar(int line, int column, char c, TerminalStyle style) {
			if (column < 0 || column >= width || line < 0 || line >= height)
				throwRuntimeException();
			try {
				int lineOffset = document.getLineOffset(line);
				int lineLength = document.getLineLength(line) - document.getLineDelimiter(line).length();

				int missingCols = column - lineLength;
				if (missingCols >= 0) {
					document.replace(lineOffset + lineLength, 0, "\0".repeat(missingCols + 1));
				}
				document.replace(lineOffset + column, 1, String.valueOf(c));

			} catch (BadLocationException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public char getChar(int line, int column) {
			if (column < 0 || column >= width || line < 0 || line >= height)
				throwRuntimeException();
			try {
				if (line >= document.getNumberOfLines()) {
					return '\0';
				}
				int lineOffset = document.getLineOffset(line);
				int lineLength2 = document.getLineLength(line);
				if (lineLength2 == 0) {
					return '\0';
				}
				int lineLength = lineLength2 - document.getLineDelimiter(line).length();
				int pos = lineOffset + column;
				if (column >= lineLength) {
					return '\0';
				} else {
					return document.getChar(pos);
				}
			} catch (BadLocationException e) {
				throw new RuntimeException(e);
			}
		}

		private void cleanLines(int line, int len) {
			for (int i = line; i < line + len; i++) {
				cleanLine(i);
			}
		}

		@Override
		public void cleanLine(int line) {
			try {
				int lineOffset = document.getLineOffset(line);
				String lineDelimiter = document.getLineDelimiter(line);
				int length = lineDelimiter.length();
				int lineLength2 = document.getLineLength(line);
				int lineLength = lineLength2 - length;
				document.replace(lineOffset, lineLength, "");
				wrappedLines.clear(line);

			} catch (BadLocationException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void setWrappedLine(int line) {
			wrappedLines.set(line);
		}

		@Override
		public void setMaxHeight(int height) {
			this.maxHeight = height;
		}

		@Override
		public void setDimensions(int height, int width) {
			assert height >= 0 || throwRuntimeException();
			assert width >= 0 || throwRuntimeException();
			this.height = height;
			this.width = width;
			int missingLines = height - (document.getNumberOfLines() - 1);
			if (missingLines >= 0) {
				try {
					document.replace(document.getLength(), 0,
							document.getDefaultLineDelimiter().repeat(missingLines + 1));

				} catch (BadLocationException e) {
					throw new RuntimeException(e);
				}
			}

		}

		@Override
		public void setCursorLine(int line) {
			throw new UnsupportedOperationException();

		}

		@Override
		public void setCursorColumn(int column) {
			throw new UnsupportedOperationException();

		}

		@Override
		public void setChars(int line, int column, char[] chars, int start, int len, TerminalStyle style) {
			// TODO optimize
			for (int i = 0; i < len; i++) {
				setChar(line, column + i, chars[i + start], style);
			}
		}

		@Override
		public void scroll(int startLine, int size, int shift) {
			assert startLine + size <= getHeight() || throwRuntimeException();
			try {
				if (Math.abs(shift) > size) {
					int outStart = document.getLineOffset(startLine);
					int outLength = document.getLineOffset(startLine + size) - outStart;
					document.replace(outStart, outLength, "");
					document.replace(outStart, 0, document.getDefaultLineDelimiter().repeat(size));
				} else if (shift >= 0) {
					int outStart = document.getLineOffset(startLine);
					int outLength = document.getLineOffset(startLine + size) - outStart;
					int inStart = outStart;
					int inLength = document.getLineOffset(startLine + size - shift) - inStart;
					document.replace(outStart, outLength, document.get(inStart, inLength));
					document.replace(outStart, 0, document.getDefaultLineDelimiter().repeat(shift));
					for (int i = startLine + size - 1; i >= startLine && i - shift >= 0; i--) {
						wrappedLines.set(i, wrappedLines.get(i - shift));
					}
					cleanLines(startLine, Math.min(shift, getHeight() - startLine));

				} else {
					int outStart = document.getLineOffset(startLine);
					int outLength = document.getLineOffset(startLine + size) - outStart;
					int inStart = document.getLineOffset(startLine + -shift);
					int inLength = document.getLineOffset(startLine + size) - inStart;
					document.replace(outStart + outLength, 0, document.getDefaultLineDelimiter().repeat(-shift));
					document.replace(outStart, outLength, document.get(inStart, inLength));
					for (int i = startLine; i < startLine + size + shift; i++) {
						wrappedLines.set(i, wrappedLines.get(i - shift));
					}
					cleanLines(Math.max(startLine, startLine + size + shift),
							Math.min(-shift, getHeight() - startLine));

				}
			} catch (BadLocationException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public int getMaxHeight() {
			return maxHeight;
		}

		@Override
		public void copyRange(ITerminalTextData source, int sourceStartLine, int destStartLine, int length) {
			// TODO optimize
			for (int i = 0; i < length; i++) {
				copyLine(source, i + sourceStartLine, i + destStartLine);
			}
		}

		@Override
		public void copyLine(ITerminalTextData source, int sourceLine, int destLine) {
			if (destLine < 0 || destLine >= height)
				throwRuntimeException();
			char[] chars = source.getChars(sourceLine);
			if (chars == null) {
				chars = new char[] {};
			}
			try {
				document.replace(document.getLineOffset(destLine), document.getLineLength(destLine),
						new String(chars) + document.getDefaultLineDelimiter());
				wrappedLines.set(destLine, source.isWrappedLine(sourceLine));
			} catch (BadLocationException e) {
				throw new RuntimeException(e);

			}

		}

		@Override
		public void copy(ITerminalTextData source) {
			width = source.getWidth();
			height = source.getHeight();
			document.set("");
			copyRange(source, 0, 0, height);
			cursorLine = source.getCursorLine();
			cursorColumn = source.getCursorColumn();
		}

		@Override
		public void addLine() {
			if (maxHeight > 0 && getHeight() < maxHeight) {
				setDimensions(getHeight() + 1, getWidth());
			} else {
				scroll(0, getHeight(), -1);
			}
		}
	}

	@Override
	protected ITerminalTextData makeITerminalTextData() {
		return new ITerminalTextDataImplementation();
	}

}
