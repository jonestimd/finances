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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JScrollPane;

import com.lowagie.text.pdf.PdfReader;
import io.github.jonestimd.finance.domain.fileimport.PageRegion;
import io.github.jonestimd.finance.file.pdf.PdfContent;
import io.github.jonestimd.finance.file.pdf.PdfTextExtractor;
import io.github.jonestimd.finance.file.pdf.PdfTextInfo;

public class PageRegionsDialog extends JFrame {
    private final List<PdfPage> pages = new ArrayList<>();
    private int page = 0;
    private final PdfPanel canvas = new PdfPanel();

    public PageRegionsDialog(JFrame owner, List<PageRegion> regions, String previewFile) {
        // super(owner, "Page Regions", ModalityType.DOCUMENT_MODAL);
        super("Page Regions");
        setContentPane(new JScrollPane(canvas));
        try (PdfReader reader = new PdfReader(previewFile)) {
            PdfContent extractor = new PdfContent(reader);
            for (int page = 0; page < reader.getNumberOfPages(); page++) {
                List<PdfTextInfo> pageText = extractor.processPage(page, new PdfTextExtractor());
                pages.add(new PdfPage(extractor.getMediaBox(page), pageText));
            }
            Rectangle mediaBox = pages.get(page).mediaBox;
            setSize(mediaBox.width, mediaBox.height);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static class PdfPage {
        public final Rectangle mediaBox;
        public final List<PdfTextInfo> text;

        public PdfPage(Rectangle mediaBox, List<PdfTextInfo> text) {
            this.mediaBox = mediaBox;
            this.text = Collections.unmodifiableList(text);
        }
    }

    private class PdfPanel extends JComponent {
        // private Map<String, Font> fonts = new HashMap<>();

        @Override
        public Dimension getPreferredSize() {
            Rectangle mediaBox = pages.get(page).mediaBox;
            return new Dimension((int) (mediaBox.width*1.5), (int) (mediaBox.height*1.5));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (g == null) ? null : (Graphics2D) g.create();
            g2d.scale(1.5, 1.5);
            try {
                PdfPage page = pages.get(PageRegionsDialog.this.page);
                Rectangle mediaBox = page.mediaBox;
                // g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // g2d.setColor(Color.WHITE);
                // g2d.fill(mediaBox);
                g2d.setColor(Color.BLACK);
                g2d.drawRect(mediaBox.x, mediaBox.y, mediaBox.width, mediaBox.height);
                for (PdfTextInfo textInfo : page.text) {
                    Font font = Font.decode(textInfo.fontInfo.getFontName());
                    // if (!font.getFontName().equals(textInfo.fontInfo.getFontName())) {
                    //     font = Font.createFont(Font.TYPE1_FONT, textInfo.fontInfo.getFontStream());
                    //     GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
                    //     System.out.println(font.getFontName() + " " + font.getNumGlyphs());
                    // }
                    g2d.setFont(font.deriveFont(textInfo.fontInfo.getSize()*textInfo.verticalScale));
                    g2d.drawString(textInfo.text, textInfo.x, mediaBox.height - textInfo.y);
                }
            // } catch (FontFormatException | IOException e) {
            //     throw new RuntimeException(e);
            } finally {
                g2d.dispose();
            }
        }
    }

    public static void main(String[] args) {
        PageRegionsDialog dialog = new PageRegionsDialog(null, null, args[0]);
        dialog.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        dialog.setVisible(true);
    }
}
