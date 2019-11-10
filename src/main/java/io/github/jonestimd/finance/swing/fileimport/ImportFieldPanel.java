// The MIT License (MIT)
//
// Copyright (c) 2019 Tim Jones
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
package io.github.jonestimd.finance.swing.fileimport;

import java.awt.Color;
import java.awt.event.ItemEvent;
import java.beans.PropertyChangeListener;
import java.text.Format;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTextField;

import io.github.jonestimd.collection.MapBuilder;
import io.github.jonestimd.finance.domain.fileimport.AmountFormat;
import io.github.jonestimd.finance.domain.fileimport.FieldType;
import io.github.jonestimd.finance.domain.fileimport.PageRegion;
import io.github.jonestimd.finance.swing.FormatFactory;
import io.github.jonestimd.swing.ComponentFactory;
import io.github.jonestimd.swing.component.BeanListComboBox;
import io.github.jonestimd.swing.component.BeanListComboBoxModel;
import io.github.jonestimd.swing.component.MultiSelectField;
import io.github.jonestimd.swing.layout.FormElement;
import io.github.jonestimd.swing.layout.GridBagBuilder;

import static io.github.jonestimd.finance.swing.BundleType.*;
import static io.github.jonestimd.finance.swing.ComponentUtils.*;
import static io.github.jonestimd.swing.component.ComponentBinder.*;

public class ImportFieldPanel extends JComponent {
    public static final String RESOURCE_PREFIX = "dialog.fileImport.importField.";
    private static final Format REGION_FORMAT = FormatFactory.format(PageRegion::getName);
    private static final FieldType[] FIELD_TYPES = Arrays.stream(FieldType.values()).filter(type -> !type.isTransaction())
            .sorted(Comparator.comparing(Object::toString)).toArray(FieldType[]::new);

    private final JComboBox<FieldType> typeField = new JComboBox<>(FIELD_TYPES);
    private final BeanListComboBox<PageRegion> pageRegionField = new BeanListComboBox<>(REGION_FORMAT);
    private final MultiSelectField labelField = new MultiSelectField.Builder(false, true).disableTab().get();
    private final JComboBox<AmountFormat> amountFormatField = new JComboBox<>(AmountFormat.values());
    private final JCheckBox negateField = ComponentFactory.newCheckBox(LABELS.get(), RESOURCE_PREFIX + "negateAmount");
    private final JTextField acceptRegexField = new JTextField();
    private final JTextField rejectRegexField = new JTextField();
    private final JTextField memoField = new JTextField();
    private ImportFieldModel model;
    private final PropertyChangeListener onPropertyChange;

    public ImportFieldPanel() {
        GridBagBuilder builder = new GridBagBuilder(this, LABELS.get(), RESOURCE_PREFIX)
                .useScrollPane(MultiSelectField.class)
                .setConstraints(MultiSelectField.class, FormElement.TEXT_AREA);
        builder.append("label", labelField);
        builder.unrelatedVerticalGap().append("type", typeField);
        builder.append("amountFormat", amountFormatField);
        builder.append(negateField);
        builder.append("acceptRegex", acceptRegexField);
        builder.append("rejectRegex", rejectRegexField);
        builder.append("pageRegion", pageRegionField);
        builder.append("memo", memoField);
        bindToModel();
        final Map<String, JComponent> propertyLabelMap = new MapBuilder<String, JComponent>()
                .put("changedLabels", getLabel(labelField))
                .put("changedType", getLabel(typeField))
                .put("changedAmountFormat", getLabel(amountFormatField))
                .put("changedNegate", negateField)
                .put("changedAcceptRegex", getLabel(acceptRegexField))
                .put("changedIgnoreRegex", getLabel(rejectRegexField))
                .put("changedMemo", getLabel(memoField))
                .put("changedRegion", getLabel(pageRegionField))
                .get();
        onPropertyChange = (event) -> {
            Color labelColor = getLabelColor(event.getNewValue() == Boolean.TRUE);
            JComponent component = propertyLabelMap.get(event.getPropertyName());
            if (component != null) component.setForeground(labelColor);
        };
    }

    private Color getLabelColor(boolean changed) {
        return changed ? Color.BLUE : Color.BLACK;
    }

    private void bindToModel() {
        bindLabelField(labelField, ImportFieldModel::setLabels);
        bindComboBox(typeField, ImportFieldModel::setType);
        bindComboBox(amountFormatField, ImportFieldModel::setAmountFormat);
        negateField.addItemListener(event -> model.setNegate(event.getStateChange() == ItemEvent.SELECTED));
        bind(acceptRegexField, ImportFieldPanel::emptyToNull, (pattern) -> model.setAcceptRegex(pattern));
        bind(rejectRegexField, ImportFieldPanel::emptyToNull, (pattern) -> model.setIgnoredRegex(pattern));
        bind(memoField, ImportFieldPanel::emptyToNull, (memo) -> model.setMemo(memo));
        bindComboBox(pageRegionField, ImportFieldModel::setRegion);
    }

    private static String emptyToNull(String value) {
        return value.isEmpty() ? null : value;
    }

    private void bindLabelField(MultiSelectField field, BiConsumer<ImportFieldModel, List<String>> setter) {
        field.addPropertyChangeListener(MultiSelectField.ITEMS_PROPERTY, (event) -> setter.accept(model, field.getItems()));
    }

    @SuppressWarnings("unchecked")
    private <T> void bindComboBox(JComboBox<T> field, BiConsumer<ImportFieldModel, T> setter) {
        field.addItemListener(event -> {
            if (event.getStateChange() == ItemEvent.SELECTED) setter.accept(model, (T) event.getItem());
        });
    }

    public void setImportFile(ImportFileModel importFile) {
        pageRegionField.setModel(new BeanListComboBoxModel<>(importFile.getPageRegions()));
    }

    public void setImportField(ImportFieldModel model) {
        if (this.model != null) this.model.removePropertyChangeListener(this.onPropertyChange);
        this.model = model;
        model.addPropertyChangeListener(this.onPropertyChange);
        labelField.setItems(model.getLabels());
        typeField.setSelectedItem(model.getType());
        amountFormatField.setSelectedItem(model.getAmountFormat());
        negateField.setSelected(model.isNegate());
        acceptRegexField.setText(model.getAcceptRegex());
        rejectRegexField.setText(model.getIgnoredRegex());
        pageRegionField.setSelectedItem(model.getRegion());
        memoField.setText(model.getMemo());
    }
}
