#ifndef GROUPTABLEMODEL_H
#define GROUPTABLEMODEL_H

#include "podtablemodel.h"
#include "service/model/transactiongroup.h"
#include "ui/model/groupstore.h"

class GroupTableModel : public PodTableModel<TransactionGroup, GroupStore> {
public:
    GroupTableModel(GroupStore *groupStore);
};

#endif // GROUPTABLEMODEL_H
