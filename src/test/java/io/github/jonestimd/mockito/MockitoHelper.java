package io.github.jonestimd.mockito;

import javax.swing.event.TableModelEvent;

import org.mockito.Mockito;

public class MockitoHelper {
    public static TableModelEvent matches(final TableModelEvent example) {
        return Mockito.argThat(actual -> actual.getSource() == example.getSource() &&
                actual.getType() == example.getType() &&
                actual.getColumn() == example.getColumn() &&
                actual.getFirstRow() == example.getFirstRow() &&
                actual.getLastRow() == example.getLastRow());
    }
}
