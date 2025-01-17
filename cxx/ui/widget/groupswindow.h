#ifndef GROUPSWINDOW_H
#define GROUPSWINDOW_H

#include "entityview.h"
#include "statuswindow.h"
#include "ui/model/datastore.h"
#include "ui/model/grouptablemodel.h"

class GroupsWindow : public StatusWindow {
    Q_OBJECT
    GroupStore *store;
    GroupTableModel model;
    QTableView *itemView{new QTableView(this)};
    EntityView tableSort;

public:
    GroupsWindow(DataStore *dataStore);

public Q_SLOTS:
    void loadGroups();
    void saveGroups();
    void setGroups(const QList<qlonglong> groupIds);

    // QWidget interface
protected:
    void closeEvent(QCloseEvent *event);
    void keyPressEvent(QKeyEvent *event);
};

#endif // GROUPSWINDOW_H
