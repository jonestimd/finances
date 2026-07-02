#ifndef GROUPSWINDOW_H
#define GROUPSWINDOW_H

#include "appwindow.h"
#include "ui/model/datastore.h"
#include "ui/model/grouptablemodel.h"

class GroupsWindow : public AppWindow {
    Q_OBJECT
    GroupStore *store;

public:
    GroupsWindow(DataStore *dataStore);
    ~GroupsWindow();

    GroupTableModel *model();

    Q_INVOKABLE void loadData() override;
    Q_INVOKABLE void saveData() override;

public Q_SLOTS:
    void setGroups(const QList<domain_id> groupIds);

protected:
    const char *settingsGroup() const override;
};

#endif // GROUPSWINDOW_H
