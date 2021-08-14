// The MIT License (MIT)
//
// Copyright (c) 2021 Tim Jones
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
package io.github.jonestimd.finance.dao;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.github.jonestimd.finance.domain.asset.Currency;
import io.github.jonestimd.finance.domain.transaction.SecurityAction;
import io.github.jonestimd.finance.domain.transaction.TransactionCategory;

// drop table stock_split, tx_detail, security_lot, tx, payee, tx_group,
//      account, tx_category, security, company, asset
public class SchemaBuilder {
    private static final String CHANGE_USER = "REF_DATA";
    private DaoRepository daoRepository;

    public SchemaBuilder(DaoRepository daoContext) {
        this.daoRepository = daoContext;
    }

    public SchemaBuilder createSchemaTables(List<String> postCreateScript) throws Exception {
        daoRepository.generateSchema(postCreateScript);
        return this;
    }

    public SchemaBuilder seedReferenceData() {
        daoRepository.doInTransaction(() -> {
            Date changeDate = new Date();
            getSecurityActions(changeDate).forEach(daoRepository.getTransactionCategoryDao()::save);
            daoRepository.getCurrencyDao().save(createDefaultCurrency(changeDate));
        });
        return this;
    }

    private Currency createDefaultCurrency(Date changeDate) {
        Currency currency = new Currency(java.util.Currency.getInstance(Locale.getDefault()));
        currency.setChangeDate(changeDate);
        currency.setChangeUser(CHANGE_USER);
        return currency;
    }

    public static List<TransactionCategory> getSecurityActions(Date changeDate) {
        List<TransactionCategory> securityActions = Arrays.asList(
            securityAction(SecurityAction.BUY, changeDate),
            securityAction(SecurityAction.SELL, changeDate),
            securityAction(SecurityAction.REINVEST, changeDate),
            securityAction(SecurityAction.SHARES_IN, changeDate),
            securityAction(SecurityAction.SHARES_OUT, changeDate),
            securityAction(SecurityAction.DIVIDEND, changeDate),
            securityAction(SecurityAction.INTEREST, changeDate),
            securityAction(SecurityAction.SHORT_TERM_CAP_GAIN, changeDate),
            securityAction(SecurityAction.MID_TERM_CAP_GAIN, changeDate),
            securityAction(SecurityAction.LONG_TERM_CAP_GAIN, changeDate),
            securityAction(SecurityAction.COMMISSION_AND_FEES, changeDate));
        return Collections.unmodifiableList(securityActions);
    }

    private static TransactionCategory securityAction(SecurityAction action, Date changeDate) {
        TransactionCategory category = new TransactionCategory();
        category.setIncome(action.isIncome());
        category.setSecurity(true);
        category.setAmountType(action.getAmountType());
        category.setCode(action.code());
        category.setChangeDate(changeDate);
        category.setChangeUser(CHANGE_USER);
        return category;
    }
}