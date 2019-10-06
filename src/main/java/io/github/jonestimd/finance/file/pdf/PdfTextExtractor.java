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
import java.util.Stack;

import com.lowagie.text.pdf.PRIndirectReference;
import com.lowagie.text.pdf.PdfDictionary;
import com.lowagie.text.pdf.PdfLiteral;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfNumber;
import com.lowagie.text.pdf.PdfObject;
import javafx.geometry.Point2D;
import javafx.scene.transform.Affine;

class GraphicsState implements Cloneable {
    public Affine matrix = new Affine();
    public PdfName colorSpace;
    // public float color;
    public PdfName strokeColorSpace;
    // public float strokeColor;

    public GraphicsState() {}

    public GraphicsState(GraphicsState source) {
        this.matrix = source.matrix.clone();
        this.colorSpace = source.colorSpace;
        this.strokeColorSpace = source.strokeColorSpace;
    }

    public void concatMatrix(List<PdfObject> operands) {
        matrix.prepend(new Affine(
                ((PdfNumber) operands.get(0)).floatValue(),
                ((PdfNumber) operands.get(1)).floatValue(),
                ((PdfNumber) operands.get(4)).floatValue(),
                ((PdfNumber) operands.get(2)).floatValue(),
                ((PdfNumber) operands.get(3)).floatValue(),
                ((PdfNumber) operands.get(5)).floatValue()));
    }

    public void setColorSpace(PdfName name, PdfDictionary resources) {
        colorSpace = name;
    }

    public void setStrokeColorSpace(PdfName name) {
        strokeColorSpace = name;
    }

    protected void setColor(List<PdfObject> operands) {
        // depends on current color space
    }

    protected void setStrokeColor(List<PdfObject> operands) {
        // depends on current color space
    }

    protected void setGray(PdfNumber gray) {
        // set color space to DeviceGray (or the DefaultGray color space) and set gray level
    }

    protected void setStrokeGray(PdfNumber gray) {
        // set color space to DeviceGray (or the DefaultGray color space) and set gray level
    }
}

public class PdfTextExtractor implements PdfContentHandler<List<PdfTextInfo>> {
    private class TextState {
        public Affine matrix = new Affine();
        public PdfFontInfo fontInfo;
        public float horizontalScale = 1f; // Th
        public float charSpacing; // Tc
        public float wordSpacing; // Tw
        public float lineSpacing; // Tl (leading)
        public float rise; // Trise
        public final StringBuilder word = new StringBuilder();

        public String text;
        private Point2D pos = new Point2D(0, 0);

        public void setFont(PdfName name, PdfNumber size, PdfDictionary resources) {
            PdfDictionary fontsDictionary = resources.getAsDict(PdfName.FONT);
            PRIndirectReference font = (PRIndirectReference) fontsDictionary.get(name);
            fontInfo = new PdfFontInfo(font, size);
        }

        public void setTextMatrix(List<PdfObject> operands) {
            matrix = new Affine(
                    ((PdfNumber) operands.get(0)).doubleValue(),
                    ((PdfNumber) operands.get(1)).doubleValue(),
                    ((PdfNumber) operands.get(4)).doubleValue(),
                    ((PdfNumber) operands.get(2)).doubleValue(),
                    ((PdfNumber) operands.get(3)).doubleValue(),
                    ((PdfNumber) operands.get(5)).doubleValue());
        }

        public void setTextPosition(PdfNumber pdfX, PdfNumber pdfY) {
            float x = pdfX.floatValue();
            float y = pdfY.floatValue();
            pos = pos.add(x, y);
            if (text != null) {
                float width = fontInfo.getWidth(text)*horizontalScale + text.length()*charSpacing;
                appendWord();
                if (y != 0 || x - width > 1f) startWord();
            }
            else startWord();
        }

        public void setCharSpacing(PdfNumber spacing) {
            charSpacing = spacing.floatValue();
        }

        public void setWordSpacing(PdfNumber spacing) {
            wordSpacing = spacing.floatValue();
        }

        public void setHorizontalScale(PdfNumber percent) {
            horizontalScale = percent.floatValue()/100f;
        }

        public void setLineSpacing(PdfNumber spacing) {
            lineSpacing = spacing.floatValue();
        }

        public void setTextRise(PdfNumber rise) {
            if (this.rise != rise.floatValue()) {
                startWord();
                this.rise = rise.floatValue();
            }
        }

        public void showText(PdfObject text) {
            this.text = fontInfo.decode(text);
        }

        private void startWord() {
            addWord();
            matrix.appendTranslation(pos.getX(), pos.getY());
            pos = new Point2D(0, 0);
        }

        private void appendWord() {
            word.append(text);
            text = null;
        }

        public void addWord() {
            if (text != null) appendWord();
            if (word.length() > 0) {
                Affine matrix = new Affine(this.matrix);
                matrix.prepend(graphicsState.matrix);
                Point2D start = matrix.transform(0, rise);
                documentText.add(new PdfTextInfo(fontInfo, word.toString(), start, charSpacing, horizontalScale, (float) matrix.getMyy()));
                word.setLength(0);
            }
        }
    }

    private GraphicsState graphicsState = new GraphicsState();
    private Stack<GraphicsState> graphicsStateStack = new Stack<>();
    private TextState textState;
    private PdfDictionary resources;

    private List<PdfTextInfo> documentText = new ArrayList<>();

    @Override
    public void setResources(PdfDictionary resources) {
        this.resources = resources;
    }

    @Override
    public List<PdfTextInfo> getResult() {
        return documentText;
    }

    @Override
    public void operator(PdfLiteral operator, List<PdfObject> operands) {
        String operatorName = operator.toString();
        switch (operatorName) {
            case "cm":
                graphicsState.concatMatrix(operands); break;
            case "cs":
                graphicsState.setColorSpace((PdfName) operands.get(0), resources); break;
            case "CS":
                graphicsState.setStrokeColorSpace((PdfName) operands.get(0)); break;
            case "scn":
                graphicsState.setColor(operands); break;
            case "SCN":
                graphicsState.setStrokeColor(operands); break;
            case "g":
                graphicsState.setGray((PdfNumber) operands.get(0)); break;
            case "G":
                graphicsState.setStrokeGray((PdfNumber) operands.get(0)); break;
            case "Tf":
                textState.setFont((PdfName) operands.get(0), (PdfNumber) operands.get(1), resources); break;
            case "Td":
                textState.setTextPosition((PdfNumber) operands.get(0), (PdfNumber) operands.get(1)); break;
            case "Tm":
                textState.setTextMatrix(operands); break;
            case "Ts":
                textState.setTextRise((PdfNumber) operands.get(0)); break;
            case "Tj":
                textState.showText(operands.get(0)); break;
            // case "TJ": showTextWithPos(); break; // includes individual glyph positioning
            // case "'": moveToNextLineAndShowText(); break;
            // case "\"": setSpacingMoveToNextLineAndShowText(); break;
            case "Tc":
                textState.setCharSpacing((PdfNumber) operands.get(0)); break;
            case "Tw":
                textState.setWordSpacing((PdfNumber) operands.get(0)); break;
            case "Tz":
                textState.setHorizontalScale((PdfNumber) operands.get(0)); break;
            case "TL":
                textState.setLineSpacing((PdfNumber) operands.get(0)); break;
            case "BT":
                beginText(); break;
            case "ET":
                endText(); break;
            // case "gs": ??(); break; // set text knockout?
            case "q":
                graphicsStateStack.push(new GraphicsState(graphicsState)); break;
            case "Q":
                graphicsState = graphicsStateStack.pop(); break;
            case "Do":
                invokeXObject((PdfName) operands.get(0), resources); break;
            default:
                unhandled(operatorName, operands);
        }
    }

    private void unhandled(String operator, List<PdfObject> operands) {
        // System.out.print("<" + operator + ">");
    }

    protected void invokeXObject(PdfName name, PdfDictionary resources) {
        // System.err.println(resources.getAsDict(PdfName.XOBJECT).getAsStream(name).get(PdfName.SUBTYPE));
    }

    protected void beginText() {
        textState = new TextState();
    }

    protected void endText() {
        textState.addWord();
        textState = null;
    }
}
