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

import io.github.jonestimd.finance.swing.BundleType;

public enum CategoryType {
    INCOME(true, false, 0),
    EXPENSE(false, false, 0),
    ASSET_INCOME(true, true, 0),
    ASSET_EXPENSE(false, true, 0),
    ASSET_ACQUISITION(false, true, 1),
    ASSET_DISPOSITION(true, true, -1);

    private final String code;
    private final boolean income;
    private final boolean asset;
    private final int assetQuantitySign;

    CategoryType(boolean income, boolean asset, int assetQuantitySign) {
        this.code = BundleType.REFERENCE.getString("categoryType." + name());
        this.income = income;
        this.asset = asset;
        this.assetQuantitySign = assetQuantitySign;
    }

    public String getCode() {
        return code;
    }

    public boolean isIncome() {
        return income;
    }

    public boolean isAsset() {
        return asset;
    }

    public boolean isAssetQuantity() {
        return assetQuantitySign != 0;
    }

    public int getAssetQuantitySign() {
        return assetQuantitySign;
    }
}
