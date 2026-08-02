#ifndef GROUPSWINDOW_H
#define GROUPSWINDOW_H

#include "appwindow.h"
#include "ui/store/datastore.h"
#include "ui/model/grouptablemodel.h"

class GroupsWindow : public EntityWindow<> {
    Q_OBJECT
    GroupStore *store;

public:
    GroupsWindow(DataStore *dataStore);
    ~GroupsWindow();

    GroupTableModel *model();

    void loadData() override;
    void saveData() override;

public Q_SLOTS:
    void setGroups(const QList<domain_id> groupIds);
};

#endif // GROUPSWINDOW_H
