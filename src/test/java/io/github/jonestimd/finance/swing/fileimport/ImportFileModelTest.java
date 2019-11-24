package io.github.jonestimd.finance.swing.fileimport;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Objects;

import io.github.jonestimd.finance.domain.fileimport.FileType;
import io.github.jonestimd.finance.domain.fileimport.ImportFile;
import io.github.jonestimd.finance.domain.fileimport.ImportType;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyObject;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ImportFileModelTest {
    private ImportFile importFile = ImportFile.newImport();
    private ImportFileModel model = ImportFileModel.create(importFile);

    @Test
    public void isValid_fieldsNotLoaded() throws Exception {
        testIsValid(true, "name", ImportType.SINGLE_DETAIL_ROWS, FileType.CSV, "yyyy-mm-dd", 0);
        testIsValid(false, null, ImportType.SINGLE_DETAIL_ROWS, FileType.CSV, "yyyy-mm-dd", 0);
        testIsValid(false, "name", null, FileType.CSV, "yyyy-mm-dd", 0);
        testIsValid(false, "name", ImportType.SINGLE_DETAIL_ROWS, null, "yyyy-mm-dd", 0);
        testIsValid(false, "name", ImportType.SINGLE_DETAIL_ROWS, FileType.CSV, null, 0);
        testIsValid(false, "name", ImportType.SINGLE_DETAIL_ROWS, FileType.CSV, "yyyy-mm-dd", null);
    }

    private void testIsValid(boolean valid, String name, ImportType importType, FileType fileType, String dateFormat, Integer startOffset) {
        model.setName(name);
        model.setImportType(importType);
        model.setFileType(fileType);
        model.setDateFormat(dateFormat);
        model.setStartOffset(startOffset);

        assertThat(model.isValid()).isEqualTo(valid);
    }

    @Test
    public void resetChanges() throws Exception {
        PropertyChangeListener listener = mock(PropertyChangeListener.class);
        model.setName("name");
        model.addPropertyChangeListener(listener);

        model.resetChanges();

        assertThat(model.getName()).isEqualTo("");
        assertThat(model.getImportFieldTableModel().getBeans()).isEmpty();
        MethodHandler source = ((ProxyObject) model).getHandler();
        verify(listener).propertyChange(matches(new PropertyChangeEvent(source, "name", null, null)));
        verify(listener).propertyChange(matches(new PropertyChangeEvent(source, "changedName", null, false)));
    }

    private static PropertyChangeEvent matches(final PropertyChangeEvent example) {
        return Mockito.argThat(new ArgumentMatcher<PropertyChangeEvent>() {
            @Override
            public boolean matches(PropertyChangeEvent actual) {
                return actual.getSource() == example.getSource() &&
                        actual.getPropertyName().equals(example.getPropertyName()) &&
                        Objects.equals(actual.getOldValue(), example.getOldValue()) &&
                        Objects.equals(actual.getNewValue(), example.getNewValue());
            }

            public String toString() {
                return example.toString();
            }
        });
    }
}