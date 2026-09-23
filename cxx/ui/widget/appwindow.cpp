#include "appwindow.h"
#include "settings.h"
#include "ui/widget/dialog.h"

AppWindow::AppWindow(QWidget *parent) : QMainWindow{parent} {}

void AppWindow::saveData() {
    qCritical("saveData not implemented");
}

void AppWindow::closeEvent(QCloseEvent *event) {
    emit closed(this);
}

////////////// EntityDialog //////////////

EntityDialog::EntityDialog(QMainWindow *parent, const QString &entityName, const char *settingsGroup, ChangeTrackingItemModel *model,
                           QTableView *itemView, StatusMessageStore *messageStore)
    : QDialog{parent}
    , layout{this}
    , entityView{this, messageStore, model, itemView, itemView->horizontalHeader(), entityName}
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
    auto model = entityView.model<ChangeTrackingItemModel>();
    if (model && event->key() == Qt::Key_Escape && !dialog::confirmDiscardChanges(this, model)) return;
    QDialog::keyPressEvent(event);
}
