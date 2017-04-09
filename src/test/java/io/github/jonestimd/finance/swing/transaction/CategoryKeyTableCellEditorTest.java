package io.github.jonestimd.finance.swing.transaction;

import java.util.Collections;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextField;

import io.github.jonestimd.finance.domain.transaction.TransactionCategory;
import io.github.jonestimd.finance.domain.transaction.TransactionCategorySummary;
import io.github.jonestimd.swing.table.model.BeanListTableModel;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class CategoryKeyTableCellEditorTest {
    private CategoryKeyTableCellEditor editor = new CategoryKeyTableCellEditor();
    private BeanListTableModel<TransactionCategorySummary> tableModel = new BeanListTableModel<>(Collections.emptyList());
    private JTable table = new JTable(tableModel);

    @Test
    public void initialFocus() throws Exception {
        assertThat(editor.getInitialFocus()).isEqualTo(0);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getTableCellEditor() throws Exception {
        TransactionCategory category = new TransactionCategory("code");
        tableModel.addRow(new TransactionCategorySummary(category, 1L));
        tableModel.addRow(new TransactionCategorySummary());

        JComponent component = (JComponent) editor.getTableCellEditorComponent(table, category.getKey(), true, 0, 0);

        assertThat(component.getComponentCount()).isEqualTo(2);
        assertThat(component.getComponent(0)).isInstanceOf(JComboBox.class);
        assertThat(component.getComponent(1)).isInstanceOf(JTextField.class);
        JComboBox<TransactionCategory> parentList = (JComboBox<TransactionCategory>) component.getComponent(0);
        ComboBoxModel<TransactionCategory> parentListModel = parentList.getModel();
        assertThat(parentListModel.getSize()).isEqualTo(2);
        assertThat(parentListModel.getElementAt(0)).isNull();
        assertThat(parentListModel.getElementAt(1)).isSameAs(tableModel.getBean(1).getCategory());
        JTextField codeField = (JTextField) component.getComponent(1);
        assertThat(codeField.getText()).isEqualTo("code");
    }
}