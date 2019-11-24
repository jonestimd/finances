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

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.swing.table.model.FunctionColumnAdapter;
import io.github.jonestimd.swing.table.model.ValidatedColumnAdapter;

import static io.github.jonestimd.finance.swing.BundleType.*;

public abstract class ImportMappingColumnAdapter<T, V> extends FunctionColumnAdapter<ImportMapping<T>, V>
        implements ValidatedColumnAdapter<ImportMapping<T>, V> {
    public static final String RESOURCE_PREFIX = "table.importMapping.column.";
    private static final String ALIAS_REQUIRED = LABELS.getString(RESOURCE_PREFIX + "alias.required");
    private static final String ALIAS_UNIQUE = LABELS.getString(RESOURCE_PREFIX + "alias.unique");
    private static final String PAYEE_REQUIRED = LABELS.getString(RESOURCE_PREFIX + "payee.required");
    private static final String SECURITY_REQUIRED = LABELS.getString(RESOURCE_PREFIX + "security.required");

    protected ImportMappingColumnAdapter(String columnId, Class<? super V> valueType,
            Function<ImportMapping<T>, V> getter, BiConsumer<ImportMapping<T>, V> setter) {
        super(LABELS.get(), RESOURCE_PREFIX, columnId, valueType, getter, setter);
    }

    public static final ImportMappingColumnAdapter<?, String> ALIAS_COLUMN_ADAPTER =
        new ImportMappingColumnAdapter<Object, String>("alias", String.class, ImportMapping::getAlias, ImportMapping::setAlias) {
            @Override
            public String validate(int selectedIndex, String alias, List<? extends ImportMapping<Object>> beans) {
                if (alias == null || alias.trim().isEmpty()) return ALIAS_REQUIRED;
                ImportMapping<?> row = beans.get(selectedIndex);
                if (beans.stream().filter(bean -> bean != row).anyMatch(bean -> bean.getAlias().equals(alias))) {
                    return ALIAS_UNIQUE;
                }
                return null;
            }
        };

    public static final ImportMappingColumnAdapter<Payee, Payee> PAYEE_COLUMN_ADAPTER =
        new ImportMappingColumnAdapter<Payee, Payee>("payee", Payee.class, ImportMapping::getBean, ImportMapping::setBean) {
            @Override
            public String validate(int selectedIndex, Payee payee, List<? extends ImportMapping<Payee>> beans) {
                if (payee == null) return PAYEE_REQUIRED;
                return null;
            }
        };

    public static final ImportMappingColumnAdapter<Security, Security> SECURITY_COLUMN_ADAPTER =
        new ImportMappingColumnAdapter<Security, Security>("security", Security.class, ImportMapping::getBean, ImportMapping::setBean) {
            @Override
            public String validate(int selectedIndex, Security security, List<? extends ImportMapping<Security>> beans) {
                if (security == null) return PAYEE_REQUIRED;
                return null;
            }
        };
}
