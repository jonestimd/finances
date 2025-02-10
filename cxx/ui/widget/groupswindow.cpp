#include "groupswindow.h"
#include "statusmessage.h"
#include "ui/widget/settings.h"
#include <QCloseEvent>

#define SETTINGS_GROUP "groups"

GroupsWindow::GroupsWindow(DataStore *dataStore)
    : AppWindow{tr("Groups"), new GroupTableModel(dataStore->groupStore), new QTableView()}
    , store{dataStore->groupStore}
{
    setWindowTitle(tr("%1 - Groups[*]").arg(dataStore->connectionName()));

    connect(store, SIGNAL(valuesLoaded(QList<qlonglong>)), this, SLOT(setGroups(QList<qlonglong>)));

    if (store->load(&entityView, tr(LOADING_GROUPS))) model()->setRows(store->ids());

    settings::restoreWindowState(SETTINGS_GROUP, this, QSize{400, 500}, &entityView);
}

GroupTableModel *GroupsWindow::model() {
    return entityView.model<GroupTableModel>();
}

void GroupsWindow::loadData() {
    if (entityView.confirmLoadData()) store->load(&entityView, tr(LOADING_GROUPS), true);
}

void GroupsWindow::saveData() {
    entityView.disableUi(tr(SAVING_GROUPS));
    store->update(this, model());
}

void GroupsWindow::setGroups(const QList<qlonglong> groupIds) {
    model()->setRows(groupIds);
    entityView.enableUi();
}

const char *GroupsWindow::settingsGroup() const {
    return SETTINGS_GROUP;
}

