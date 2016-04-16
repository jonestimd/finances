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

import java.util.Arrays;
import java.util.List;

import io.github.jonestimd.cache.CacheKey;
import io.github.jonestimd.cache.Cacheable;
import io.github.jonestimd.finance.dao.TransactionCategoryDao;
import io.github.jonestimd.finance.dao.TransactionDetailDao;
import io.github.jonestimd.finance.domain.UniqueId;
import io.github.jonestimd.finance.domain.transaction.TransactionCategory;
import io.github.jonestimd.finance.domain.transaction.TransactionCategorySummary;
import io.github.jonestimd.finance.swing.BundleType;
import io.github.jonestimd.util.MessageHelper;
import io.github.jonestimd.util.Streams;
import org.apache.commons.lang.StringUtils;

public class TransactionCategoryOperationsImpl implements TransactionCategoryOperations {
    private final TransactionCategoryDao transactionCategoryDao;
    private final TransactionDetailDao transactionDetailDao;
    private MessageHelper messageHelper;

    public TransactionCategoryOperationsImpl(TransactionCategoryDao TransactionCategoryDao, TransactionDetailDao transactionDetailDao) {
        this.transactionCategoryDao = TransactionCategoryDao;
        this.transactionDetailDao = transactionDetailDao;
        this.messageHelper = new MessageHelper(BundleType.MESSAGES.get(), getClass());
    }

    @Cacheable
    public TransactionCategory getTransactionCategory(String ... codes) {
        return transactionCategoryDao.getTransactionCategory(codes);
    }

    public TransactionCategory getSecurityAction(String code) {
        return transactionCategoryDao.getSecurityAction(code);
    }

    public List<TransactionCategory> getAllTransactionCategories() {
        return transactionCategoryDao.getAll();
    }

    public List<TransactionCategorySummary> getTransactionCategorySummaries() {
        return transactionCategoryDao.getTransactionCategorySummaries();
    }

    @Cacheable
    public TransactionCategory getOrCreateTransactionCategory(String description, boolean income, @CacheKey String ... codes) {
        if (codes == null || codes.length == 0) {
            throw new IllegalArgumentException("codes is required");
        }
        TransactionCategory type = transactionCategoryDao.getTransactionCategory(codes);
        if (type == null) {
            type = new TransactionCategory();
            type.setDescription(description);
            type.setIncome(income);
            type.setCode(codes[codes.length-1]);
            if (codes.length > 1) {
                String[] parentCodes = Arrays.asList(codes).subList(0, codes.length-1).toArray(new String[codes.length-1]);
                TransactionCategory parent = transactionCategoryDao.getTransactionCategory(parentCodes);
                if (parent == null) {
                    throw new IllegalArgumentException(messageHelper.getMessage("unknownTransactionCategory",
                            StringUtils.join(parentCodes, ':')));
                }
                type.setParent(parent);
            }
            type = transactionCategoryDao.save(type);
        }
        return type;
    }

    public <T extends TransactionCategory> T save(T category) {
        return transactionCategoryDao.save(category);
    }

    public <T extends Iterable<TransactionCategory>> T saveAll(T categories) {
        return transactionCategoryDao.saveAll(categories);
    }

    @Override
    public <T extends Iterable<TransactionCategory>> void deleteAll(T categories) {
        transactionCategoryDao.deleteAll(categories);
    }

    @Override
    public List<TransactionCategory> merge(List<TransactionCategory> toReplace, TransactionCategory selection) {
        transactionDetailDao.replaceCategory(toReplace, selection);
        List<Long> parentIds = Streams.map(transactionCategoryDao.getParentCategories(), UniqueId::getId);
        List<TransactionCategory> toDelete = Streams.filter(toReplace, category -> !parentIds.contains(category.getId()));
        if (! toDelete.isEmpty()) {
            transactionCategoryDao.deleteAll(toDelete);
        }
        return toDelete;
    }
}
