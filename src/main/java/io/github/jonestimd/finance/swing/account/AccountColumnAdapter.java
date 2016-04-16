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
package io.github.jonestimd.finance.swing.account;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.account.AccountSummary;
import io.github.jonestimd.finance.domain.account.AccountType;
import io.github.jonestimd.finance.domain.account.Company;
import io.github.jonestimd.finance.domain.transaction.TransactionSummary;
import io.github.jonestimd.finance.swing.BundleType;
import io.github.jonestimd.swing.table.model.ColumnAdapter;
import io.github.jonestimd.swing.table.model.FunctionColumnAdapter;
import io.github.jonestimd.swing.validation.BeanPropertyValidator;

import static io.github.jonestimd.finance.swing.BundleType.*;

public class AccountColumnAdapter<V> extends FunctionColumnAdapter<AccountSummary, V> implements BeanPropertyValidator<AccountSummary, V> {
    public static final String RESOURCE_PREFIX = "table.account.column.";
    private final BeanPropertyValidator<AccountSummary, V> validator;

    private AccountColumnAdapter(String columnId, Class<? super V> valueType, Function<Account, V> getter, BiConsumer<Account, V> setter, BeanPropertyValidator<AccountSummary, V> validator) {
        super(BundleType.LABELS.get(), RESOURCE_PREFIX, columnId, valueType, TransactionSummary.compose(getter), TransactionSummary.compose(setter));
        this.validator = validator;
    }

    @Override
    public String validate(int selectedIndex, V propertyValue, List<? extends AccountSummary> beans) {
        return validator.validate(selectedIndex, propertyValue, beans);
    }

    private static <V> ColumnAdapter<AccountSummary, V> create(String columnId, Class<? super V> valueType,
               Function<Account,V> getter, BiConsumer<Account, V> setter) {
        return new FunctionColumnAdapter<>(LABELS.get(), RESOURCE_PREFIX, columnId, valueType, TransactionSummary.compose(getter), TransactionSummary.compose(setter));
    }

    public static final ColumnAdapter<AccountSummary, Boolean> CLOSED_ADAPTER =
            create(Account.CLOSED, Boolean.class, Account::isClosed, Account::setClosed);

    public static final ColumnAdapter<AccountSummary, String> NAME_ADAPTER =
            new AccountColumnAdapter<>(Account.NAME, String.class, Account::getName, Account::setName, new AccountNameValidator());

    public static final ColumnAdapter<AccountSummary, AccountType> TYPE_ADAPTER = new AccountColumnAdapter<AccountType>(
            Account.TYPE, AccountType.class, Account::getType, Account::setType,
            BeanPropertyValidator.from(new AccountTypeValidator())) {

        @Override
        public boolean isEditable(AccountSummary row) {
            return row.getTransactionAttribute().getId() == null && super.isEditable(row);
        }
    };

    public static final ColumnAdapter<AccountSummary, String> NUMBER_ADAPTER =
            create(Account.NUMBER, String.class, Account::getNumber, Account::setNumber);

    public static final ColumnAdapter<AccountSummary, String> DESCRIPTION_ADAPTER =
            create(Account.DESCRIPTION, String.class, Account::getDescription, Account::setDescription);

    public static final ColumnAdapter<AccountSummary, Company> COMPANY_ADAPTER =
            create(Account.COMPANY + '.' + Company.NAME, Company.class, Account::getCompany, Account::setCompany);

    public static final ColumnAdapter<AccountSummary, BigDecimal> BALANCE_ADAPTER =
            new FunctionColumnAdapter<>(LABELS.get(), RESOURCE_PREFIX, "balance", BigDecimal.class, AccountSummary::getBalance, null);
}