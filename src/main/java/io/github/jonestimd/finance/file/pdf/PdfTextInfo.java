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

import javafx.geometry.Point2D;

public class PdfTextInfo {
    public final PdfFontInfo fontInfo;
    public final String text;
    public final float x;
    public final float y;
    public final float charSpacing;
    public final float horizontalScale;
    public final float verticalScale;

    public PdfTextInfo(PdfFontInfo fontInfo, String text, Point2D start, float charSpacing, float horizontalScale, float verticalScale) {
        this.fontInfo = fontInfo;
        this.text = text;
        this.x = (float) start.getX();
        this.y = (float) start.getY();
        this.charSpacing = charSpacing;
        this.horizontalScale = horizontalScale;
        this.verticalScale = verticalScale;
    }
}
