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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.lowagie.text.pdf.CMapAwareDocumentFont;
import com.lowagie.text.pdf.PRIndirectReference;
import com.lowagie.text.pdf.PRStream;
import com.lowagie.text.pdf.PdfDictionary;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfNumber;
import com.lowagie.text.pdf.PdfObject;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStream;

public class PdfFontInfo {
    private final PdfDictionary fontDict;
    private final CMapAwareDocumentFont font;
    private final float fontSize; // units in user space. Default unit is 1/72 inch?

    public PdfFontInfo(PRIndirectReference fontReference, PdfNumber fontSize) {
        this.fontDict = (PdfDictionary) PdfReader.getPdfObject(fontReference);
        this.font = new CMapAwareDocumentFont(fontReference);
        this.fontSize = fontSize.floatValue();
    }

    public String getFontName() {
        return font.getPostscriptFontName();
    }

    public InputStream getFontStream() throws IOException {
        PdfDictionary fontDesc = (PdfDictionary) PdfReader.getPdfObject(fontDict.get(PdfName.FONTDESCRIPTOR));
        PdfStream stream = fontDesc.getAsStream(PdfName.FONTFILE);
        return new ByteArrayInputStream(PdfReader.getStreamBytes((PRStream) stream));
    }

    public String decode(PdfObject operand) {
        byte[] bytes = operand.getBytes();
        return this.font.decode(bytes, 0, bytes.length);
    }

    public float getWidth(String text) {
        return this.font.getWidthPoint(text, this.fontSize);
    }

    public float getSize() {
        return this.fontSize;
    }
}
