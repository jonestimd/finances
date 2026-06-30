#include "payeetablemodel.h"
#include "columnadapter.h"
#include "numbercolumnadapter.h"
#include "../validation/unique.h"

#define PAYEE_NAME_COLUMN 0

PayeeTableModel::PayeeTableModel(PayeeStore *payeeStore)
    : PodTableModel<Payee, PayeeStore> {
        payeeStore,
        QList<ColumnAdapter<Payee>*>{
            new FieldColumnAdapter<Payee, QString>(tr("Name"), &Payee::name, true, new UniqueValidatorFactory(PAYEE_NAME_COLUMN)),
            new NumberColumnAdapter<Payee, int>(tr("Transactions"), &Payee::transactions),
        },
    }
{}
