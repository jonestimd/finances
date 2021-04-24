package io.github.jonestimd.finance.swing.asset;

import java.awt.event.KeyEvent;
import java.math.BigDecimal;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.asset.SecuritySummary;
import io.github.jonestimd.finance.domain.event.DomainEvent;
import io.github.jonestimd.finance.domain.transaction.SecurityBuilder;
import io.github.jonestimd.finance.operations.AssetOperations;
import io.github.jonestimd.finance.service.MockServiceContext;
import io.github.jonestimd.finance.service.ServiceLocator;
import io.github.jonestimd.finance.swing.FinanceTableFactory;
import io.github.jonestimd.finance.swing.SwingContext;
import io.github.jonestimd.finance.swing.SwingRobotTest;
import io.github.jonestimd.finance.swing.event.DomainEventPublisher;
import io.github.jonestimd.finance.swing.event.EventType;
import org.assertj.swing.core.GenericTypeMatcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class StockSplitDialogTest extends SwingRobotTest {
    @Mock
    private DomainEventPublisher eventPublisher;
    private final ServiceLocator serviceLocator = new MockServiceContext();
    private final AssetOperations assetOperations = serviceLocator.getAssetOperations();
    private final SwingContext swingContext = new SwingContext(serviceLocator);
    private final Security security = new SecurityBuilder().nextId().splits().get();
    private StockSplitDialog dialog;
    @Captor
    private ArgumentCaptor<DomainEvent<Long, SecuritySummary>> eventCaptor;

    private void showDialog() {
        FinanceTableFactory tableFactory = swingContext.getTableFactory();
        dialog = new StockSplitDialog(JOptionPane.getRootFrame(), tableFactory, security, assetOperations, eventPublisher);
        SwingUtilities.invokeLater(() -> {
            dialog.pack();
            dialog.setVisible(true);
        });
    }

    @Test
    public void saveCallsSaveSplits() throws Exception {
        SecuritySummary summary = new SecuritySummary(security, 1, BigDecimal.ONE);
        when(assetOperations.saveSplits(any(Security.class))).thenReturn(singletonList(summary));
        showDialog();
        robot.finder().findByType(StockSplitDialog.class).requestFocus();
        robot.pressAndReleaseKey(KeyEvent.VK_N, KeyEvent.CTRL_MASK);
        robot.enterText("\t\t2\n");

        SwingUtilities.invokeAndWait(() -> robot.finder().find(new TooltipMatcher("Save")).doClick());
        robot.close(dialog);

        verify(assetOperations).saveSplits(security);
        assertThat(security.getSplits()).hasSize(1);
        assertThat(security.getSplits().get(0).getSecurity()).isSameAs(security);
        assertThat(security.getSplits().get(0).getSharesIn()).isEqualTo(new BigDecimal(1));
        assertThat(security.getSplits().get(0).getSharesOut()).isEqualTo(new BigDecimal(2));
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getDomainObjects()).containsExactly(summary);
        assertThat(eventCaptor.getValue().getType()).isEqualTo(EventType.REPLACED);
    }

    private static class TooltipMatcher extends GenericTypeMatcher<JButton> {
        private final String tooltip;

        public TooltipMatcher(String tooltip) {
            super(JButton.class);
            this.tooltip = tooltip;
        }

        @Override
        protected boolean isMatching(JButton component) {
            String text = component.getToolTipText();
            return text != null && text.startsWith(tooltip);
        }
    }
}