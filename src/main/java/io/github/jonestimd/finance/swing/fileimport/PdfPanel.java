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
import io.github.jonestimd.swing.table.DecoratedTable;
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
    private static final float OUTLINE_ALPHA = 0.5f;
    private static final float LABEL_ALPHA = 0.15f;
    private static final float VALUE_ALPHA = 0.25f;
    private PDDocument document;
    private PDFRenderer renderer;
    private float scale = 1f;
    private BufferedImage image;
    private final DecoratedTable<PageRegion, PageRegionTableModel> regionsTable;

    public PdfPanel(DecoratedTable<PageRegion, PageRegionTableModel> regionsTable) {
        this.regionsTable = regionsTable;
        regionsTable.getModel().addTableModelListener(event -> repaint());
        regionsTable.getSelectionModel().addListSelectionListener(event -> repaint());

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
            for (PageRegion region : regionsTable.getModel().getBeans()) {
                int top = clip(size.height - region.top()*scale, 0, size.height);
                int height = clip((region.top() - region.bottom())*scale, 0, size.height);
                boolean selected = regionsTable.getSelectedItems().contains(region);
                drawRegion(graphics, top, size.width, height, region.labelLeft(), region.labelRight(), selected, i, LABEL_ALPHA);
                if (region.labelLeft() != region.valueLeft() || region.labelRight() != region.valueRight()) {
                    drawRegion(graphics, top, size.width, height, region.valueLeft(), region.valueRight(), selected, i, VALUE_ALPHA);
                }
                i = (i + 1)%REGION_COLORS.size();
            }
        } finally {
            graphics.dispose();
        }
    }

    private void drawRegion(Graphics2D graphics, int top, int width, int height, float left, float right, boolean selected, int color, float alpha) {
        graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        graphics.setColor(REGION_COLORS.get(color));
        graphics.fillRect(clip(left*scale, 0, width), top, clip(right*scale - clip(left*scale, 0, width), 0, width), height);
        if (selected) {
            graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, OUTLINE_ALPHA));
            graphics.setColor(Color.BLACK);
            graphics.drawRect(clip(left*scale, 0, width), top, clip(right*scale - clip(left*scale, 0, width), 0, width), height);
        }
    }
}
