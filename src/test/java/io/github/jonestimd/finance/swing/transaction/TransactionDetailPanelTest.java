package io.github.jonestimd.finance.swing.transaction;

import java.awt.Component;
import java.awt.Rectangle;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelListener;

import com.google.common.collect.Lists;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import io.github.jonestimd.finance.domain.transaction.TransactionDetailBuilder;
import io.github.jonestimd.finance.swing.FinanceTableFactory;
import io.github.jonestimd.finance.swing.WindowType;
import io.github.jonestimd.swing.ComponentTreeUtils;
import io.github.jonestimd.swing.table.DecoratedTable;
import io.github.jonestimd.swing.table.TableInitializer;
import io.github.jonestimd.swing.window.FrameManager;
import io.github.jonestimd.swing.window.StatusFrame;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static io.github.jonestimd.finance.swing.BundleType.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TransactionDetailPanelTest {
    private FinanceTableFactory tableFactory;
    @Mock
    private TableInitializer tableInitializer;
    @Mock
    private FrameManager<WindowType> frameManager;
    @Mock
    private StatusFrame transactionsWindow;
    @Mock
    private JPanel contentPane;
    @Mock
    private TransactionTable transactionTable;
    @Mock
    private TransactionTableModel transactionsModel;
    @Captor
    private ArgumentCaptor<TableModelListener> listenerCaptor;

    private final JFrame frame = new JFrame();

    @Before
    public void setupMocks() throws Exception {
        when(tableInitializer.initialize(any())).thenAnswer(invocation -> invocation.getArguments()[0]);
        tableFactory = spy(new FinanceTableFactory(tableInitializer));
    }

    @After
    public void disposeFrame() {
        frame.dispose();
    }

    private void showFrame(TransactionDetailPanel panel) throws InterruptedException, InvocationTargetException {
        SwingUtilities.invokeAndWait(() -> {
            frame.setContentPane(panel);
            frame.setVisible(true);
        });
    }

    @Test
    public void addsGotoActionToMenu() throws Exception {
        TransactionDetail detail = new TransactionDetailBuilder().get();
        TransactionDetailPanel panel = new TransactionDetailPanel(tableFactory, Lists.newArrayList(detail), frameManager);
        ComponentTreeUtils.findComponent(panel, DecoratedTable.class).clearSelection();

        showFrame(panel);

        JMenu menu = frame.getJMenuBar().getMenu(0);
        assertThat(menu.getText()).isEqualTo(LABELS.getString("menu.transactions.mnemonicAndName").substring(1));
        assertThat(menu.getItem(0).getText()).isEqualTo(LABELS.getString("action.transaction.goto.mnemonicAndName").substring(1));
        assertThat(menu.getItem(0).getAction().isEnabled()).isFalse();
        JToolBar toolBar = (JToolBar) frame.getJMenuBar().getComponent(2);
        assertThat(((JButton) toolBar.getComponent(0)).getAction()).isSameAs(menu.getItem(0).getAction());
    }

    @Test
    public void enablesGotoActionForSingleSelectedRow() throws Exception {
        TransactionDetail detail = new TransactionDetailBuilder().get();
        TransactionDetailPanel panel = new TransactionDetailPanel(tableFactory, Lists.newArrayList(detail), frameManager);
        ComponentTreeUtils.findComponent(panel, DecoratedTable.class).setRowSelectionInterval(0, 0);

        showFrame(panel);

        JMenu menu = frame.getJMenuBar().getMenu(0);
        assertThat(menu.getItem(0).getAction().isEnabled()).isTrue();
    }

    @Test
    public void disablesGotoActionForMultipleSelectedRows() throws Exception {
        TransactionDetail detail1 = new TransactionDetailBuilder().get();
        TransactionDetail detail2 = new TransactionDetailBuilder().get();
        TransactionDetailPanel panel = new TransactionDetailPanel(tableFactory, Lists.newArrayList(detail1, detail2), frameManager);
        ComponentTreeUtils.findComponent(panel, DecoratedTable.class).setRowSelectionInterval(0, 1);

        showFrame(panel);

        JMenu menu = frame.getJMenuBar().getMenu(0);
        assertThat(menu.getItem(0).getAction().isEnabled()).isFalse();
    }

    @Test
    public void gotoActionSelectsTransactionWindow() throws Exception {
        final int beanIndex = 1;
        final int transactionModelIndex = 2;
        final int transactionViewIndex = 3;
        final Rectangle cellRect = new Rectangle();
        TransactionDetail detail = new TransactionDetailBuilder().onTransaction().get();
        when(transactionsModel.getBeans()).thenReturn(Lists.newArrayList(detail.getTransaction()));
        when(transactionsModel.indexOf(any(Transaction.class))).thenReturn(beanIndex);
        when(transactionsModel.getLeadRowForGroup(anyInt())).thenReturn(transactionModelIndex);
        when(transactionTable.getModel()).thenReturn(transactionsModel);
        when(transactionTable.convertRowIndexToView(anyInt())).thenReturn(transactionViewIndex);
        when(transactionTable.getCellRect(anyInt(), anyInt(), anyBoolean())).thenReturn(cellRect);
        when(contentPane.getComponents()).thenReturn(new Component[]{transactionTable});
        when(transactionsWindow.getContentPane()).thenReturn(contentPane);
        when(frameManager.showFrame(any())).thenReturn(transactionsWindow);
        TransactionDetailPanel panel = new TransactionDetailPanel(tableFactory, Lists.newArrayList(detail), frameManager);
        ComponentTreeUtils.findComponent(panel, DecoratedTable.class).setRowSelectionInterval(0, 0);
        showFrame(panel);

        frame.getJMenuBar().getMenu(0).getItem(0).getAction().actionPerformed(null);

        verify(transactionsModel).indexOf(detail.getTransaction());
        verify(transactionTable).convertRowIndexToView(transactionModelIndex);
        verify(transactionTable).getCellRect(transactionViewIndex, 0, true);
        verify(transactionTable).scrollRectToVisible(cellRect);
    }

    @Test
    public void gotoActionWaitsForTransactionsToLoad() throws Exception {
        final int beanIndex = 1;
        final int transactionModelIndex = 2;
        final int transactionViewIndex = 3;
        final Rectangle cellRect = new Rectangle();
        TransactionDetail detail = new TransactionDetailBuilder().onTransaction().get();
        when(transactionsModel.getBeans()).thenReturn(Collections.emptyList());
        when(transactionsModel.indexOf(any(Transaction.class))).thenReturn(beanIndex);
        when(transactionsModel.getLeadRowForGroup(anyInt())).thenReturn(transactionModelIndex);
        when(transactionTable.getModel()).thenReturn(transactionsModel);
        when(transactionTable.convertRowIndexToView(anyInt())).thenReturn(transactionViewIndex);
        when(transactionTable.getCellRect(anyInt(), anyInt(), anyBoolean())).thenReturn(cellRect);
        when(contentPane.getComponents()).thenReturn(new Component[]{transactionTable});
        when(transactionsWindow.getContentPane()).thenReturn(contentPane);
        when(frameManager.showFrame(any())).thenReturn(transactionsWindow);
        TransactionDetailPanel panel = new TransactionDetailPanel(tableFactory, Lists.newArrayList(detail), frameManager);
        ComponentTreeUtils.findComponent(panel, DecoratedTable.class).setRowSelectionInterval(0, 0);
        showFrame(panel);
        frame.getJMenuBar().getMenu(0).getItem(0).getAction().actionPerformed(null);
        InOrder inOrder = inOrder(transactionsModel, transactionTable);

        inOrder.verify(transactionsModel).addTableModelListener(listenerCaptor.capture());
        listenerCaptor.getValue().tableChanged(null);

        SwingUtilities.invokeAndWait(() -> {});
        inOrder.verify(transactionsModel).removeTableModelListener(listenerCaptor.getValue());
        inOrder.verify(transactionsModel).indexOf(detail.getTransaction());
        inOrder.verify(transactionTable).convertRowIndexToView(transactionModelIndex);
        inOrder.verify(transactionTable).getCellRect(transactionViewIndex, 0, true);
        inOrder.verify(transactionTable).scrollRectToVisible(cellRect);
    }
}