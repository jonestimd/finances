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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.jonestimd.function.FailableConsumer;
import org.apache.pdfbox.contentstream.PDFStreamEngine;
import org.apache.pdfbox.contentstream.operator.state.Concatenate;
import org.apache.pdfbox.contentstream.operator.state.Restore;
import org.apache.pdfbox.contentstream.operator.state.Save;
import org.apache.pdfbox.contentstream.operator.state.SetMatrix;
import org.apache.pdfbox.contentstream.operator.text.BeginText;
import org.apache.pdfbox.contentstream.operator.text.EndText;
import org.apache.pdfbox.contentstream.operator.text.MoveText;
import org.apache.pdfbox.contentstream.operator.text.SetCharSpacing;
import org.apache.pdfbox.contentstream.operator.text.SetFontAndSize;
import org.apache.pdfbox.contentstream.operator.text.SetTextHorizontalScaling;
import org.apache.pdfbox.contentstream.operator.text.SetTextLeading;
import org.apache.pdfbox.contentstream.operator.text.SetTextRise;
import org.apache.pdfbox.contentstream.operator.text.SetWordSpacing;
import org.apache.pdfbox.contentstream.operator.text.ShowText;
import org.apache.pdfbox.contentstream.operator.text.ShowTextAdjusted;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.io.RandomAccessBufferedFileInputStream;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.state.PDTextState;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.Vector;

public class TextExtractorEngine extends PDFStreamEngine {
    private final List<PdfTextInfo> documentText = new ArrayList<>();
    private final StringBuilder wordBuffer = new StringBuilder();
    private Vector pos;

    public TextExtractorEngine() {
        addOperator(new Concatenate());
        addOperator(new SetFontAndSize());
        addOperator(new MoveText());
        addOperator(new SetMatrix());
        addOperator(new SetTextRise());
        addOperator(new ShowText());
        addOperator(new ShowTextAdjusted());
        // addOperator(new ShowTextLine());
        // addOperator(new ShowTextLineAndSpace());
        addOperator(new SetCharSpacing());
        addOperator(new SetWordSpacing());
        addOperator(new SetTextHorizontalScaling());
        addOperator(new SetTextLeading());
        addOperator(new BeginText());
        addOperator(new EndText());
        addOperator(new Save());
        addOperator(new Restore());
    }

    public List<PdfTextInfo> getDocumentText() {
        return Collections.unmodifiableList(documentText);
    }

    @Override
    public void processPage(PDPage page) throws IOException {
        this.documentText.clear();
        super.processPage(page);
    }

    @Override
    public void showTextString(byte[] string) throws IOException {
        captureString(string, super::showTextString);
    }

    @Override
    public void showTextStrings(COSArray array) throws IOException {
        captureString(array, super::showTextStrings);
    }

    private <T> void captureString(T arg, FailableConsumer<T, IOException> superImpl) throws IOException {
        PDTextState textState = getGraphicsState().getTextState();
        PDFont font = textState.getFont();
        float fontSize = textState.getFontSize();
        float charSpacing = textState.getCharacterSpacing();
        float horizontalScale = textState.getHorizontalScaling();
        wordBuffer.setLength(0);
        pos = null;
        superImpl.accept(arg);
        if (pos != null && wordBuffer.length() > 0) {
            documentText.add(new PdfTextInfo(font, fontSize, wordBuffer.toString(), pos, charSpacing, horizontalScale));
        }
    }

    @Override
    protected void showGlyph(Matrix textRenderingMatrix, PDFont font, int code, String unicode, Vector displacement) throws IOException {
        if (wordBuffer.length() > 0 || unicode.trim().length() > 0) {
            if (pos == null) pos = textRenderingMatrix.transform(new Vector(0, 0));
            wordBuffer.append(unicode);
        }
        super.showGlyph(textRenderingMatrix, font, code, unicode, displacement);
    }

    public static void main(String[] args) {
        try {
            TextExtractorEngine extractor = new TextExtractorEngine();
            PDFParser parser = new PDFParser(new RandomAccessBufferedFileInputStream(args[0]));
            parser.parse();
            extractor.processPage(parser.getPDDocument().getPage(0));
            for (PdfTextInfo entry : extractor.getDocumentText()) {
                System.out.println(entry.x + ", " + entry.y + ": " + entry.text);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
