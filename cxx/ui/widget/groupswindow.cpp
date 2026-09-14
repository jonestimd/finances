#include "groupswindow.h"
#include "statusmessage.h"
#include "ui/widget/dialog.h"
#include "ui/widget/settings.h"
#include <QCloseEvent>

#define SETTINGS_GROUP "groups"

GroupsWindow::GroupsWindow(DataStore *dataStore)
    : EntityWindow{tr("Groups"), new GroupTableModel(dataStore->groupStore), new QTableView(), &dataStore->messageStore}
    , store{dataStore->groupStore}
{
    setWindowTitle(tr("%1 - Groups[*]").arg(dataStore->connectionName()));

    connect(store, SIGNAL(valuesLoaded(QList<domain_id>)), this, SLOT(setGroups(QList<domain_id>)));

    if (store->load(&entityView, tr(LOADING_GROUPS))) model()->setRows(store->ids());

    setProperty(SETTINGS_GROUP_PROP, SETTINGS_GROUP);
    settings::restoreWindowState(SETTINGS_GROUP, this, QSize{400, 500}, &entityView);
}

GroupsWindow::~GroupsWindow() {
    delete model();
}

void GroupsWindow::loadData() {
    if (dialog::confirmDiscardChanges(this, model())) store->load(&entityView, tr(LOADING_GROUPS), true);
}

void GroupsWindow::saveData() {
    store->update(this, model(), tr(SAVING_GROUPS));
}

void GroupsWindow::setGroups(const QList<domain_id> groupIds) {
    model()->setRows(groupIds);
}
