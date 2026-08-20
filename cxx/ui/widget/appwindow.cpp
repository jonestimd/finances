#include "appwindow.h"
#include "settings.h"
#include "ui/widget/dialog.h"

AppWindow::AppWindow(QWidget *parent) : QMainWindow{parent} {}

void AppWindow::closeEvent(QCloseEvent *event) {
    emit closed(this);
}

EntityDialog::EntityDialog(QMainWindow *parent, const QString &entityName, const char *settingsGroup, AdapterItemModel *model,
                           QTableView *itemView, StatusMessageStore *messageStore)
    : QDialog{parent}
    , layout{this}
    , entityView{this, messageStore, model, itemView, entityName}
{
    layout.addWidget(&entityView.toolbar);
    layout.addWidget(itemView);
    layout.addWidget(&entityView.statusBar);
    layout.setSpacing(0);
    layout.setContentsMargins(0, 0, 0, 0);

    setProperty(SETTINGS_GROUP_PROP, settingsGroup);
    settings::restoreWindowState(settingsGroup, this, QSize{400, 500}, &entityView);

    entityView.focusItemView();
}

void EntityDialog::keyPressEvent(QKeyEvent *event) {
    if (event->key() == Qt::Key_Escape && !dialog::confirmDiscardChanges(this, entityView.model())) return;
    QDialog::keyPressEvent(event);
}

ReadOnlyEntityWindow::ReadOnlyEntityWindow(const QString &entityName, QAbstractItemModel *model, QTableView *itemView, StatusMessageStore *messageStore)
    : EntityWindow{entityName, model, itemView, messageStore} {}

ReadOnlyEntityWindow::ReadOnlyEntityWindow(const QString &entityName, QAbstractItemModel *model, QTreeView *itemView, StatusMessageStore *messageStore)
    : EntityWindow{entityName, model, itemView, messageStore} {}

void ReadOnlyEntityWindow::saveData() {}
