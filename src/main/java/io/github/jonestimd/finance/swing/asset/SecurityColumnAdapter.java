// The MIT License (MIT)
//
// Copyright (c) 2016 Tim Jones
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
package io.github.jonestimd.finance.swing.asset;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.google.common.collect.ImmutableList;
import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.asset.SecuritySummary;
import io.github.jonestimd.finance.domain.transaction.TransactionSummary;
import io.github.jonestimd.finance.swing.transaction.TransactionSummaryColumnAdapter;
import io.github.jonestimd.swing.table.model.ColumnAdapter;
import io.github.jonestimd.swing.table.model.FunctionColumnAdapter;
import io.github.jonestimd.swing.validation.BeanPropertyValidator;
import io.github.jonestimd.swing.validation.RequiredValidator;

import static io.github.jonestimd.finance.swing.BundleType.*;

public class SecurityColumnAdapter<V> extends FunctionColumnAdapter<SecuritySummary, V> implements BeanPropertyValidator<SecuritySummary, V> {
    public static final String RESOURCE_PREFIX = "table.security.";
    private final BeanPropertyValidator<SecuritySummary, V> validator;

    private SecurityColumnAdapter(String columnId, Class<V> valueType, Function<Security, V> getter, BiConsumer<Security, V> setter,
                                  BeanPropertyValidator<SecuritySummary, V> validator) {
        super(LABELS.get(), RESOURCE_PREFIX, columnId, valueType, TransactionSummary.compose(getter), TransactionSummary.compose(setter));
        this.validator = validator;
    }

    @Override
    public String validate(int selectedIndex, V propertyValue, List<? extends SecuritySummary> beans) {
        return validator.validate(selectedIndex, propertyValue, beans);
    }

    public static final ColumnAdapter<SecuritySummary, String> TYPE_ADAPTER =
            new SecurityColumnAdapter<>(Security.TYPE, String.class, Security::getType, Security::setType,
            BeanPropertyValidator.from(new RequiredValidator(LABELS.getString("validation.security.typeRequired"))));

    public static final ColumnAdapter<SecuritySummary, String> NAME_ADAPTER =
            new SecurityColumnAdapter<>(Security.NAME, String.class, Security::getName, Security::setName,
            new SecurityNameValidator<>(SecuritySummary::getName));

    public static final ColumnAdapter<SecuritySummary, String> SYMBOL_ADAPTER = new FunctionColumnAdapter<>(
            LABELS.get(), RESOURCE_PREFIX, Security.SYMBOL, String.class, TransactionSummary.compose(Security::getSymbol), TransactionSummary.compose(Security::setSymbol));

    public static final ColumnAdapter<SecuritySummary, BigDecimal> SHARES_ADAPTER =
            new FunctionColumnAdapter<>(LABELS.get(), RESOURCE_PREFIX, "shares", BigDecimal.class, SecuritySummary::getShares, null);

    public static final ColumnAdapter<SecuritySummary, Date> FIRST_PURCHASE_ADAPTER =
            new FunctionColumnAdapter<>(LABELS.get(), RESOURCE_PREFIX, "firstAcquired", Date.class, SecuritySummary::getFirstAcquired, null);

    public static final ColumnAdapter<SecuritySummary, BigDecimal> COST_BASIS_ADAPTER =
            new FunctionColumnAdapter<>(LABELS.get(), RESOURCE_PREFIX, "costBasis", BigDecimal.class,
                    summary -> summary.getShares().compareTo(BigDecimal.ZERO) > 0 ? summary.getCostBasis() : null, null);

    public static final ColumnAdapter<SecuritySummary, BigDecimal> DIVIDENDS_ADAPTER =
            new FunctionColumnAdapter<>(LABELS.get(), RESOURCE_PREFIX, "dividends", BigDecimal.class, SecuritySummary::getDividends, null);

    public static final List<ColumnAdapter<? super SecuritySummary, ?>> ADAPTERS = ImmutableList.<ColumnAdapter<? super SecuritySummary, ?>>of(
            SecurityColumnAdapter.NAME_ADAPTER,
            SecurityColumnAdapter.TYPE_ADAPTER,
            SecurityColumnAdapter.SYMBOL_ADAPTER,
            SecurityColumnAdapter.FIRST_PURCHASE_ADAPTER,
            TransactionSummaryColumnAdapter.COUNT_ADAPTER,
            SecurityColumnAdapter.SHARES_ADAPTER,
            SecurityColumnAdapter.COST_BASIS_ADAPTER,
            SecurityColumnAdapter.DIVIDENDS_ADAPTER);
}