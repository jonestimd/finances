package io.github.jonestimd.finance.swing.transaction.action;

import java.awt.ComponentOrientation;
import java.awt.event.ActionEvent;

import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumnModel;

import io.github.jonestimd.finance.service.TransactionService;
import io.github.jonestimd.finance.swing.SwingRobotTest;
import io.github.jonestimd.finance.swing.transaction.TransactionTable;
import io.github.jonestimd.finance.swing.transaction.TransactionTableModel;
import org.assertj.swing.core.matcher.JButtonMatcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RefreshActionTest extends SwingRobotTest {
    @Mock
    private TransactionTable table;
    @Mock
    private TransactionTableModel model;
    @Mock
    private ListSelectionModel selectionModel;
    @Mock
    private TableColumnModel columnModel;
    @Mock
    private ListSelectionModel columnSelectionModel;
    @Mock
    private TransactionService service;

    private boolean confirmed;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        when(table.getModel()).thenReturn(model);
        when(table.getSelectionModel()).thenReturn(selectionModel);
        when(table.getColumnModel()).thenReturn(columnModel);
        when(columnModel.getSelectionModel()).thenReturn(columnSelectionModel);
    }

    @Test
    public void confirmActionReturnsTrueForNoUnsavedChanges() throws Exception {
        RefreshAction action = new RefreshAction(table, service);
        when(model.isUnsavedChanges()).thenReturn(false);

        assertThat(action.confirmAction(new ActionEvent("test", 0, "refresh"))).isTrue();
    }

    @Test
    public void confirmActionReturnsTrueWhenUserConfirms() throws Exception {
        RefreshAction action = new RefreshAction(table, service);
        when(model.isUnsavedChanges()).thenReturn(true);
        when(table.getComponentOrientation()).thenReturn(ComponentOrientation.LEFT_TO_RIGHT);

        SwingUtilities.invokeLater(() -> confirmed = action.confirmAction(new ActionEvent("test", 0, "refresh")));
        robot.click(robot.finder().find(JButtonMatcher.withText("Discard Changes")));

        assertThat(confirmed).isTrue();
    }

    @Test
    public void confirmActionReturnsFalseWhenUserCancels() throws Exception {
        RefreshAction action = new RefreshAction(table, service);
        when(model.isUnsavedChanges()).thenReturn(true);
        when(table.getComponentOrientation()).thenReturn(ComponentOrientation.LEFT_TO_RIGHT);

        SwingUtilities.invokeLater(() -> confirmed = action.confirmAction(new ActionEvent("test", 0, "refresh")));
        robot.click(robot.finder().find(JButtonMatcher.withText("Cancel").andShowing()));

        assertThat(confirmed).isFalse();
    }
}