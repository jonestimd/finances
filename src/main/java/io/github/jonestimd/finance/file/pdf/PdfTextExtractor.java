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

import java.util.ArrayList;
import java.util.List;

import com.lowagie.text.pdf.PRIndirectReference;
import com.lowagie.text.pdf.PdfDictionary;
import com.lowagie.text.pdf.PdfLiteral;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfNumber;
import com.lowagie.text.pdf.PdfObject;
import javafx.geometry.Point2D;
import javafx.scene.transform.Affine;

public class PdfTextExtractor implements PdfContentHandler<List<PdfTextInfo>> {
    private Affine matrix = new Affine();
    private PdfFontInfo fontInfo;
    private PdfName colorSpace;
    private float color;
    private PdfName strokeColorSpace;
    private float strokeColor;

    private class TextState {
        public Affine matrix = new Affine();
        public float horizontalScale = 1f; // Th
        public float charSpacing; // Tc
        public float wordSpacing; // Tw
        public float lineSpacing; // Tl (leading)
        public final StringBuilder word = new StringBuilder();

        public String text;
        private Point2D pos = new Point2D(0, 0); // TODO use matrix to translate

        public void startWord() {
            addWord();
            word.setLength(0);
            matrix.appendTranslation(pos.getX(), pos.getY());
            pos = new Point2D(0, 0);
        }

        public void append() {
            word.append(text);
            text = null;
        }

        public void addWord() {
            if (text != null) append();
            if (word.length() > 0) {
                Point2D p = matrix.transform(0, 0);
                documentText.add(new PdfTextInfo(fontInfo, word.toString(), (float) p.getX(), (float) p.getY()));
            }
        }

        public void move(float x, float y) {
            pos = pos.add(x, y);
        }
    }

    private TextState textState;

    private List<PdfTextInfo> documentText = new ArrayList<>();

    @Override
    public List<PdfTextInfo> getResult() {
        return documentText;
    }

    @Override
    public void operator(PdfLiteral operator, List<PdfObject> operands, PdfDictionary resources) {
        String operatorName = operator.toString();
        switch (operatorName) {
            case "cm": concatMatrix(operands); break;
            case "cs": setColorSpace((PdfName) operands.get(0)); break;
            case "CS": setStrokeColorSpace((PdfName) operands.get(0)); break;
            case "scn": setColor(operands); break;
            case "SCN": setStrokeColor(operands); break;
            case "g": setGray((PdfNumber) operands.get(0)); break;
            case "G": setStrokeGray((PdfNumber) operands.get(0)); break;
            case "Tf": setTextFont((PdfName) operands.get(0), (PdfNumber) operands.get(1), resources); break;
            case "Td": setTextPosition((PdfNumber) operands.get(0), (PdfNumber) operands.get(1)); break;
            case "Tm": setTextMatrix(operands); break;
            case "Tj": showText(operands.get(0)); break;
            // case "TJ": showTextWithPos(); break; // includes individual glyph positioning
            // case "'": moveToNextLineAndShowText(); break;
            // case "\"": setSpacingMoveToNextLineAndShowText(); break;
            case "Tc": setCharSpacing((PdfNumber) operands.get(0)); break;
            case "Tw": setWordSpacing((PdfNumber) operands.get(0)); break;
            case "Tz": setHorizontalScale((PdfNumber) operands.get(0)); break;
            case "TL": setLineSpacing((PdfNumber) operands.get(0)); break;
            case "BT": beginText(); break;
            case "ET": endText(); break;
            // case "gs": ??(); break; // set text knockout?
            // case "q": saveGraphicsState(); break;
            // case "Q": restoreGraphicsState(); break;
            case "Do": invokeXObject((PdfName) operands.get(0), resources); break;
            default: unhandled(operatorName, operands);
        }
    }

    private void unhandled(String operator, List<PdfObject> operands) {
        // System.out.print("<" + operator + ">");
    }

    protected void invokeXObject(PdfName name, PdfDictionary resources) {
        // PdfName name = (PdfName) operands.get(0);
        // System.err.println(resources.getAsDict(PdfName.XOBJECT).getAsStream(name).get(PdfName.SUBTYPE));
    }

    protected void concatMatrix(List<PdfObject> operands) {
        // matrix = (AffineTransform) matrix.clone();
        matrix.prepend(new Affine(
                ((PdfNumber) operands.get(0)).floatValue(),
                ((PdfNumber) operands.get(1)).floatValue(),
                ((PdfNumber) operands.get(2)).floatValue(),
                ((PdfNumber) operands.get(3)).floatValue(),
                ((PdfNumber) operands.get(4)).floatValue(),
                ((PdfNumber) operands.get(5)).floatValue()));
    }

    protected void setColorSpace(PdfName name) {
        this.colorSpace = name;
    }

    protected void setColor(List<PdfObject> operands) {
        // TODO depends on current color space
        // this.color = color.floatValue();
    }

    protected void setStrokeColorSpace(PdfName name) {
        this.strokeColorSpace = name;
    }

    protected void setStrokeColor(List<PdfObject> operands) {
        // TODO depends on current color space
        // strokeColor = ((PdfNumber) operands.get(0)).floatValue();
    }

    protected void setGray(PdfNumber gray) {
        // set color space to DeviceGray (or the DefaultGray color space) and set gray level
    }

    protected void setStrokeGray(PdfNumber gray) {
        // set color space to DeviceGray (or the DefaultGray color space) and set gray level
    }

    protected void setTextFont(PdfName name, PdfNumber size, PdfDictionary resources) {
        PdfDictionary fontsDictionary = resources.getAsDict(PdfName.FONT);
        PdfObject pdfObject = fontsDictionary.get(name);
        fontInfo = new PdfFontInfo((PRIndirectReference) pdfObject, size);
    }

    protected void setTextPosition(PdfNumber pdfX, PdfNumber pdfY) {
        float x = pdfX.floatValue();
        float y = pdfY.floatValue();
        textState.move(x, y);
        if (textState.text != null) {
            float width = fontInfo.getWidth(textState.text);
            textState.append();
            if (y != 0 || x - width > 1f) textState.startWord();
        }
        else textState.startWord();
    }

    protected void setTextMatrix(List<PdfObject> operands) {
        textState.matrix = new Affine(
                ((PdfNumber) operands.get(0)).doubleValue(),
                ((PdfNumber) operands.get(1)).doubleValue(),
                ((PdfNumber) operands.get(2)).doubleValue(),
                ((PdfNumber) operands.get(3)).doubleValue(),
                ((PdfNumber) operands.get(4)).doubleValue(),
                ((PdfNumber) operands.get(5)).doubleValue());
    }

    protected void setCharSpacing(PdfNumber spacing) {
        // TODO clone state and begin new "word"
        textState.charSpacing = spacing.floatValue();
    }

    protected void setWordSpacing(PdfNumber spacing) {
        // TODO clone state and begin new "word"
        textState.wordSpacing = spacing.floatValue();
    }

    protected void setHorizontalScale(PdfNumber percent) {
        // TODO clone state and begin new "word"
        textState.horizontalScale = percent.floatValue()/100f;
    }

    protected void setLineSpacing(PdfNumber spacing) {
        textState.lineSpacing = spacing.floatValue();
    }

    protected void showText(PdfObject text) {
        textState.text = fontInfo.decode(text);
    }

    protected void beginText() {
        textState = new TextState();
    }

    protected void endText() {
        textState.addWord();
        textState = null;
    }
}
