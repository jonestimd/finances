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
package io.github.jonestimd.finance.swing.transaction;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import com.google.common.base.Functions;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.transaction.CategoryKey;
import io.github.jonestimd.finance.domain.transaction.TransactionCategory;
import io.github.jonestimd.finance.domain.transaction.TransactionType;
import io.github.jonestimd.finance.operations.AccountOperations;
import io.github.jonestimd.finance.operations.TransactionCategoryOperations;
import io.github.jonestimd.finance.service.ServiceLocator;
import io.github.jonestimd.finance.swing.BundleType;
import io.github.jonestimd.finance.swing.event.ComboBoxDomainEventListener;
import io.github.jonestimd.finance.swing.event.DomainEventPublisher;
import io.github.jonestimd.swing.component.EditableComboBoxCellEditor;
import io.github.jonestimd.util.Streams;

import static io.github.jonestimd.util.JavaPredicates.*;

public class TransactionTypeCellEditor extends EditableComboBoxCellEditor<TransactionType> {
    private static final String NEXT_TYPE_ELEMENT = "next type element";
    private static final String LOADING_MESSAGE_KEY = "table.transaction.detail.type.initialize";
    private final AccountOperations accountOperations;
    private final TransactionCategoryOperations transactionCategoryOperations;
    @SuppressWarnings("FieldCanBeLocal") // need a reference to avoid garbage collection
    private final ComboBoxDomainEventListener<Long, TransactionType> domainEventListener = new ComboBoxDomainEventListener<>(getComboBox());

    public TransactionTypeCellEditor(final ServiceLocator serviceLocator, DomainEventPublisher domainEventPublisher) {
        super(new TransactionTypeFormat(), new TransactionCategoryValidator(true), BundleType.LABELS.getString(LOADING_MESSAGE_KEY));
        this.accountOperations = serviceLocator.getAccountOperations();
        this.transactionCategoryOperations = serviceLocator.getTransactionCategoryOperations();
        domainEventPublisher.register(TransactionType.class, domainEventListener);
        JComponent editorComponent = (JComponent) getComboBox().getEditor().getEditorComponent();
        editorComponent.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, KeyEvent.CTRL_DOWN_MASK), NEXT_TYPE_ELEMENT);
        editorComponent.getActionMap().put(NEXT_TYPE_ELEMENT, new NextTypeElementAction());
    }

    protected List<TransactionType> getComboBoxValues() {
        List<TransactionType> types = new ArrayList<>();
        types.addAll(accountOperations.getAllAccounts());
        types.addAll(transactionCategoryOperations.getAllTransactionCategories());
        return types;
    }

    protected Map<CategoryKey, TransactionCategory> getCategories() {
        return Streams.of(getComboBoxModel()).skip(1) // null
                .filter(not(TransactionType::isTransfer))
                .map(TransactionCategory.class::cast)
                .collect(Collectors.toMap(TransactionCategory::getKey, Functions.identity()));
    }

    protected TransactionType saveItem(TransactionType type) {
        Map<CategoryKey, TransactionCategory> categories = getCategories();
        TransactionCategory category = (TransactionCategory) type;
        while (category.getParent() != null) {
            TransactionCategory parent = categories.get(category.getParent().getKey());
            if (parent != null) {
                category.setParent(parent);
                break;
            }
            category = category.getParent();
        }
        return type;
    }

    private class NextTypeElementAction extends AbstractAction {
        private final TransactionTypeFormat format = new TransactionTypeFormat();

        @Override
        public void actionPerformed(ActionEvent e) {
            String text = ((JTextField) e.getSource()).getText();
            TransactionType type = getComboBox().getSelectedItem();
            if (format.format(type).toLowerCase().startsWith(text.toLowerCase()) && getComboBoxModel().indexOf(type) >= 0) {
                if (type instanceof TransactionCategory) {
                    TransactionCategory category = (TransactionCategory) type;
                    setEditorText((JTextField) e.getSource(), new TransactionCategory(category, ""));
                }
                else if (type instanceof Account) {
                    Account account = (Account) type;
                    if (account.getCompany() != null) {
                        setEditorText((JTextField) e.getSource(), new Account(account.getCompany(), ""));
                    }
                }
            }
            else if (! text.endsWith(CategoryKeyFormat.SEPARATOR)) {
                try {
                    TransactionCategory parent = format.parseObject(text);
                    getComboBox().setSelectedItem(new TransactionCategory(parent, ""));
                } catch (ParseException e1) {
                    // ignore
                }
            }
        }

        private void setEditorText(JTextField textField, TransactionType value) {
            textField.setText(format.format(value));
        }
    }
}