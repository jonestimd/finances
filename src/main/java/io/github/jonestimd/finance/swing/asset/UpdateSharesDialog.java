// The MIT License (MIT)
//
// Copyright (c) 2016 Tim Jones
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
package io.github.jonestimd.finance.swing.asset;

import java.awt.Dimension;
import java.awt.Window;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import javax.swing.JFormattedTextField;
import javax.swing.JTextField;

import com.google.common.base.Strings;
import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.asset.SecuritySummary;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import io.github.jonestimd.finance.swing.FormatFactory;
import io.github.jonestimd.swing.EditableComponentFocusTraversalPolicy;
import io.github.jonestimd.swing.component.BeanListComboBox;
import io.github.jonestimd.swing.component.DateField;
import io.github.jonestimd.swing.component.TextField;
import io.github.jonestimd.swing.dialog.FormDialog;
import io.github.jonestimd.swing.layout.GridBagBuilder;

import static io.github.jonestimd.finance.swing.BundleType.*;

public class UpdateSharesDialog extends FormDialog {
    private static final String RESOURCE_PREFIX = "dialog.updateShares.";
    private final BeanListComboBox<SecuritySummary> securityField;
    private final DateField dateField;
    private final JFormattedTextField currentSharesField;
    private final JTextField endingSharesField;
    private final JTextField grossAmountField;
    private final JTextField feesField;

    public UpdateSharesDialog(Window owner) {
        super(owner, LABELS.getString(RESOURCE_PREFIX + "title"), LABELS.get());
        GridBagBuilder builder = new GridBagBuilder(getFormPanel(), LABELS.get(), RESOURCE_PREFIX);
        dateField = builder.append("date.mnemonicAndName", new DateField(LABELS.getString("format.date.pattern")));
        securityField = builder.append("security.mnemonicAndName", BeanListComboBox.<SecuritySummary>builder(new SecuritySummaryFormat())
                .required(LABELS.getString(RESOURCE_PREFIX + "security.required")).get());
        currentSharesField = builder.append("currentShares.mnemonicAndName", TextField.formatted(FormatFactory.numberFormat()).readOnly().get());
        currentSharesField.setEditable(false);
        currentSharesField.setHorizontalAlignment(JTextField.RIGHT);
        endingSharesField = builder.append("endingShares.mnemonicAndName", createEndingSharesField());
        grossAmountField = builder.append("grossAmount.mnemonicAndName", numericField("grossAmount"));
        feesField = builder.append("fees.mnemonicAndName", numericField("fees"));
        securityField.addItemListener(event -> setCurrentShares(securityField.getSelectedItem()));
        setMinimumSize(new Dimension(350, 10));
        setFocusTraversalPolicy(new EditableComponentFocusTraversalPolicy());
    }

    private void setCurrentShares(SecuritySummary summary) {
        if (summary == null) {
            currentSharesField.setValue(null);
        }
        else {
            currentSharesField.setValue(summary.getShares());
        }
    }

    protected JTextField numericField(String field) {
        return TextField.validated(LABELS.get(), RESOURCE_PREFIX)
                .numeric(field + ".invalidNumber").positiveNumber(field + ".negativeNumber").configure().rightAligned().get();
    }

    protected JTextField createEndingSharesField() {
        return TextField.validated(LABELS.get(), RESOURCE_PREFIX)
                .required("endingShares.required").numeric("endingShares.invalidNumber").configure().rightAligned().get();
    }

    public boolean show(List<SecuritySummary> securitySummaries, TransactionDetail detail) {
        securityField.getModel().setElements(securitySummaries, false);
        securityField.setSelectedItem(detail.getTransaction().getSecurity());
        endingSharesField.setText("");
        grossAmountField.setText("");
        feesField.setText("");
        pack();
        setVisible(true);
        return ! isCancelled();
    }

    public Date getDate() {
        return dateField.getValue();
    }

    public Security getSecurity() {
        return securityField.getSelectedItem().getSecurity();
    }

    public BigDecimal getShares() {
        return new BigDecimal(endingSharesField.getText()).subtract(securityField.getSelectedItem().getShares());
    }

    public BigDecimal getGrossAmount() {
        return getOptionalValue(grossAmountField);
    }

    public BigDecimal getFees() {
        return getOptionalValue(feesField);
    }

    private BigDecimal getOptionalValue(JTextField field) {
        String text = field.getText();
        return Strings.isNullOrEmpty(text) ? null : new BigDecimal(text);
    }
}
