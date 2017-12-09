// The MIT License (MIT)
//
// Copyright (c) 2017 Tim Jones
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
package io.github.jonestimd.finance.stockquote;

import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import com.google.common.collect.ImmutableList;
import com.typesafe.config.Config;
import net.sf.image4j.codec.ico.ICODecoder;

import static io.github.jonestimd.finance.stockquote.StockQuotePlugin.*;

/**
 * Loads icons from URLs.  Used to get icons for stock quote service providers.  If a icon is larger than 16x16
 * then a scaled version will be returned.
 */
public class IconLoader {
    protected static final String ICON_URL_KEY = "iconUrl";
    protected static final ImageIcon DEFAULT_ICON = new ImageIcon(IconLoader.class.getResource(BUNDLE.getString("table.security.price.defaultIcon")));
    private static final List<Integer> FAILED_STATUS = ImmutableList.of(MediaTracker.ABORTED, MediaTracker.ERRORED);
    private final Config config;

    /**
     * Create an icon loader.
     * @param config must contain a {@code iconUrl} entry for the icon URL
     */
    public IconLoader(Config config) {
        this.config = config;
    }

    /**
     * Get the icon from the configured URL.
     * @return the icon at the configured URL or a blank icon.
     */
    public ImageIcon getIcon() {
        ImageIcon icon = config.hasPath(ICON_URL_KEY) ? getIcon(config.getString(ICON_URL_KEY)) : null;
        return isFailed(icon) ? DEFAULT_ICON : icon;
    }

    private ImageIcon getIcon(String url) {
        try {
            return url.endsWith(".ico") ? decodeIcon(url) : loadImage(url);
        } catch (Exception ex) {
            return null;
        }
    }

    private ImageIcon decodeIcon(String url) throws IOException {
        try (InputStream stream = new URL(url).openStream()) {
            List<BufferedImage> images = ICODecoder.read(stream);
            for (BufferedImage image : images) {
                if (image.getWidth() == 16) return new ImageIcon(image);
            }
            return scaleImage(images.get(0));
        }
    }

    private ImageIcon loadImage(String url) throws IOException {
        BufferedImage bufferedImage = ImageIO.read(new URL(url));
        if (bufferedImage.getWidth() <= 16) return new ImageIcon(bufferedImage);
        return scaleImage(bufferedImage);
    }

    private ImageIcon scaleImage(BufferedImage bufferedImage) {
        return new ImageIcon(bufferedImage.getScaledInstance(16, -1, Image.SCALE_SMOOTH));
    }

    private boolean isFailed(ImageIcon icon) {
        return icon == null || FAILED_STATUS.contains(icon.getImageLoadStatus());
    }
}
