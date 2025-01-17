#include "groupswindow.h"
#include "ui/widget/dialog.h"
#include "ui/widget/settings.h"
#include <QCloseEvent>

#define LOADING_GROUPS "Loading groups..."
#define SAVING_GROUPS "Saving groups..."
#define SETTINGS_GROUP "groups"

GroupsWindow::GroupsWindow(DataStore *dataStore)
    : StatusWindow{}
    , store{dataStore->groupStore}
    , model{dataStore->groupStore, this}
    , tableSort{this, &model, itemView, &statusBar, tr("Groups"), tr("Name"), SLOT(saveGroups()), SLOT(loadGroups())}
{
    setCentralWidget(itemView);
    setWindowTitle(tr("%1 - Groups[*]").arg(dataStore->connectionName()));
    addToolBar(&tableSort.toolbar);

    connect(store, SIGNAL(valuesLoaded(QList<qlonglong>)), this, SLOT(setGroups(QList<qlonglong>)));

    if (store->load(this)) model.setRows(store->ids());
    else statusBar.addMessage(tr(LOADING_GROUPS));

    tableSort.enableColumnResize();
    settings::restoreWindowState(SETTINGS_GROUP, this, QSize{400, 500}, &tableSort);
}

void GroupsWindow::loadGroups() {
    if (tableSort.confirmLoadData(tr(LOADING_GROUPS))) store->load(this, true);
}

void GroupsWindow::saveGroups() {
    disableUi(tr(SAVING_GROUPS));
    store->update(this, model.unsavedChanges(), model.unsavedAdds(), model.unsavedDeletes());
}

void GroupsWindow::setGroups(const QList<qlonglong> groupIds) {
    model.setRows(groupIds);
    statusBar.removeMessage(tr(LOADING_GROUPS));
    statusBar.removeMessage(tr(SAVING_GROUPS));
    itemView->setEnabled(true);
}

void GroupsWindow::closeEvent(QCloseEvent *event) {
    if (!dialog::confirmDiscardChanges(this, &model)) event->ignore();
    else settings::saveWindowState(SETTINGS_GROUP, this, &tableSort);
}

void GroupsWindow::keyPressEvent(QKeyEvent *event) {
    if (!tableSort.focusFilter(event)) QMainWindow::keyPressEvent(event);
}
