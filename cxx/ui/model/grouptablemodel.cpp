#include "grouptablemodel.h"
#include "columnadapter.h"
#include "numbercolumnadapter.h"
#include "../validation/unique.h"

#define GROUP_NAME_COLUMN 0

GroupTableModel::GroupTableModel(GroupStore *groupStore, QObject *parent)
    : PodTableModel<TransactionGroup, GroupService>{
        groupStore,
        QList<ColumnAdapter<TransactionGroup>*>{
            new ColumnAdapter<TransactionGroup>(tr("Name"), &TransactionGroup::name, true, new UniqueValidatorFactory(GROUP_NAME_COLUMN)),
            new ColumnAdapter<TransactionGroup>(tr("Description"), &TransactionGroup::description),
            new NumberColumnAdapter<TransactionGroup>(tr("Transactions"), &TransactionGroup::transactions),
        },
        parent,
    }
{}
