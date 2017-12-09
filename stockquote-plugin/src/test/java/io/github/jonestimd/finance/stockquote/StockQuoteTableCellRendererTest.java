package io.github.jonestimd.finance.stockquote;

import java.awt.Color;
import java.math.BigDecimal;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;

import io.github.jonestimd.finance.stockquote.StockQuote.QuoteStatus;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class StockQuoteTableCellRendererTest {
    private JTable table = new JTable();
    private StockQuoteTableCellRenderer renderer = new StockQuoteTableCellRenderer();

    @Test
    public void displaysString() throws Exception {
        renderer.getTableCellRendererComponent(table, "not a quote", false, false, 0, 0);

        assertThat(renderer.getText()).isEqualTo("not a quote");
    }

    @Test
    public void displaysPendingStatus() throws Exception {
        renderer.setIcon(new ImageIcon());

        renderer.getTableCellRendererComponent(table, new StockQuote("S1", QuoteStatus.PENDING, null, null, null, null), false, false, 0, 0);

        assertThat(renderer.getIcon()).isNull();
        assertThat(renderer.getHorizontalAlignment()).isEqualTo(JLabel.CENTER);
        assertThat(renderer.getForeground()).isEqualTo(Color.gray);
        assertThat(renderer.getText()).isEqualTo(QuoteStatus.PENDING.value);
    }

    @Test
    public void displaysNotAvailableStatus() throws Exception {
        renderer.setIcon(new ImageIcon());

        renderer.getTableCellRendererComponent(table, new StockQuote("S1", QuoteStatus.NOT_AVAILABLE, null, null, null, null), false, false, 0, 0);

        assertThat(renderer.getIcon()).isNull();
        assertThat(renderer.getHorizontalAlignment()).isEqualTo(JLabel.CENTER);
        assertThat(renderer.getForeground()).isEqualTo(Color.gray);
        assertThat(renderer.getText()).isEqualTo(QuoteStatus.NOT_AVAILABLE.value);
    }

    @Test
    public void displaysQuoteAsLink() throws Exception {
        ImageIcon sourceIcon = new ImageIcon();

        renderer.getTableCellRendererComponent(table, new StockQuote("S1", BigDecimal.TEN, "url", sourceIcon, "message"), false, false, 0, 0);

        assertThat(renderer.getIcon()).isSameAs(sourceIcon);
        assertThat(renderer.getHorizontalAlignment()).isEqualTo(JLabel.RIGHT);
        assertThat(renderer.getHorizontalTextPosition()).isEqualTo(JLabel.LEADING);
        assertThat(renderer.getText()).isEqualTo("<html><a href=\"url\">$10.00</a></html>");
        assertThat(renderer.getToolTipText()).isEqualTo("message");
    }

    @Test
    public void displaysPlainQuote() throws Exception {
        ImageIcon sourceIcon = new ImageIcon();

        renderer.getTableCellRendererComponent(table, new StockQuote("S1", BigDecimal.TEN, null, sourceIcon, "message"), false, false, 0, 0);

        assertThat(renderer.getIcon()).isSameAs(sourceIcon);
        assertThat(renderer.getHorizontalAlignment()).isEqualTo(JLabel.RIGHT);
        assertThat(renderer.getHorizontalTextPosition()).isEqualTo(JLabel.LEADING);
        assertThat(renderer.getText()).isEqualTo("$10.00");
        assertThat(renderer.getToolTipText()).isEqualTo("message");
    }
}