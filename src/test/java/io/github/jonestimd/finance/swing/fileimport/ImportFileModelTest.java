package io.github.jonestimd.finance.swing.fileimport;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import io.github.jonestimd.finance.domain.fileimport.FileType;
import io.github.jonestimd.finance.domain.fileimport.ImportFile;
import io.github.jonestimd.finance.domain.fileimport.ImportType;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyObject;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ImportFileModelTest {
    private ImportFile importFile = ImportFile.newImport();
    private ImportFileModel model = ImportFileModel.create(importFile);

    @Test
    public void isValid_fieldsNotLoaded() throws Exception {
        testIsValid(true, "name", ImportType.SINGLE_DETAIL_ROWS, FileType.CSV, "yyyy-mm-dd", 0, singletonList("date"));
        testIsValid(false, null, ImportType.SINGLE_DETAIL_ROWS, FileType.CSV, "yyyy-mm-dd", 0, singletonList("date"));
        testIsValid(false, "name", null, FileType.CSV, "yyyy-mm-dd", 0, singletonList("date"));
        testIsValid(false, "name", ImportType.SINGLE_DETAIL_ROWS, null, "yyyy-mm-dd", 0, singletonList("date"));
        testIsValid(false, "name", ImportType.SINGLE_DETAIL_ROWS, FileType.CSV, null, 0, singletonList("date"));
        testIsValid(false, "name", ImportType.SINGLE_DETAIL_ROWS, FileType.CSV, "yyyy-mm-dd", null, singletonList("date"));
        testIsValid(false, "name", ImportType.SINGLE_DETAIL_ROWS, FileType.CSV, "yyyy-mm-dd", 0, emptyList());
    }

    private void testIsValid(boolean valid, String name, ImportType importType, FileType fileType, String dateFormat, Integer startOffset,
            List<String> dateLabels) {
        model.setName(name);
        model.setImportType(importType);
        model.setFileType(fileType);
        model.setDateFormat(dateFormat);
        model.setStartOffset(startOffset);

        assertThat(model.isValid()).isEqualTo(valid);
    }

    @Test
    public void isValid_fieldsLoaded() throws Exception {
        // model.getFieldModels();
        testIsValid(false, "name", ImportType.SINGLE_DETAIL_ROWS, FileType.CSV, "yyyy-mm-dd", 0, singletonList("date"));

        // model.addFieldModel(new ImportField());
        testIsValid(true, "name", ImportType.SINGLE_DETAIL_ROWS, FileType.CSV, "yyyy-mm-dd", 0, singletonList("date"));
    }

    @Test
    public void resetChanges() throws Exception {
        PropertyChangeListener listener = mock(PropertyChangeListener.class);
        model.setName("name");
        // model.getFieldModels();
        // model.addFieldModel(new ImportField());
        model.addPropertyChangeListener(listener);

        model.resetChanges();

        assertThat(model.getName()).isEqualTo("");
        // assertThat(model.getFieldModels()).isEmpty();
        MethodHandler source = ((ProxyObject) model).getHandler();
        verify(listener).propertyChange(matches(new PropertyChangeEvent(source, "name", null, null)));
        verify(listener).propertyChange(matches(new PropertyChangeEvent(source, "dateLabelsChanged", null, false)));
        verify(listener).propertyChange(matches(new PropertyChangeEvent(source, "payeeLabelsChanged", null, false)));
        verify(listener).propertyChange(matches(new PropertyChangeEvent(source, "securityLabelsChanged", null, false)));
        verify(listener).propertyChange(matches(new PropertyChangeEvent(source, "fields", null, Collections.emptyList())));
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