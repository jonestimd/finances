package io.github.jonestimd.finance.swing.transaction.action;

import java.awt.event.ActionEvent;
import java.util.Collections;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import io.github.jonestimd.finance.domain.transaction.TransactionDetailBuilder;
import io.github.jonestimd.finance.service.TransactionService;
import io.github.jonestimd.finance.swing.BundleType;
import io.github.jonestimd.finance.swing.FinanceTableFactory;
import io.github.jonestimd.finance.swing.SwingRobotTest;
import io.github.jonestimd.finance.swing.WindowType;
import io.github.jonestimd.finance.swing.transaction.TransactionDetailTableModel;
import io.github.jonestimd.swing.table.TableInitializer;
import io.github.jonestimd.swing.table.model.BeanListTableModel;
import io.github.jonestimd.swing.window.FrameManager;
import io.github.jonestimd.swing.window.StatusFrame;
import org.assertj.swing.finder.JOptionPaneFinder;
import org.assertj.swing.finder.WindowFinder;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JOptionPaneFixture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FindActionTest extends SwingRobotTest {
    private final StatusFrame frame = new StatusFrame(BundleType.LABELS.get(), "window.transactions");
    private final JPanel frameContent = new JPanel();
    @Mock
    private TransactionService transactionService;
    @Mock
    private TableInitializer tableInitializer;
    @Mock
    private FrameManager<WindowType> frameManager;

    private FinanceTableFactory tableFactory;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        frame.setContentPane(frameContent);
        when(tableInitializer.initialize(any())).thenAnswer(invocation -> invocation.getArguments()[0]);
        tableFactory = spy(new FinanceTableFactory(tableInitializer));
    }

    @Test
    public void doesNotCallServiceWhenCancelled() throws Exception {
        FindAction action = new FindAction(frameContent, tableFactory, transactionService, frameManager);

        SwingUtilities.invokeLater(() -> action.actionPerformed(new ActionEvent(frameContent, -1, "x")));
        JOptionPaneFixture optionPane = JOptionPaneFinder.findOptionPane().using(robot);
        optionPane.textBox().setText("search");
        optionPane.buttonWithText("Cancel").click();

        robot.waitForIdle();
        verifyNoInteractions(transactionService, tableFactory);
    }

    @Test
    public void displaysMessageForNoMatches() throws Exception {
        when(transactionService.findAllDetails("search")).thenReturn(Collections.emptyList());
        FindAction action = new FindAction(frameContent, tableFactory, transactionService, frameManager);

        SwingUtilities.invokeLater(() -> action.actionPerformed(new ActionEvent(frameContent, -1, "x")));
        JOptionPaneFixture optionPane = JOptionPaneFinder.findOptionPane().using(robot);
        optionPane.textBox().setText("search");
        optionPane.buttonWithText("OK").click();

        robot.waitForIdle();
        JOptionPaneFixture messagePane = JOptionPaneFinder.findOptionPane().using(robot);
        messagePane.requireMessage("No matches found");
        verify(transactionService).findAllDetails("search");
        verifyNoInteractions(tableFactory);
    }

    @Test
    public void displaysResults() throws Exception {
        TransactionDetail detail = new TransactionDetailBuilder().get();
        when(transactionService.findAllDetails("search")).thenReturn(Collections.singletonList(detail));
        FindAction action = new FindAction(frameContent, tableFactory, transactionService, frameManager);

        SwingUtilities.invokeLater(() -> action.actionPerformed(new ActionEvent(frameContent, -1, "x")));
        JOptionPaneFixture optionPane = JOptionPaneFinder.findOptionPane().using(robot);
        optionPane.textBox().setText("search");
        optionPane.buttonWithText("OK").click();

        robot.waitForIdle();
        FrameFixture frame = WindowFinder.findFrame(StatusFrame.class).using(robot);
        frame.requireTitle("Transaction Details matching \"search\"");
        verify(transactionService).findAllDetails("search");
        ArgumentCaptor<BeanListTableModel> modelCaptor = ArgumentCaptor.forClass(BeanListTableModel.class);
        verify(tableFactory).tableBuilder(modelCaptor.capture());
        assertThat(modelCaptor.getValue()).isInstanceOf(TransactionDetailTableModel.class);
    }
}