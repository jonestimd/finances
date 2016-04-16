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

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.ImageRenderInfo;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;
import com.itextpdf.text.pdf.parser.RenderListener;
import com.itextpdf.text.pdf.parser.TextRenderInfo;
import com.itextpdf.text.pdf.parser.Vector;

public class TextExtractor {
    private final List<Map<Vector, String>> pageText = new ArrayList<>();

    public TextExtractor(InputStream is) throws IOException {
        PdfReader pdfReader = new PdfReader(is);
        PdfReaderContentParser parser = new PdfReaderContentParser(pdfReader);
        int pages = pdfReader.getNumberOfPages();
        for (int i = 1; i <= pages; i++) {
            ImportRenderListener renderListener = new ImportRenderListener();
            parser.processContent(i, renderListener);
            pageText.add(renderListener.text);
        }
    }

    public Stream<Entry<Vector, String>> getText() {
        return pageText.stream().map(Map::entrySet).flatMap(Collection::stream);
    }

    private class ImportRenderListener implements RenderListener {
        private Map<Vector, String> text = new TreeMap<>(VectorComparator.TOP_DOWN_LEFT_TO_RIGHT);

        @Override
        public void beginTextBlock() {
        }

        @Override
        public void endTextBlock() {
        }

        @Override
        public void renderImage(ImageRenderInfo info) {
        }

        @Override
        public void renderText(TextRenderInfo info) {
            text.put(info.getBaseline().getStartPoint(), info.getText());
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
