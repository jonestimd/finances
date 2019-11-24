// The MIT License (MIT)
//
// Copyright (c) 2019 Tim Jones
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
package io.github.jonestimd.finance.file.pdf;

import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.util.Vector;

public class PdfTextInfo {
    public final PDFont font;
    public final float fontSize;
    public final String text;
    public final float x;
    public final float y;
    public final float charSpacing;
    public final float horizontalScale;

    public PdfTextInfo(PDFont font, float fontSize, String text, Vector start, float charSpacing, float horizontalScale) {
        this.font = font;
        this.fontSize = fontSize;
        this.text = text;
        this.x = start.getX();
        this.y = start.getY();
        this.charSpacing = charSpacing;
        this.horizontalScale = horizontalScale;
    }

    public String getText() {
        return text;
    }

    public Vector getPos() {
        return new Vector(x, y);
    }

    public String toString() {
        return String.format("PdfTextInfo(%s @ %f,%f}", text, x, y);
    }
}
