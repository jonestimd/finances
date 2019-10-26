package io.github.jonestimd.finance.swing.fileimport;

import java.text.NumberFormat;

import javax.swing.JLabel;
import javax.swing.JTable;

import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class InfinityRendererTest {
    private NumberFormat format = NumberFormat.getInstance();

    @Test
    public void displaysInfinityForNull() throws Exception {
        InfinityRenderer renderer = new InfinityRenderer(Float.POSITIVE_INFINITY);

        renderer.getTableCellRendererComponent(new JTable(), null, false, false, 0, 0);

        assertThat(renderer.getText()).isEqualTo(format.format(Float.POSITIVE_INFINITY));
        assertThat(renderer.getHorizontalAlignment()).isEqualTo(JLabel.RIGHT);
    }

    @Test
    public void formatsNumber() throws Exception {
        InfinityRenderer renderer = new InfinityRenderer(Float.POSITIVE_INFINITY);

        renderer.getTableCellRendererComponent(new JTable(), 5.1f, false, false, 0, 0);

        assertThat(renderer.getText()).isEqualTo(format.format(5.1f));
    }
}