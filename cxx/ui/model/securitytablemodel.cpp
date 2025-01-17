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

SecurityTableModel::SecurityTableModel(SecurityStore *store, QObject *parent)
    : PodTableModel{
        store,
        QList<ColumnAdapter<Security>*>{
            new ColumnAdapter<Security>{tr("Name"), &Security::name, true, new UniqueValidatorFactory(SECURITY_NAME_COLUMN)},
            new ColumnAdapter<Security>{tr("Symbol"), &Security::symbol},
            new EnumColumnAdapter<Security, SecurityType>(tr("Type"), &Security::securityType, &SecurityType::values, requiredValidatorFactory, true),
            new NumberColumnAdapter<Security>(tr("Transactions"), &Security::transactions),
            new AmountColumnAdapter<Security>(tr("Shares"), &Security::shares, securityShares, false),
            new FormatColumnAdapter<Security>{tr("First Acquired"), &Security::firstAcquired, formatDate, false},
            new AmountColumnAdapter<Security>(tr("Cost Basis"), &Security::costBasis, securityDollarAmount, false),
            new AmountColumnAdapter<Security>(tr("Dividends"), &Security::dividends, securityDollarAmount, false),
        },
        parent,
    }
{}
