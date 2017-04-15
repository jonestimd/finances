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
package io.github.jonestimd.finance.file.quicken.qif;

import java.util.Collections;
import java.util.Currency;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.account.AccountType;
import io.github.jonestimd.finance.operations.AccountOperations;
import io.github.jonestimd.finance.operations.AssetOperations;
import org.apache.log4j.Logger;

public class AccountConverter implements RecordConverter {

    private static final Logger logger = Logger.getLogger(AccountConverter.class);
    private static final Set<String> TYPES = Collections.singleton("Account");

    private Map<String, AccountType> typeMap;
    private AssetOperations assetOperations;
    private AccountOperations accountOperations;

    public AccountConverter(Map<String, AccountType> typeMap, AssetOperations assetOperations, AccountOperations accountOperations) {
        this.typeMap = typeMap;
        this.assetOperations = assetOperations;
        this.accountOperations = accountOperations;
    }

    @Override
    public String getStatusKey() {
        return "import.qif.account.converter.status";
    }

    public void importRecord(AccountHolder accountHolder, QifRecord record) {
        String name = record.getValue(QifField.NAME);
        Account account = accountOperations.getAccount(null, name);
        String logMessage = "Account: ";
        if (account == null) {
            logMessage = "Creating account: ";
            account = new Account(assetOperations.getCurrency(Currency.getInstance(Locale.getDefault()).getCurrencyCode()));
            account.setName(name);
            account.setDescription(record.getValue(QifField.DESCRIPTION));
            account.setType(typeMap.get(record.getValue(QifField.TYPE)));
            accountOperations.save(account);
        }
        accountHolder.setAccount(account);
        logger.info(logMessage + account.getName());
    }

    public Set<String> getTypes() {
        return TYPES;
    }
}
