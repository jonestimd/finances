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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.Scrollable;

import io.github.jonestimd.finance.domain.fileimport.PageRegion;
import io.github.jonestimd.swing.table.model.BeanListTableModel;
import org.apache.pdfbox.io.RandomAccessBufferedFileInputStream;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.PDFRenderer;

public class PdfPanel extends JComponent implements Scrollable {
    private static final List<Color> REGION_COLORS = Arrays.asList(Color.RED, Color.GREEN, Color.BLUE, Color.CYAN, Color.MAGENTA);
    private static final int POINTS_PER_INCH = 72;
    private static final int DEFAULT_WIDTH = (int) (8.5*POINTS_PER_INCH);
    private static final int DEFAULT_HEIGHT = 11*POINTS_PER_INCH;
    private PDDocument document;
    private PDFRenderer renderer;
    private float scale = 1f;
    private BufferedImage image;
    private final BeanListTableModel<PageRegion> regionsTableModel;

    public PdfPanel(BeanListTableModel<PageRegion> regionsTableModel) {
        this.regionsTableModel = regionsTableModel;
        regionsTableModel.addTableModelListener(event -> repaint());
        setOpaque(true);
        setSize();
    }

    public void setDocument(String fileName) {
        setDocument(new File(fileName));
    }

    public void setDocument(File fileName) {
        closeDocument();
        try (RandomAccessBufferedFileInputStream stream = new RandomAccessBufferedFileInputStream(fileName)) {
            PDFParser parser = new PDFParser(stream);
            parser.parse();
            document = parser.getPDDocument();
            renderer = new PDFRenderer(document);
            image = null;
            setSize();
            repaint();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        closeDocument();
    }

    private void closeDocument() {
        try {
            if (document != null) document.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            document = null;
            renderer = null;
        }
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        if (scale != this.scale) {
            this.scale = scale;
            image = null;
            setSize();
            repaint();
        }
    }

    private void setSize() {
        float width = DEFAULT_WIDTH;
        float height = DEFAULT_HEIGHT;
        if (document != null) {
            PDRectangle mediaBox = document.getPage(0).getMediaBox();
            width = mediaBox.getWidth();
            height = mediaBox.getHeight();
        }
        Dimension size = new Dimension((int) (width*scale), (int) (height*scale));
        setPreferredSize(size);
        setSize(size);
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 72;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 6*72;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return false;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    private void renderImage() {
        try {
            if (renderer != null) image = renderer.renderImage(0, scale);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int clip(float value, float min, float max) {
        return (int) Math.min(Math.max(value, min), max);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (image == null) renderImage();
        Graphics2D graphics = (Graphics2D) g.create();
        try {
            Dimension size = getSize();
            if (image != null) graphics.drawImage(image, 0, 0, null);
            else {
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, size.width, size.height);
            }
            graphics.setColor(Color.BLACK);
            graphics.drawRect(0, 0, size.width, size.height);
            int i = 0;
            for (PageRegion region : regionsTableModel.getBeans()) {
                graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.15f));
                i = i%REGION_COLORS.size();
                graphics.setColor(REGION_COLORS.get(i++));
                int top = clip(size.height - region.top()*scale, 0, size.height);
                int height = clip((region.top() - region.bottom())*scale, 0, size.height);
                drawRect(graphics, region.labelLeft(), region.labelRight(), top, height, size.width);
                if (region.labelLeft() != region.valueLeft() || region.labelRight() != region.valueRight()) {
                    graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));
                    drawRect(graphics, region.valueLeft(), region.valueRight(), top, height, size.width);
                }
            }
        } finally {
            graphics.dispose();
        }
    }

    private void drawRect(Graphics2D graphics, float left, float right, int top, int height, int width) {
        graphics.fillRect(clip(left*scale, 0, width), top, clip(right*scale - clip(left*scale, 0, width), 0, width), height);
    }
}
