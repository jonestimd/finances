#include "groupswindow.h"
#include "statusmessage.h"
#include "ui/widget/settings.h"
#include <QCloseEvent>

#define SETTINGS_GROUP "groups"

GroupsWindow::GroupsWindow(DataStore *dataStore)
    : AppWindow{tr("Groups"), new GroupTableModel(dataStore->groupStore), new QTableView(), &dataStore->messageStore}
    , store{dataStore->groupStore}
{
    setWindowTitle(tr("%1 - Groups[*]").arg(dataStore->connectionName()));

    connect(store, SIGNAL(valuesLoaded(QList<domain_id>)), this, SLOT(setGroups(QList<domain_id>)));

    if (store->load(&entityView, tr(LOADING_GROUPS))) model()->setRows(store->ids());

    settings::restoreWindowState(SETTINGS_GROUP, this, QSize{400, 500}, &entityView);
}

GroupsWindow::~GroupsWindow() {
    delete model();
}

GroupTableModel *GroupsWindow::model() {
    return entityView.model<GroupTableModel>();
}

void GroupsWindow::loadData() {
    if (entityView.confirmLoadData()) store->load(&entityView, tr(LOADING_GROUPS), true);
}

void GroupsWindow::saveData() {
    store->update(this, model(), tr(SAVING_GROUPS));
}

void GroupsWindow::setGroups(const QList<domain_id> groupIds) {
    model()->setRows(groupIds);
}

const char *GroupsWindow::settingsGroup() const {
    return SETTINGS_GROUP;
}

