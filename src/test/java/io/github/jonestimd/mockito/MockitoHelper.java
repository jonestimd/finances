package io.github.jonestimd.mockito;

import javax.swing.event.TableModelEvent;

import org.hamcrest.Description;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

public class MockitoHelper {
    @SuppressWarnings("deprecation")
    public static <T> ArgumentCaptor<T> captor() {
        return new ArgumentCaptor<>();
    }

    public static TableModelEvent matches(final TableModelEvent example) {
        return Mockito.argThat(new ArgumentMatcher<TableModelEvent>() {
            @Override
            public void describeTo(Description description) {
                description.appendText(example.getClass().getName())
                        .appendText("[source=").appendText(example.getSource().toString())
                        .appendText(",type=").appendValue(example.getType())
                        .appendText(",column=").appendValue(example.getColumn())
                        .appendText(",firstRow=").appendValue(example.getFirstRow())
                        .appendText(",lastRow=").appendValue(example.getLastRow())
                        .appendText("]");
            }

            @Override
            public boolean matches(Object argument) {
                TableModelEvent actual = (TableModelEvent) argument;
                return actual.getSource() == example.getSource() &&
                        actual.getType() == example.getType() &&
                        actual.getColumn() == example.getColumn() &&
                        actual.getFirstRow() == example.getFirstRow() &&
                        actual.getLastRow() == example.getLastRow();
            }
        });
    }
}
