#include "groupswindow.h"
#include "ui/widget/settings.h"
#include <QCloseEvent>

#define LOADING_GROUPS "Loading groups..."
#define SAVING_GROUPS "Saving groups..."
#define SETTINGS_GROUP "groups"

GroupsWindow::GroupsWindow(DataStore *dataStore)
    : AppWindow{tr("Groups"), new GroupTableModel(dataStore->groupStore), new QTableView(), SETTINGS_GROUP}
    , store{dataStore->groupStore}
{
    setWindowTitle(tr("%1 - Groups[*]").arg(dataStore->connectionName()));

    connect(store, SIGNAL(valuesLoaded(QList<qlonglong>)), this, SLOT(setGroups(QList<qlonglong>)));

    if (store->load(this)) model()->setRows(store->ids());
    else disableUi(tr(LOADING_GROUPS));

    settings::restoreWindowState(SETTINGS_GROUP, this, QSize{400, 500}, &entityView);
}

GroupTableModel *GroupsWindow::model() {
    return static_cast<GroupTableModel*>(entityView.model);
}

void GroupsWindow::loadData() {
    if (entityView.confirmLoadData(tr(LOADING_GROUPS))) store->load(this, true);
}

void GroupsWindow::saveData() {
    disableUi(tr(SAVING_GROUPS));
    store->update(this, model()->unsavedChanges(), model()->unsavedAdds(), model()->unsavedDeletes());
}

void GroupsWindow::setGroups(const QList<qlonglong> groupIds) {
    model()->setRows(groupIds);
    entityView.enableUi();
}
