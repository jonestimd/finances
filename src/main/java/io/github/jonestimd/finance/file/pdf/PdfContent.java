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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;

import com.lowagie.text.ExceptionConverter;
import com.lowagie.text.pdf.PRIndirectReference;
import com.lowagie.text.pdf.PRStream;
import com.lowagie.text.pdf.PRTokeniser;
import com.lowagie.text.pdf.PdfArray;
import com.lowagie.text.pdf.PdfContentParser;
import com.lowagie.text.pdf.PdfDictionary;
import com.lowagie.text.pdf.PdfLiteral;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfObject;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.RandomAccessFileOrArray;

public class PdfContent {
    private final PdfReader reader;

    public PdfContent(PdfReader reader) {
        this.reader = reader;
    }

    public <T> T processPage(int page, PdfContentHandler<T> handler) throws IOException {
        PdfDictionary pageDict = reader.getPageN(page);
        if (pageDict != null) {
            System.out.println(pageDict.getKeys());
            System.out.println(pageDict.get(PdfName.MEDIABOX)); // boundry in 1/72 inch units
            PdfDictionary resources = pageDict.getAsDict(PdfName.RESOURCES);
            processContent(getContentBytesForPage(page), resources, handler);
        }
        return handler.getResult();
    }

    protected void processContent(byte[] contentBytes, PdfDictionary resources, PdfContentHandler<?> handler) {
        try {
            PdfContentParser ps = new PdfContentParser(new PRTokeniser(contentBytes));
            List<PdfObject> operands = new ArrayList<>();
            while (ps.parse(operands).size() > 0) {
                PdfLiteral operator = (PdfLiteral) operands.get(operands.size() - 1);
                handler.operator(operator, operands, resources);
            }
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    private byte[] getContentBytesForPage(int pageNum) throws IOException {
        try (RandomAccessFileOrArray ignored = reader.getSafeFile()) {
            PdfDictionary pageDictionary = reader.getPageN(pageNum);
            PdfObject contentObject = pageDictionary.get(PdfName.CONTENTS);
            return getContentBytesFromContentObject(contentObject);
        }
    }

    private byte[] getContentBytesFromContentObject(PdfObject contentObject) throws IOException {
        final byte[] result;
        switch (contentObject.type()) {
            case PdfObject.INDIRECT:
                PRIndirectReference ref = (PRIndirectReference) contentObject;
                PdfObject directObject = PdfReader.getPdfObject(ref);
                result = getContentBytesFromContentObject(directObject);
                break;
            case PdfObject.STREAM:
                PRStream stream = (PRStream) PdfReader.getPdfObject(contentObject);
                result = PdfReader.getStreamBytes(stream);
                break;
            case PdfObject.ARRAY:
                ByteArrayOutputStream allBytes = new ByteArrayOutputStream();
                PdfArray contentArray = (PdfArray) contentObject;
                ListIterator<PdfObject> iter = contentArray.listIterator();
                while (iter.hasNext()) {
                    PdfObject element = iter.next();
                    allBytes.write(getContentBytesFromContentObject(element));
                }
                result = allBytes.toByteArray();
                break;
            default:
                throw new IllegalStateException("Unable to handle Content of type " + contentObject.getClass());
        }
        return result;
    }

    public static void main(String[] args) {
        try (PdfReader reader = new PdfReader(args[0])) {
            PdfContent extractor = new PdfContent(reader);
            for (int page = 0; page < reader.getNumberOfPages(); page++) {
                System.out.println("==== page " + (page + 1));
                List<PdfTextInfo> pageText = extractor.processPage(page + 1, new PdfTextExtractor());
                float y = Float.NaN;
                for (PdfTextInfo info : pageText) {
                    if (y != info.y) System.out.println(y);
                    System.out.print(info.text + " ");
                    y = info.y;
                }
                // String text = pageText.stream().map(info -> info.text).collect(Collectors.joining(" "));
                // System.out.println(text);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
