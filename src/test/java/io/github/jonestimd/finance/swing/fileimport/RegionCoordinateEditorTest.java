package io.github.jonestimd.finance.swing.fileimport;

import javax.swing.JTable;

import io.github.jonestimd.swing.validation.ValidatedTextField;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class RegionCoordinateEditorTest {
    @Test
    public void acceptsEmptyValue() throws Exception {
        RegionCoordinateEditor editor = new RegionCoordinateEditor(new JTable());
        ValidatedTextField textField = (ValidatedTextField) editor.getComponent();

        textField.setText("");

        assertThat(textField.getValidationMessages()).isNull();
        assertThat(editor.stopCellEditing()).isTrue();
    }

    @Test
    public void requiresNumericValue() throws Exception {
        RegionCoordinateEditor editor = new RegionCoordinateEditor(new JTable());
        ValidatedTextField textField = (ValidatedTextField) editor.getComponent();

        textField.setText("a");

        assertThat(textField.getValidationMessages()).isEqualTo("Invalid number");
        assertThat(editor.stopCellEditing()).isFalse();
    }

    @Test
    public void convertsEmptyStringToNull() throws Exception {
        RegionCoordinateEditor editor = new RegionCoordinateEditor(new JTable());
        ValidatedTextField textField = (ValidatedTextField) editor.getComponent();

        textField.setText("");

        assertThat(editor.getCellEditorValue()).isNull();
    }

    @Test
    public void convertsStringToFloat() throws Exception {
        RegionCoordinateEditor editor = new RegionCoordinateEditor(new JTable());
        ValidatedTextField textField = (ValidatedTextField) editor.getComponent();

        textField.setText("123");

        assertThat(editor.getCellEditorValue()).isEqualTo(Float.valueOf("123"));
    }
}