// The MIT License (MIT)
//
// Copyright (c) 2017 Tim Jones
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
package io.github.jonestimd.finance.swing.account;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import io.github.jonestimd.finance.domain.account.Company;
import io.github.jonestimd.finance.swing.BundleType;
import io.github.jonestimd.swing.table.model.FunctionColumnAdapter;
import io.github.jonestimd.swing.table.model.ValidatedBeanListTableModel;
import io.github.jonestimd.swing.validation.BeanPropertyValidator;
import org.apache.commons.lang.StringUtils;

public class CompanyTableModel extends ValidatedBeanListTableModel<Company> {
    public static final int NAME_INDEX = 0;

    private static final CompanyColumnAdapter<String> NAME_ADAPTER =
        new CompanyColumnAdapter<String>(Company.NAME, String.class, Company::getName, Company::setName) {
            private final String requiredMessage = BundleType.LABELS.getString("validation.company.requireName");
            private final String uniqueMessage = BundleType.LABELS.getString("validation.company.uniqueName");

            @Override
            public String validate(int selectedIndex, String name, List<? extends Company> companies) {
                if (StringUtils.isBlank(name)) {
                    return requiredMessage;
                }
                Company selectedCompany = companies.get(selectedIndex);
                for (Company company : companies) {
                    if (selectedCompany != company && name.trim().equalsIgnoreCase(company.getName())) {
                        return uniqueMessage;
                    }
                }
                return null;
            }
        };
    private static final CompanyColumnAdapter<Integer> ACCOUNTS_ADAPTER =
        new CompanyColumnAdapter<Integer>("accounts", Integer.class, company -> company.getAccounts().size(), null) {
            @Override
            public String validate(int selectedIndex, Integer propertyValue, List<? extends Company> beans) {
                return null;
            }
        };

    @SuppressWarnings("unchecked")
    public CompanyTableModel() {
        super(Arrays.asList(NAME_ADAPTER, ACCOUNTS_ADAPTER));
    }

    private abstract static class CompanyColumnAdapter<V> extends FunctionColumnAdapter<Company, V> implements BeanPropertyValidator<Company, V> {
        protected CompanyColumnAdapter(String columnId, Class<? super V> valueType, Function<Company, V> getter, BiConsumer<Company, V> setter) {
            super(BundleType.LABELS.get(), "table.company.column.", columnId, valueType, getter, setter);
        }
    }
}