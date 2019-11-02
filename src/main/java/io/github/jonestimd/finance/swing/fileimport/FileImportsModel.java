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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import io.github.jonestimd.finance.domain.fileimport.ImportFile;
import io.github.jonestimd.swing.component.BeanListComboBoxModel;
import io.github.jonestimd.util.Streams;

import static io.github.jonestimd.finance.swing.BundleType.*;

public class FileImportsModel extends BeanListComboBoxModel<ImportFile> {
    public static final String RESOURCE_PREFIX = "dialog.fileImport.";

    private static final String NAME_REQUIRED = LABELS.getString(RESOURCE_PREFIX + "name.required");
    private static final String NAME_UNIQUE = LABELS.getString(RESOURCE_PREFIX + "name.unique");

    private final Map<Long, PageRegionTableModel> regionTableModels = new HashMap<>();
    private final List<BiConsumer<FileImportsModel, ImportFile>> selectionListeners = new ArrayList<>();

    public FileImportsModel(Collection<? extends ImportFile> elements) {
        super(elements);
    }

    public String validateName(String name) {
        if (name.trim().isEmpty()) return NAME_REQUIRED;
        List<ImportFile> matches = Streams.filter(this, file -> file.getName().equalsIgnoreCase(name));
        if (matches.size() > 1 || !matches.isEmpty() && matches.get(0) != getSelectedItem()) return NAME_UNIQUE;
        return null;
    }

    public PageRegionTableModel getRegionTableModel() {
        if (getSelectedItem() == null) return new PageRegionTableModel();
        return regionTableModels.computeIfAbsent(getSelectedItem().getId(), (id) -> {
            PageRegionTableModel model = new PageRegionTableModel();
            model.setBeans(getSelectedItem().getPageRegions());
            return model;
        });
    }

    @Override
    public void setSelectedItem(Object anItem) {
        super.setSelectedItem(anItem);
        notifySelectionListeners((ImportFile) anItem);
    }

    public void addSelectionListener(BiConsumer<FileImportsModel, ImportFile> listener) {
        selectionListeners.add(listener);
    }

    private void notifySelectionListeners(ImportFile importFile) {
        selectionListeners.forEach(listener -> listener.accept(this, importFile));
    }

    public boolean isChanged() {
        return false;
    }
}
