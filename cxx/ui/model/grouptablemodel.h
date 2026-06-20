#ifndef GROUPTABLEMODEL_H
#define GROUPTABLEMODEL_H

#include "podtablemodel.h"
#include "service/groupservice.h"
#include "ui/model/datastore.h"

class GroupTableModel : public PodTableModel<TransactionGroup, GroupService> {
public:
    GroupTableModel(GroupStore *groupStore);
};

#endif // GROUPTABLEMODEL_H
