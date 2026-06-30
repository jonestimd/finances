#include "grouptablemodel.h"
#include "columnadapter.h"
#include "numbercolumnadapter.h"
#include "../validation/unique.h"

#define GROUP_NAME_COLUMN 0

GroupTableModel::GroupTableModel(GroupStore *groupStore)
    : PodTableModel<TransactionGroup, GroupStore>{
        groupStore,
        QList<ColumnAdapter<TransactionGroup>*>{
            new FieldColumnAdapter<TransactionGroup, QString>(tr("Name"), &TransactionGroup::name, true, new UniqueValidatorFactory(GROUP_NAME_COLUMN)),
            new FieldColumnAdapter<TransactionGroup, QString>(tr("Description"), &TransactionGroup::description),
            new NumberColumnAdapter<TransactionGroup, int>(tr("Transactions"), &TransactionGroup::details),
        },
    }
{}
