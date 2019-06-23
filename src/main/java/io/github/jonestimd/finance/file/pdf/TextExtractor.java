// The MIT License (MIT)
//
// Copyright (c) 2016 Tim Jones
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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Stream;

import com.google.common.collect.Maps;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.FinalText;
import com.lowagie.text.pdf.parser.ParsedText;
import com.lowagie.text.pdf.parser.ParsedTextImpl;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.lowagie.text.pdf.parser.TextAssembler;
import com.lowagie.text.pdf.parser.Vector;
import com.lowagie.text.pdf.parser.Word;

import static io.github.jonestimd.finance.file.pdf.VectorComparator.*;

public class TextExtractor {
    private final List<Map<Vector, String>> pageText = new ArrayList<>();

    public TextExtractor(InputStream is) throws IOException {
        PdfReader pdfReader = new PdfReader(is);
        int pages = pdfReader.getNumberOfPages();
        for (int i = 1; i <= pages; i++) {
            ImportRenderListener renderListener = new ImportRenderListener();
            PdfTextExtractor extractor = new PdfTextExtractor(pdfReader, renderListener);
            extractor.getTextFromPage(i);
            pageText.add(Maps.transformValues(renderListener.textByPosition, StringBuilder::toString));
        }
    }

    public Stream<Entry<Vector, String>> getText() {
        return pageText.stream().map(Map::entrySet).flatMap(Collection::stream);
    }

    private class ImportRenderListener implements TextAssembler {
        private Map<Vector, StringBuilder> textByPosition = new TreeMap<>(VectorComparator.TOP_DOWN_LEFT_TO_RIGHT);
        private Vector pos;
        private Vector lastEnd;
        private StringBuilder buffer;

        @Override
        public void process(FinalText completed, String contextName) {
        }

        @Override
        public void process(Word completed, String contextName) {
        }

        @Override
        public void process(ParsedText parsed, String contextName) {
            String text = parsed.getText();
            if (text.trim().length() > 0) {
                if (pos == null || !isSameLine(parsed) || !isSameWord(parsed)) {
                    buffer = new StringBuilder();
                    pos = parsed.getStartPoint();
                    textByPosition.put(pos, buffer);
                }
                lastEnd = parsed.getEndPoint();
                buffer.append(parsed.getText());
            }
        }

        private boolean isSameLine(ParsedText parsed) {
            return pos.get(Y_INDEX) == parsed.getStartPoint().get(Y_INDEX);
        }

        private boolean isSameWord(ParsedText parsed) {
            return Math.abs(parsed.getStartPoint().get(X_INDEX) - lastEnd.get(X_INDEX)) < parsed.getSingleSpaceWidth()/2f;
        }

        @Override
        public void renderText(FinalText completed) {
        }

        @Override
        public void renderText(ParsedTextImpl parsed) {
            System.out.println(parsed);
        }

        @Override
        public FinalText endParsingContext(String containingElementName) {
            return null;
        }

        @Override
        public String getWordId() {
            return null;
        }

        @Override
        public void setPage(int page) {

        }

        @Override
        public void reset() {
        }
    }

    public static void main(String[] args) {
        try {
            FileInputStream stream = new FileInputStream(args[0]);
            Stream<Entry<Vector, String>> text = new TextExtractor(stream).getText();
            text.forEach(entry -> System.out.println(entry.getKey().toString() + ":" + entry.getValue()));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
