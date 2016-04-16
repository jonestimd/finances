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
package io.github.jonestimd.finance.domain.transaction;

import java.math.BigDecimal;
import java.util.List;

import com.google.common.collect.ImmutableList;
import io.github.jonestimd.finance.swing.BundleType;

public enum SecurityAction {
    BUY(AmountType.DEBIT_DEPOSIT, CategoryType.ASSET_ACQUISITION),
    SELL(AmountType.DEBIT_DEPOSIT, CategoryType.ASSET_DISPOSITION),
    REINVEST(AmountType.DEBIT_DEPOSIT, CategoryType.ASSET_ACQUISITION), // FIXME negative shares from QIF import???
    SHARES_IN(AmountType.ASSET_VALUE, CategoryType.ASSET_ACQUISITION),
    SHARES_OUT(AmountType.ASSET_VALUE, CategoryType.ASSET_DISPOSITION),
    DIVIDEND(AmountType.DEBIT_DEPOSIT, CategoryType.ASSET_INCOME),
    INTEREST(AmountType.DEBIT_DEPOSIT, CategoryType.ASSET_INCOME), // FIXME should not be a security action
    SHORT_TERM_CAP_GAIN(AmountType.DEBIT_DEPOSIT, CategoryType.ASSET_INCOME),
    MID_TERM_CAP_GAIN(AmountType.DEBIT_DEPOSIT, CategoryType.ASSET_INCOME),
    LONG_TERM_CAP_GAIN(AmountType.DEBIT_DEPOSIT, CategoryType.ASSET_INCOME),
    COMMISSION_AND_FEES(AmountType.DEBIT_DEPOSIT, CategoryType.ASSET_EXPENSE); // FIXME should not be a security action

    public static final List<String> REQUIRES_LOTS = ImmutableList.of(SELL.code(), SHARES_OUT.code());

    private final String code;
    private final AmountType amountType;
    private final CategoryType categoryType;

    SecurityAction(AmountType amountType, CategoryType categoryType) {
        code = BundleType.REFERENCE.getString("securityAction." + name());
        this.amountType = amountType;
        this.categoryType = categoryType;
    }

    public String code() {
        return code;
    }

    public AmountType getAmountType() {
        return amountType;
    }

    public CategoryType getCategoryType() {
        return categoryType;
    }

    public boolean isIncome() {
        return categoryType.isIncome();
    }

    public boolean isSameCode(TransactionCategory category) {
        return category != null && code.equals(category.qualifiedName("\n"));
    }

    public boolean isSecurityRequired() {
        return  this != COMMISSION_AND_FEES && this != INTEREST;
    }

    public boolean isSharesRequired() {
        return categoryType.isAssetQuantity();
    }

    public boolean isValidShares(BigDecimal shares) {
        return shares.signum() == categoryType.getAssetQuantitySign();
    }

    public static boolean isSecurityRequired(TransactionCategory category) {
        SecurityAction action = forCategory(category);
        return action != null && action.isSecurityRequired();
    }

    public static SecurityAction forCategory(TransactionCategory category) {
        if (category != null && category.getParent() == null) {
            String categoryCode = category.getCode();
            for (SecurityAction action : SecurityAction.values()) {
                if (action.code.equals(categoryCode)) {
                    return action;
                }
            }
        }
        return null;
    }
}