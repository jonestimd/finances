// The MIT License (MIT)
//
// Copyright (c) 2018 Tim Jones
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
package io.github.jonestimd.finance.swing.transaction.action;

import java.awt.event.ActionEvent;

import javax.swing.JComponent;
import javax.swing.JOptionPane;

import com.google.common.base.Strings;
import io.github.jonestimd.finance.service.TransactionService;
import io.github.jonestimd.finance.swing.BundleType;
import io.github.jonestimd.finance.swing.FinanceTableFactory;
import io.github.jonestimd.finance.swing.transaction.TransactionDetailPanel;
import io.github.jonestimd.swing.BackgroundTask;
import io.github.jonestimd.swing.ComponentTreeUtils;
import io.github.jonestimd.swing.action.MnemonicAction;
import io.github.jonestimd.swing.window.StatusFrame;

public class FindAction extends MnemonicAction {
    private static final String RESOURCE_PREFIX = "action.transaction.find";
    private static final String DIALOG_TITLE = BundleType.LABELS.getString(RESOURCE_PREFIX + ".dialog.title");
    private static final String INPUT_LABEL = BundleType.LABELS.getString(RESOURCE_PREFIX + ".input.label");

    private final JComponent owner;
    private final FinanceTableFactory tableFactory;
    private final TransactionService transactionService;
    private final String loadingMessage;
    private final String noMatches;

    public FindAction(JComponent owner, FinanceTableFactory tableFactory, TransactionService transactionService) {
        super(BundleType.LABELS.get(), RESOURCE_PREFIX);
        this.owner = owner;
        this.tableFactory = tableFactory;
        this.transactionService = transactionService;
        this.loadingMessage = BundleType.LABELS.getString(RESOURCE_PREFIX + ".status");
        this.noMatches = BundleType.LABELS.getString(RESOURCE_PREFIX + ".message.noMatches");
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        StatusFrame ownerFrame = ComponentTreeUtils.findAncestor(owner, StatusFrame.class);
        String search = JOptionPane.showInputDialog(ownerFrame, INPUT_LABEL, DIALOG_TITLE, JOptionPane.PLAIN_MESSAGE);
        if (!Strings.isNullOrEmpty(search)) {
            BackgroundTask.task(loadingMessage, () -> transactionService.findAllDetails(search), (result) -> {
                if (result.isEmpty()) {
                    JOptionPane.showMessageDialog(owner, noMatches);
                }
                else {
                    StatusFrame resultFrame = new StatusFrame(BundleType.LABELS.get(), "transactionDetails");
                    resultFrame.setTitle(resultFrame.getTitle() + BundleType.LABELS.formatMessage(RESOURCE_PREFIX + ".message.matches", search));
                    resultFrame.setContentPane(new TransactionDetailPanel(tableFactory, result));
                    resultFrame.pack();
                    resultFrame.setVisible(true);
                }
            }).run(ownerFrame);
        }
    }
}
