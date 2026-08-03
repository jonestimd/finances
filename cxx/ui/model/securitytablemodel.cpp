#include "securitytablemodel.h"
#include "numbercolumnadapter.h"
#include "enumcolumnadapter.h"
#include "amountcolumnadapter.h"
#include "formatcolumnadapter.h"
#include "formats.h"
#include "service/model/securitytype.h"
#include "ui/model/formats.h"
#include "ui/validation/required.h"
#include "ui/validation/unique.h"

#define SECURITY_NAME_COLUMN 0
#define SHARES_TITLE "Shares"

SecurityTableModel::SecurityTableModel(SecurityStore *store)
    : PodTableModel{
        store,
        QList<ColumnAdapter<Security>*>{
            new FieldColumnAdapter<Security, QString>{tr("Name"), &Security::name, true, new UniqueValidatorFactory(SECURITY_NAME_COLUMN)},
            new FieldColumnAdapter<Security, QString>{tr("Symbol"), &Security::symbol},
            new EnumColumnAdapter<Security, SecurityType>(tr("Type"), &Security::securityType, &SecurityType::values, requiredValidatorFactory, true),
            new NumberColumnAdapter<Security, int>(tr("Transactions"), &Security::transactions),
            new AmountColumnAdapter<Security, QDecNumber>(tr(SHARES_TITLE), &Security::shares, securityShares, false),
            new FormatColumnAdapter<Security, std::optional<QDate>>{tr("First Acquired"), &Security::firstAcquired, dateFormat, false},
            new AmountColumnAdapter<Security, QDecNumber>(tr("Cost Basis"), &Security::costBasis, dollarFormat, false),
            new AmountColumnAdapter<Security, QDecNumber>(tr("Dividends"), &Security::dividends, dollarFormat, false),
        },
    }
    , sharesColumn{columnIndex(tr(SHARES_TITLE))}
{
    connect(&store->stockSplitStore, SIGNAL(valuesLoaded(QList<domain_id>)), this, SLOT(splitsLoaded()));
}

void SecurityTableModel::splitsLoaded() {
    auto rows = rowCount();
    if (rows > 0) emit dataChanged(index(0, sharesColumn), index(rows-1, sharesColumn));
}
