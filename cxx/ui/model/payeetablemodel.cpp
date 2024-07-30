#include "payeetablemodel.h"
#include "columnadapter.h"
#include "numbercolumnadapter.h"
#include "../validation/unique.h"

#define PAYEE_NAME_COLUMN 0

PayeeTableModel::PayeeTableModel(DataStore *dataStore, QObject *parent)
    : dataStore{dataStore}
    , PodTableModel<Payee> {
        QList<ColumnAdapter<Payee>*>{
            new ColumnAdapter<Payee>(tr("Name"), &Payee::name, true, new UniqueValidatorFactory(PAYEE_NAME_COLUMN)),
            new NumberColumnAdapter<Payee>(tr("Transactions"), &Payee::transactions),
        },
        parent,
    }
{}
