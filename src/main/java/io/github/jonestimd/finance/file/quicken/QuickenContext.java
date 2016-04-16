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
package io.github.jonestimd.finance.file.quicken;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.jonestimd.cache.CachingMethodInterceptor;
import io.github.jonestimd.finance.domain.account.AccountType;
import io.github.jonestimd.finance.file.FileImport;
import io.github.jonestimd.finance.file.quicken.capitalgain.CapitalGainImport;
import io.github.jonestimd.finance.file.quicken.handler.ActionAmountSecurityDetailHandler;
import io.github.jonestimd.finance.file.quicken.handler.MiscSecurityDetailHandler;
import io.github.jonestimd.finance.file.quicken.handler.MultiActionSecurityHandler;
import io.github.jonestimd.finance.file.quicken.handler.NetAmountSecurityDetailHandler;
import io.github.jonestimd.finance.file.quicken.handler.SecurityDetailHandler;
import io.github.jonestimd.finance.file.quicken.handler.SecurityTransactionHandler;
import io.github.jonestimd.finance.file.quicken.handler.StockSplitSecurityHandler;
import io.github.jonestimd.finance.file.quicken.handler.TransferHandler;
import io.github.jonestimd.finance.file.quicken.qif.AccountConverter;
import io.github.jonestimd.finance.file.quicken.qif.CategoryConverter;
import io.github.jonestimd.finance.file.quicken.qif.ClassConverter;
import io.github.jonestimd.finance.file.quicken.qif.MoneyTransactionConverter;
import io.github.jonestimd.finance.file.quicken.qif.PayeeCache;
import io.github.jonestimd.finance.file.quicken.qif.QifImport;
import io.github.jonestimd.finance.file.quicken.qif.SecurityConverter;
import io.github.jonestimd.finance.file.quicken.qif.SecurityTransactionConverter;
import io.github.jonestimd.finance.file.quicken.qif.TransferDetailCache;
import io.github.jonestimd.finance.operations.AccountOperations;
import io.github.jonestimd.finance.operations.AssetOperations;
import io.github.jonestimd.finance.operations.TransactionCategoryOperations;
import io.github.jonestimd.finance.service.ServiceLocator;
import io.github.jonestimd.finance.swing.securitylot.LotAllocationDialog;

public class QuickenContext {
    private ServiceLocator serviceLocator;
    private TransferDetailCache transferDetailCache = new TransferDetailCache();
    private AccountOperations cachedAccountOperations;
    private PayeeCache payeeCache;
    private TransactionCategoryOperations cachedTransactionCategoryOperations;
    private AssetOperations cachedAssetOperations;
    private AccountConverter accountConverter;
    private CategoryConverter categoryConverter;
    private ClassConverter classConverter;
    private Map<String, SecurityTransactionHandler> securityHandlers;
    private SecurityConverter securityConverter;
    private MoneyTransactionConverter moneyTransactionConverter;
    private SecurityTransactionConverter securityTransactionConverter;

    public QuickenContext(ServiceLocator serviceLocator) {
        this.serviceLocator = serviceLocator;
        cachedAccountOperations = applyCachingInterceptor(serviceLocator.getAccountOperations(), AccountOperations.class);
        cachedTransactionCategoryOperations = applyCachingInterceptor(serviceLocator.getTransactionCategoryOperations(), TransactionCategoryOperations.class);
        cachedAssetOperations = applyCachingInterceptor(serviceLocator.getAssetOperations(), AssetOperations.class);
    }

    private synchronized PayeeCache getPayeeCache() {
        if (payeeCache == null) {
            payeeCache = new PayeeCache(serviceLocator.getPayeeOperations());
        }
        return payeeCache;
    }

    private void buildAccountConverter() {
        Map<String, AccountType> typeMap = new HashMap<>();
        typeMap.put("Bank", AccountType.BANK);
        typeMap.put("CCard", AccountType.CREDIT);
        typeMap.put("Cash", AccountType.CASH);
        typeMap.put("Oth L", AccountType.LOAN);
        typeMap.put("Port", AccountType.BROKERAGE);
        typeMap.put("401(k)", AccountType._401K);
        accountConverter = new AccountConverter(typeMap, cachedAssetOperations, cachedAccountOperations);
    }

    private void buildSecurityTransactionConverter() {
        SecurityDetailHandler buyDetailHandler = new ActionAmountSecurityDetailHandler("Buy", cachedTransactionCategoryOperations);
        SecurityDetailHandler reinvestDetailHandler = new ActionAmountSecurityDetailHandler("Reinvest", cachedTransactionCategoryOperations);
        SecurityDetailHandler sellDetailHandler = new ActionAmountSecurityDetailHandler("Sell", cachedTransactionCategoryOperations);
        SecurityDetailHandler shrsInDetailHandler = new ActionAmountSecurityDetailHandler("Shares In", cachedTransactionCategoryOperations);
        SecurityDetailHandler shrsOutDetailHandler = new ActionAmountSecurityDetailHandler("Shares Out", cachedTransactionCategoryOperations);
        SecurityDetailHandler dividendDetailHandler = new NetAmountSecurityDetailHandler("Dividend", cachedTransactionCategoryOperations);
        SecurityDetailHandler interestDetailHandler = new NetAmountSecurityDetailHandler("Interest", cachedTransactionCategoryOperations);
        SecurityDetailHandler stcgDetailHandler = new NetAmountSecurityDetailHandler("Short Term Cap Gain", cachedTransactionCategoryOperations);
        SecurityDetailHandler mtcgDetailHandler = new NetAmountSecurityDetailHandler("Mid Term Cap Gain", cachedTransactionCategoryOperations);
        SecurityDetailHandler ltcgDetailHandler = new NetAmountSecurityDetailHandler("Long Term Cap Gain", cachedTransactionCategoryOperations);
        SecurityDetailHandler miscIncDetailHandler = new MiscSecurityDetailHandler(cachedTransactionCategoryOperations,
                serviceLocator.getTransactionGroupOperations(), true, "Misc Income");
        SecurityDetailHandler miscExpDetailHandler = new MiscSecurityDetailHandler(cachedTransactionCategoryOperations,
                serviceLocator.getTransactionGroupOperations(), false, "Misc Expense");

        List<String> sharesOutActions = Arrays.asList("Sell", "ShrsOut");
        securityHandlers = new HashMap<>();
        securityHandlers.put("ReinvDiv", new MultiActionSecurityHandler(sharesOutActions, cachedAssetOperations, cachedTransactionCategoryOperations, dividendDetailHandler, reinvestDetailHandler));
        securityHandlers.put("ReinvInt", new MultiActionSecurityHandler(sharesOutActions, cachedAssetOperations, cachedTransactionCategoryOperations, interestDetailHandler, reinvestDetailHandler));
        securityHandlers.put("ReinvSh", new MultiActionSecurityHandler(sharesOutActions, cachedAssetOperations, cachedTransactionCategoryOperations, stcgDetailHandler, reinvestDetailHandler));
        securityHandlers.put("ReinvMd", new MultiActionSecurityHandler(sharesOutActions, cachedAssetOperations, cachedTransactionCategoryOperations, mtcgDetailHandler, reinvestDetailHandler));
        securityHandlers.put("ReinvLg", new MultiActionSecurityHandler(sharesOutActions, cachedAssetOperations, cachedTransactionCategoryOperations, ltcgDetailHandler, reinvestDetailHandler));
        securityHandlers.put("Buy", new MultiActionSecurityHandler(sharesOutActions, cachedAssetOperations, cachedTransactionCategoryOperations, buyDetailHandler));
        securityHandlers.put("Sell", new MultiActionSecurityHandler(sharesOutActions, cachedAssetOperations, cachedTransactionCategoryOperations, sellDetailHandler));
        securityHandlers.put("Div", new MultiActionSecurityHandler(sharesOutActions, cachedAssetOperations, cachedTransactionCategoryOperations, dividendDetailHandler));
        securityHandlers.put("IntInc", new MultiActionSecurityHandler(sharesOutActions, cachedAssetOperations, cachedTransactionCategoryOperations, interestDetailHandler));
        securityHandlers.put("CGShort", new MultiActionSecurityHandler(sharesOutActions, cachedAssetOperations, cachedTransactionCategoryOperations, stcgDetailHandler));
        securityHandlers.put("CGMid", new MultiActionSecurityHandler(sharesOutActions, cachedAssetOperations, cachedTransactionCategoryOperations, mtcgDetailHandler));
        securityHandlers.put("CGLong", new MultiActionSecurityHandler(sharesOutActions, cachedAssetOperations, cachedTransactionCategoryOperations, ltcgDetailHandler));
        securityHandlers.put("ShrsIn", new MultiActionSecurityHandler(sharesOutActions, cachedAssetOperations, cachedTransactionCategoryOperations, shrsInDetailHandler));
        securityHandlers.put("ShrsOut", new MultiActionSecurityHandler(sharesOutActions, cachedAssetOperations, cachedTransactionCategoryOperations, shrsOutDetailHandler));
        securityHandlers.put("MiscInc", new MultiActionSecurityHandler(sharesOutActions, cachedAssetOperations, cachedTransactionCategoryOperations, miscIncDetailHandler));
        securityHandlers.put("MiscExp", new MultiActionSecurityHandler(sharesOutActions, cachedAssetOperations, cachedTransactionCategoryOperations, miscExpDetailHandler));
        securityHandlers.put("XIn", new TransferHandler(cachedAccountOperations, serviceLocator.getTransactionGroupOperations(), getPayeeCache(), transferDetailCache));
        securityHandlers.put("XOut", new TransferHandler(cachedAccountOperations, serviceLocator.getTransactionGroupOperations(), getPayeeCache(), transferDetailCache, false));
        securityHandlers.put("StkSplit", new StockSplitSecurityHandler(cachedAssetOperations));
        securityTransactionConverter = new SecurityTransactionConverter(serviceLocator.getTransactionService(), securityHandlers);
    }

    public TransferDetailCache getTransferDetailCache() {
        return transferDetailCache;
    }

    public AccountConverter getAccountConverter() {
        if (accountConverter == null) {
            buildAccountConverter();
        }
        return accountConverter;
    }

    public CategoryConverter getCategoryConverter() {
        if (categoryConverter == null) {
            categoryConverter = new CategoryConverter(serviceLocator.getTransactionCategoryOperations());
        }
        return categoryConverter;
    }

    public ClassConverter getClassConverter() {
        if (classConverter == null) {
            classConverter = new ClassConverter(serviceLocator.getTransactionGroupOperations());
        }
        return classConverter;
    }

    public SecurityConverter getSecurityConverter() {
        if (securityConverter == null) {
            securityConverter = new SecurityConverter(cachedAssetOperations);
        }
        return securityConverter;
    }

    public MoneyTransactionConverter getMoneyTransactionConverter() {
        if (moneyTransactionConverter == null) {
            moneyTransactionConverter = new MoneyTransactionConverter(cachedTransactionCategoryOperations, serviceLocator.getTransactionGroupOperations(),
                    getPayeeCache(), serviceLocator.getTransactionService(), cachedAccountOperations, transferDetailCache);
        }
        return moneyTransactionConverter;
    }

    public SecurityTransactionHandler getSecurityHandler(String code) {
        getSecurityTransactionConverter();
        return securityHandlers.get(code);
    }

    public SecurityTransactionConverter getSecurityTransactionConverter() {
        if (securityTransactionConverter == null) {
            buildSecurityTransactionConverter();
        }
        return securityTransactionConverter;
    }

    public FileImport newQifImport() {
        QifImport qifImport = new QifImport(Arrays.asList(getAccountConverter(), getCategoryConverter(), getClassConverter(),
                getSecurityConverter(), getMoneyTransactionConverter(), getSecurityTransactionConverter()));
        return serviceLocator.transactional(qifImport, FileImport.class);
    }

    public FileImport newTxfImport(LotAllocationDialog lotAllocationDialog) {
        CapitalGainImport txfImport = new CapitalGainImport(serviceLocator.getTransactionService(), lotAllocationDialog);
        return serviceLocator.transactional(txfImport, FileImport.class);
    }

    @SuppressWarnings("unchecked")
    private <T> T applyCachingInterceptor(T target, Class<T> iface) {
        return (T) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { iface }, new CachingMethodInterceptor(target));
    }
}