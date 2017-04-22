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
package io.github.jonestimd.finance.swing.event;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.event.DomainEvent;
import io.github.jonestimd.finance.swing.SwingRobotTest;
import io.github.jonestimd.swing.dialog.ExceptionDialog;
import io.github.jonestimd.swing.table.model.BeanTableModel;
import io.github.jonestimd.swing.window.StatusFrame;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static io.github.jonestimd.finance.swing.BundleType.*;
import static io.github.jonestimd.finance.swing.WindowType.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ReloadEventHandlerTest extends SwingRobotTest {
    public static final String MESSAGE_KEY = "account.action.reload.status.initialize";
    private StatusFrame window;
    @Mock
    private Supplier<List<Account>> supplier;
    @Mock
    private BeanTableModel<Account> tableModel;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        SwingUtilities.invokeAndWait(() -> {
            window = new StatusFrame(LABELS.get(), ACCOUNTS.getResourcePrefix());
            window.pack();
        });
    }

    @Override
    public void cleanUp() throws Exception {
        super.cleanUp();
        if (window.isVisible()) SwingUtilities.invokeAndWait(window::dispose);
    }

    @Test
    public void doesNothingIfComponentNotShowing() throws Exception {
        ReloadEventHandler<Long, Account> handler = new ReloadEventHandler<>(new JPanel(), MESSAGE_KEY, supplier, tableModel);

        handler.onDomainEvent(new DomainEvent<>(this));

        verifyZeroInteractions(supplier, tableModel);
    }

    @Test
    public void reloadsDataOnReloadEvent() throws Exception {
        List<Account> beans = new ArrayList<>();
        when(supplier.get()).thenReturn(beans);
        ReloadEventHandler<Long, Account> handler = showWindow();

        SwingUtilities.invokeAndWait(() -> handler.onDomainEvent(new DomainEvent<>(this)));

        waitForEnableUI();
        verify(supplier).get();
        verify(tableModel).updateBeans(same(beans), any());
    }

    @Test
    public void displaysErrorFromSupplier() throws Exception {
        String message = "error loading data";
        when(supplier.get()).thenThrow(new RuntimeException(message));
        ReloadEventHandler<Long, Account> handler = showWindow();

        SwingUtilities.invokeAndWait(() -> handler.onDomainEvent(new DomainEvent<>(this)));

        waitForEnableUI();
        verify(supplier, timeout(1000)).get();
        ExceptionDialog dialog = robot.finder().findByType(ExceptionDialog.class);
        JTextArea textArea = robot.finder().findByType(dialog.getContentPane(), JTextArea.class);
        assertThat(textArea.getText()).contains(message);
        SwingUtilities.invokeAndWait(dialog::dispose);
        verifyZeroInteractions(tableModel);
    }

    @Test
    public void displaysErrorFromTableModel() throws Exception {
        String message = "error updating table";
        when(supplier.get()).thenReturn(new ArrayList<>());
        doThrow(new RuntimeException(message)).when(tableModel).updateBeans(any(), any());
        ReloadEventHandler<Long, Account> handler = showWindow();

        SwingUtilities.invokeAndWait(() -> handler.onDomainEvent(new DomainEvent<>(this)));

        verify(supplier, timeout(1000)).get();
        verify(tableModel, timeout(1000)).updateBeans(any(), any());
        ExceptionDialog dialog = robot.finder().findByType(ExceptionDialog.class);
        JTextArea textArea = robot.finder().findByType(dialog.getContentPane(), JTextArea.class);
        assertThat(textArea.getText()).contains(message);
        SwingUtilities.invokeAndWait(dialog::dispose);
        waitForEnableUI();
    }

    private ReloadEventHandler<Long, Account> showWindow() throws InterruptedException, InvocationTargetException {
        SwingUtilities.invokeAndWait(() -> window.setVisible(true));
        return new ReloadEventHandler<>(window.getContentPane(), MESSAGE_KEY, supplier, tableModel);
    }

    private void waitForEnableUI() {
        long deadline = System.currentTimeMillis() + 1000;
        while (window.getGlassPane().isVisible() && System.currentTimeMillis() < deadline) Thread.yield();
        if (window.getGlassPane().isVisible()) fail("timed out waiting for window to be enabled");
    }
}