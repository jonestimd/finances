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
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import javafx.util.Pair;
import org.apache.pdfbox.io.RandomAccessBufferedFileInputStream;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.Vector;

import static io.github.jonestimd.finance.file.pdf.VectorComparator.*;

public class TextExtractor {
    private static final Comparator<PdfTextInfo> TEXT_ORDERING = Comparator.comparing(PdfTextInfo::getPos, TOP_DOWN_LEFT_TO_RIGHT);
    private final List<List<PdfTextInfo>> pageText = new ArrayList<>();

    public TextExtractor(InputStream is) throws IOException {
        TextExtractorEngine extractor = new TextExtractorEngine();
        PDFParser parser = new PDFParser(new RandomAccessBufferedFileInputStream(is));
        parser.parse();
        PDDocument document = parser.getPDDocument();
        for (int i = 0; i < document.getNumberOfPages(); i++) {
            extractor.processPage(document.getPage(i));
            pageText.add(extractor.getDocumentText());
        }
    }

    private static Stream<Pair<Vector, String>> toPairs(List<PdfTextInfo> infos) {
        return infos.stream().sorted(TEXT_ORDERING).map(info -> new Pair<>(info.getPos(), info.getText()));
    }

    public Stream<Pair<Vector, String>> getText() {
        return pageText.stream().flatMap(TextExtractor::toPairs);
    }

    public static void main(String[] args) {
        try {
            FileInputStream stream = new FileInputStream(args[0]);
            Stream<Pair<Vector, String>> text = new TextExtractor(stream).getText();
            text.forEach(entry -> System.out.println(entry.getKey().toString() + ":" + entry.getValue()));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
