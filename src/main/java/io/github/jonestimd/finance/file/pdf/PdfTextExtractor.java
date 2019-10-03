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

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.lowagie.text.pdf.PRIndirectReference;
import com.lowagie.text.pdf.PdfDictionary;
import com.lowagie.text.pdf.PdfLiteral;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfNumber;
import com.lowagie.text.pdf.PdfObject;
import org.apache.commons.collections4.map.HashedMap;

public class PdfTextExtractor implements PdfContentHandler<List<PdfTextInfo>> {
    @FunctionalInterface
    public interface ContentHandler {
        void invoke(List<PdfObject> operands, PdfDictionary resources);
    }

    private Map<String, ContentHandler> operators = new HashedMap<>();
    private AffineTransform matrix = new AffineTransform();
    private PdfFontInfo fontInfo;
    private PdfName colorSpace;
    private float color;
    private PdfName strokeColorSpace;
    private float strokeColor;

    private class TextState {
        public AffineTransform matrix = new AffineTransform();
        public float horizontalScale = 1f; // Th
        public float charSpacing; // Tc
        public float wordSpacing; // Tw
        public float lineSpacing; // Tl (leading)
        public final StringBuilder word = new StringBuilder();

        public String text;
        public final Point2D.Float pos = new Point2D.Float(); // TODO use matrix to translate
        public final Point2D.Float wordPos = new Point2D.Float();

        public void startWord() {
            addWord();
            word.setLength(0);
            wordPos.setLocation(pos);
        }

        public void append() {
            word.append(text);
            text = null;
        }

        public void addWord() {
            if (text != null) append();
            if (word.length() > 0) {
                Point2D.Float p = new Point2D.Float();
                matrix.transform(wordPos, p);
                documentText.add(new PdfTextInfo(fontInfo, word.toString(), p.x, p.y));
            }
        }

        public void move(float x, float y) {
            pos.x += x;
            pos.y += y;
        }
    }

    private TextState textState;

    private List<PdfTextInfo> documentText = new ArrayList<>();

    public PdfTextExtractor() {
        operators.put("cm", this::concatMatrix);
        operators.put("cs", this::setColorSpace);
        operators.put("CS", this::setStrokeColorSpace);
        operators.put("scn", this::setColor);
        operators.put("SCN", this::setStrokeColor);
        operators.put("g", this::setGray);
        operators.put("G", this::setStrokeGray);
        operators.put("Tf", this::setTextFont);
        operators.put("Td", this::setTextPosition);
        operators.put("Tm", this::setTextMatrix);
        operators.put("Tj", this::showText);
        // operators.put("TJ", this::showTextWithPos); // includes individual glyph positioning
        // operators.put("'", this::moveToNextLineAndShowText);
        // operators.put("\"", this::setSpacingMoveToNextLineAndShowText);
        operators.put("Tc", this::setCharSpacing);
        operators.put("Tw", this::setWordSpacing);
        operators.put("Tz", this::setHorizontalScale);
        operators.put("TL", this::setLineSpacing);
        operators.put("BT", this::beginText);
        operators.put("ET", this::endText);
        // operators.put("gs", this::??); // set text knockout?
        // operators.put("q", this::saveGraphicsState);
        // operators.put("Q", this::restoreGraphicsState);
        // operators.put("Do", this::invokeXObject);
    }

    @Override
    public List<PdfTextInfo> getResult() {
        return documentText;
    }

    @Override
    public void operator(PdfLiteral operator, List<PdfObject> operands, PdfDictionary resources) {
        String operatorName = operator.toString();
        lookupOperator(operatorName).orElse(this::unhandled).invoke(operands, resources);
    }

    private void unhandled(List<PdfObject> operands, PdfDictionary resources) {
        // PdfLiteral operator = (PdfLiteral) operands.get(operands.size() - 1);
        // System.out.print("<" + operator.toString() + ">");
    }

    protected void concatMatrix(List<PdfObject> operands, PdfDictionary resources) {
        // matrix = (AffineTransform) matrix.clone();
        matrix.preConcatenate(new AffineTransform(
                ((PdfNumber) operands.get(0)).floatValue(),
                ((PdfNumber) operands.get(1)).floatValue(),
                ((PdfNumber) operands.get(2)).floatValue(),
                ((PdfNumber) operands.get(3)).floatValue(),
                ((PdfNumber) operands.get(4)).floatValue(),
                ((PdfNumber) operands.get(5)).floatValue()
        ));
    }

    protected void setColorSpace(List<PdfObject> operands, PdfDictionary resources) {
        colorSpace = (PdfName) operands.get(0);
    }

    protected void setColor(List<PdfObject> operands, PdfDictionary resources) {
        color = ((PdfNumber) operands.get(0)).floatValue();
    }

    protected void setStrokeColorSpace(List<PdfObject> operands, PdfDictionary resources) {
        strokeColorSpace = (PdfName) operands.get(0);
    }

    protected void setStrokeColor(List<PdfObject> operands, PdfDictionary resources) {
        strokeColor = ((PdfNumber) operands.get(0)).floatValue();
    }

    protected void setGray(List<PdfObject> operands, PdfDictionary resources) {
    }

    protected void setStrokeGray(List<PdfObject> operands, PdfDictionary resources) {
    }

    protected void setTextFont(List<PdfObject> operands, PdfDictionary resources) {
        PdfName fontResourceName = (PdfName) operands.get(0);
        PdfDictionary fontsDictionary = resources.getAsDict(PdfName.FONT);
        PdfObject pdfObject = fontsDictionary.get(fontResourceName);
        fontInfo = new PdfFontInfo((PRIndirectReference) pdfObject, (PdfNumber) operands.get(1));
    }

    protected void setTextPosition(List<PdfObject> operands, PdfDictionary resources) {
        float x = ((PdfNumber) operands.get(0)).floatValue();
        float y = ((PdfNumber) operands.get(1)).floatValue();
        textState.move(x, y);
        if (textState.text != null) {
            float width = fontInfo.getWidth(textState.text);
            textState.append();
            if (y != 0 || x - width > 1f) textState.startWord();
        }
        else textState.startWord();
    }

    protected void setTextMatrix(List<PdfObject> operands, PdfDictionary resources) {
        textState.matrix = new AffineTransform(
                ((PdfNumber) operands.get(0)).floatValue(),
                ((PdfNumber) operands.get(1)).floatValue(),
                ((PdfNumber) operands.get(2)).floatValue(),
                ((PdfNumber) operands.get(3)).floatValue(),
                ((PdfNumber) operands.get(4)).floatValue(),
                ((PdfNumber) operands.get(5)).floatValue());
    }

    protected void setCharSpacing(List<PdfObject> operands, PdfDictionary resources) {
        // TODO clone state and begin new "word"
        textState.charSpacing = ((PdfNumber) operands.get(0)).floatValue();
    }

    protected void setWordSpacing(List<PdfObject> operands, PdfDictionary resources) {
        // TODO clone state and begin new "word"
        textState.wordSpacing = ((PdfNumber) operands.get(0)).floatValue();
    }

    protected void setHorizontalScale(List<PdfObject> operands, PdfDictionary resources) {
        // TODO clone state and begin new "word"
        textState.horizontalScale = ((PdfNumber) operands.get(0)).floatValue() / 100f;
    }

    protected void setLineSpacing(List<PdfObject> operands, PdfDictionary resources) {
        textState.lineSpacing = ((PdfNumber) operands.get(0)).floatValue();
    }

    protected void showText(List<PdfObject> operands, PdfDictionary resources) {
        textState.text = fontInfo.decode(operands.get(0));
    }

    protected void beginText(List<PdfObject> operands, PdfDictionary resources) {
        textState = new TextState();
    }

    protected void endText(List<PdfObject> operands, PdfDictionary resources) {
        textState.addWord();
        textState = null;
    }

    protected Optional<ContentHandler> lookupOperator(String operatorName) {
        return Optional.ofNullable(operators.get(operatorName));
    }
}
