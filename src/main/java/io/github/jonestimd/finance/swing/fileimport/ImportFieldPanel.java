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

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTextField;

import io.github.jonestimd.finance.domain.fileimport.FieldType;
import io.github.jonestimd.finance.domain.fileimport.PageRegion;
import io.github.jonestimd.swing.ComponentFactory;
import io.github.jonestimd.swing.component.MultiSelectField;
import io.github.jonestimd.swing.layout.FormElement;
import io.github.jonestimd.swing.layout.GridBagBuilder;

import static io.github.jonestimd.finance.swing.BundleType.*;

public class ImportFieldPanel extends JComponent {
    public static final String RESOURCE_PREFIX = "dialog.fileImport.importField.";

    private final JComboBox<FieldType> typeField = new JComboBox<>(FieldType.values());
    private final JComboBox<PageRegion> pageRegionField = new JComboBox<>();
    private final MultiSelectField labelField = new MultiSelectField.Builder(false, true).disableTab().get();
    private final JTextField amountFormatField = new JTextField();
    private final JCheckBox negateField = ComponentFactory.newCheckBox(LABELS.get(), RESOURCE_PREFIX + "negateAmount");
    private final JTextField acceptRegexField = new JTextField();
    private final JTextField rejectRegexField = new JTextField();
    private final JTextField memoField = new JTextField();

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
    }
}
