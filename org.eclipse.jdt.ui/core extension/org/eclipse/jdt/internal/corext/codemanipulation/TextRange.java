/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.codemanipulation;

import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.internal.core.Assert;

public final class TextRange {

	/* package */ int fOffset;
	/* package */ int fLength;

	/**
	 * Creates a insert position with the given offset.
	 *
	 * @param offset the position offset, must be >= 0
	 */
	public TextRange(int offset) {
		this(offset, 0);
	}
	
	/**
	 * Creates a new range with the given offset and length.
	 *
	 * @param offset the position offset, must be >= 0
	 * @param length the position length, must be >= 0
	 */
	public TextRange(int offset, int length) {
		fOffset= offset;
		Assert.isTrue(fOffset >= 0);
		fLength= length;
		Assert.isTrue(fLength >= 0);
	}
	
	/**
	 * Creates a new range from the given source range.
	 * 
	 * @range the source range denoting offset and length
	 */
	public TextRange(ISourceRange range) {
		this(range.getOffset(), range.getLength());
	}
	
	/**
	 * Returns the offset of this range.
	 *
	 * @return the length of this range
	 */
	public int getOffset() {
		return fOffset;
	}
	
	/**
	 * Returns the length of this range.
	 *
	 * @return the length of this range
	 */
	public int getLength() {
		return fLength;
	}
	
	/**
	 * Returns the end position of this range. This position is covered
	 * by this range.
	 * 
	 * @return the end position
	 */
	public int getEnd() {
		return fOffset + fLength - 1;
	}
	
	/**
	 * Creates a copy of this <code>TextRange</code>.
	 * 
	 * @return a copy of this <code>TextRange</code>
	 */
	public TextRange copy() {
		return new TextRange(fOffset, fLength);
	}
	
	/* package */ boolean isInsertionPoint() {
		return fLength == 0;
	}
	
	/* package */ boolean equals(TextRange range) {
		return fOffset == range.fOffset && fLength == range.fLength;
	}

	/* package */ boolean isEqualInsertionPoint(TextRange range)	{
		return fLength == 0 && range.fLength == 0 && fOffset == range.fOffset;
	}

	/* package */ boolean liesBehind(TextRange range) {
		return fOffset >= range.fOffset + range.fLength;
	}

	/* package */ boolean isInsertionPointAt(int o) {
		return fOffset == o && fLength == 0;
	}
	
	/* package */ boolean covers(TextRange other) {
		if (fLength == 0) {	// an insertion point can't cover anything
			return false;
		} else if (other.fLength == 0) {
			int otherOffset= other.fOffset;
			return fOffset < otherOffset && otherOffset < fOffset + fLength;
		} else {
			int otherOffset= other.fOffset;
			return fOffset <= otherOffset && otherOffset + other.fLength <= fOffset + fLength;
		}
	}
	/* non Java-doc
	 * @see Object#toString()
	 */
	public String toString() {
		StringBuffer buffer= new StringBuffer();
		buffer.append("Offset: ");
		buffer.append(fOffset);
		buffer.append("Length: ");
		buffer.append(fLength);
		return buffer.toString();
	}
}

