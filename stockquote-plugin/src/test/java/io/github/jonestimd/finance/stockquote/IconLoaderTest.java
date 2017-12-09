package io.github.jonestimd.finance.stockquote;

import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;
import sun.awt.image.ToolkitImage;

import static org.assertj.core.api.Assertions.*;

public class IconLoaderTest extends HttpServerTest {
    @Test
    public void returns16x16ImageFromIcoFile() throws Exception {
        Config config = ConfigFactory.parseMap(ImmutableMap.of("iconUrl", getUrl("multi-icon.ico")));

        ImageIcon icon = new IconLoader(config).getIcon();

        assertThat(icon.getIconWidth()).isEqualTo(16);
        BufferedImage image = (BufferedImage) icon.getImage();
        assertThat(image.getRGB(0, 0)).isEqualTo(0xff0000ff);
    }

    @Test
    public void returnsScaled16x16ImageFromIcoFile() throws Exception {
        Config config = ConfigFactory.parseMap(ImmutableMap.of("iconUrl", getUrl("icon-32x32.ico")));

        ImageIcon icon = new IconLoader(config).getIcon();

        assertThat(icon.getIconWidth()).isEqualTo(16);
        ToolkitImage image = (ToolkitImage) icon.getImage();
        assertThat(image.getBufferedImage().getRGB(0, 0)).isEqualTo(0xffffff00);
    }

    @Test
    public void returnsDefaultIconForMissingIcoFile() throws Exception {
        Config config = ConfigFactory.parseMap(ImmutableMap.of("iconUrl", getUrl("icon.ico")));

        ImageIcon icon = new IconLoader(config).getIcon();

        assertThat(icon).isSameAs(IconLoader.DEFAULT_ICON);
    }

    @Test
    public void returns16x16ImageFromPngFile() throws Exception {
        Config config = ConfigFactory.parseMap(ImmutableMap.of("iconUrl", getUrl("icon-16x16.png")));

        ImageIcon icon = new IconLoader(config).getIcon();

        assertThat(icon.getIconWidth()).isEqualTo(16);
        BufferedImage image = (BufferedImage) icon.getImage();
        assertThat(image.getRGB(0, 0)).isEqualTo(0xffff00ff);
    }

    @Test
    public void returnsScaled16x16ImageFromPngFile() throws Exception {
        Config config = ConfigFactory.parseMap(ImmutableMap.of("iconUrl", getUrl("icon-32x32.png")));

        ImageIcon icon = new IconLoader(config).getIcon();

        assertThat(icon.getIconWidth()).isEqualTo(16);
        ToolkitImage image = (ToolkitImage) icon.getImage();
        assertThat(image.getBufferedImage().getRGB(0, 0)).isEqualTo(0xffffff00);
    }
}