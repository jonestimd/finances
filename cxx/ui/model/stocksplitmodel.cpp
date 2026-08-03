#include "stocksplitmodel.h"
#include "ui/model/amountcolumnadapter.h"
#include "ui/model/formatcolumnadapter.h"
#include "ui/model/formats.h"
#include "ui/validation/numeric.h"
#include "ui/validation/unique.h"

StockSplitTableModel::StockSplitTableModel(const StockSplitStore *store, domain_id securityId, QObject *parent)
    : PodTableModel<StockSplit, StockSplitStore> {
        store,
        QList<ColumnAdapter<StockSplit>*>{
            new FormatColumnAdapter<StockSplit, QDate>(tr("Date"), &StockSplit::date, dateFormat, true, new UniqueValidatorFactory(0)),
            new AmountColumnAdapter<StockSplit, QDecNumber>(tr("Shares Divider"), &StockSplit::sharesIn,
                securityShares, true, new NumberValidatorFactory{SHARE_DECIMALS, true, 0, false}),
            new AmountColumnAdapter<StockSplit, QDecNumber>(tr("Shares Multiplier"), &StockSplit::sharesOut,
                securityShares, true, new NumberValidatorFactory{SHARE_DECIMALS, true, 0, false}),
        },
        parent,
    }
    , securityId{securityId}
{}

void StockSplitTableModel::setRows() {
    PodTableModel::setRows(store->getSplits(securityId));
}

StockSplit *StockSplitTableModel::newRow() {
    return new StockSplit{securityId};
}
