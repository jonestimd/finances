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
package io.github.jonestimd.finance.operations;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import io.github.jonestimd.cache.Cacheable;
import io.github.jonestimd.finance.dao.CurrencyDao;
import io.github.jonestimd.finance.dao.DaoRepository;
import io.github.jonestimd.finance.dao.SecurityDao;
import io.github.jonestimd.finance.dao.StockSplitDao;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.asset.Currency;
import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.asset.SecuritySummary;
import io.github.jonestimd.finance.domain.asset.SecurityType;
import io.github.jonestimd.finance.domain.asset.SplitRatio;
import io.github.jonestimd.finance.domain.transaction.StockSplit;

import static io.github.jonestimd.finance.swing.BundleType.*;

public class AssetOperationsImpl implements AssetOperations {
    private static final String SECURITY_TYPE_MISMATCH = "import.qif.securityTypeMismatch";
    private final CurrencyDao currencyDao;
    private final SecurityDao securityDao;
    private final StockSplitDao stockSplitDao;

    public AssetOperationsImpl(DaoRepository daoRepository) {
        this.currencyDao = daoRepository.getCurrencyDao();
        this.securityDao = daoRepository.getSecurityDao();
        this.stockSplitDao = daoRepository.getStockSplitDao();
    }

    public Currency getCurrency(String code) {
        return currencyDao.getCurrency(code);
    }

    public List<Security> getAllSecurities() {
        return securityDao.getAll();
    }

    public List<SecuritySummary> getSecuritySummaries() {
        return securityDao.getSecuritySummaries();
    }

    public List<SecuritySummary> getSecuritySummaries(Account account) {
        return securityDao.getSecuritySummaries(account.getId());
    }

    @Override
    public List<SecuritySummary> getSecuritySummariesByAccount() {
        return securityDao.getSecuritySummariesByAccount();
    }

    public Security getSecurity(String symbol) {
        return securityDao.getSecurity(symbol);
    }

    public Security createIfUnique(Security security) {
        Security existingSecurity = securityDao.getSecurity(security.getSymbol());
        if (existingSecurity != null && ! existingSecurity.getType().equals(security.getType())) {
            throw new IllegalArgumentException(MESSAGES.formatMessage(SECURITY_TYPE_MISMATCH, security.getSymbol()));
        }
        return existingSecurity == null ? securityDao.save(security) : existingSecurity;
    }

    public Security save(Security security) {
        return securityDao.save(security);
    }

    public <T extends Iterable<Security>> T saveAll(T securities) {
        return securityDao.saveAll(securities);
    }

    @Cacheable
    public Security findOrCreate(String name) {
        Security security = securityDao.findByName(name);
        if (security == null) {
            security = new Security();
            security.setName(name);
            security.setType(SecurityType.STOCK.getValue());
            securityDao.save(security);
        }
        return security;
    }

    public StockSplit findOrCreateSplit(String securityName, Date splitDate, BigDecimal sharesIn, BigDecimal sharesOut) {
        // TODO check for sales/shares_out after splitDate and warn user to reallocate lots
        Security security = findOrCreate(securityName);
        StockSplit split = stockSplitDao.find(security, splitDate);
        if (split == null) {
            split = new StockSplit();
            split.setSecurity(security);
            split.setDate(splitDate);
            split.setSplitRatio(new SplitRatio(sharesIn, sharesOut));
            stockSplitDao.save(split);
        }
        return split;
    }

    @Override
    public <T extends Iterable<Security>> void deleteAll(T securities) {
        securityDao.deleteAll(securities);
    }
}