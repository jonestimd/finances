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
package io.github.jonestimd.finance.swing.fileimport;

import java.util.List;

import javax.swing.JFrame;
import javax.swing.JScrollPane;

import io.github.jonestimd.finance.domain.fileimport.PageRegion;
import org.apache.pdfbox.io.RandomAccessBufferedFileInputStream;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

public class PageRegionsDialog extends JFrame {
    private int page = 0;
    private final PDDocument pdfDocument;
    private final PdfPanel pdfPanel;

    public PageRegionsDialog(JFrame owner, List<PageRegion> regions, String previewFile) {
        // super(owner, "Page Regions", ModalityType.DOCUMENT_MODAL);
        super("Page Regions");
        try (RandomAccessBufferedFileInputStream reader = new RandomAccessBufferedFileInputStream(previewFile)) {
            PDFParser parser = new PDFParser(reader);
            parser.parse();
            this.pdfDocument = parser.getPDDocument();
            this.pdfPanel = new PdfPanel(pdfDocument);
            setContentPane(new JScrollPane(pdfPanel));
            PDRectangle mediaBox = pdfDocument.getPage(page).getMediaBox();
            setSize((int) mediaBox.getWidth(), (int) mediaBox.getHeight());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void main(String[] args) {
        PageRegionsDialog dialog = new PageRegionsDialog(null, null, args[0]);
        dialog.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        dialog.setVisible(true);
    }
}
