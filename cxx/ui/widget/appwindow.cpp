#include "appwindow.h"

AppWindow::AppWindow(const QString &entityName, AdapterItemModel *model, QAbstractItemView *itemView, QHeaderView *viewHeader, StatusMessageStore* messageStore)
    : QMainWindow{}
    , entityView{this, messageStore, model, itemView, viewHeader, entityName}
{
    addToolBar(&entityView.toolbar);
    setCentralWidget(itemView);
    setStatusBar(&entityView.statusBar);
}

AppWindow::AppWindow(const QString &entityName, AdapterItemModel *model, QTableView *itemView, StatusMessageStore* messageStore)
    : AppWindow{entityName, model, itemView, itemView->horizontalHeader(), messageStore}
{
    itemView->resizeColumnsToContents();
    // itemView->verticalHeader()->setDefaultSectionSize(5); // minimize row height
}

AppWindow::AppWindow(const QString &entityName, AdapterItemModel *model, QTreeView *itemView, StatusMessageStore* messageStore)
    : AppWindow{entityName, model, itemView, itemView->header(), messageStore}
{
    using enum QAbstractItemView::EditTrigger;
    itemView->setSelectionBehavior(QAbstractItemView::SelectItems);
    itemView->setEditTriggers(AllEditTriggers ^ CurrentChanged);
}

void AppWindow::show() {
    QMainWindow::show();
    emit opened(this);
}

void AppWindow::closeEvent(QCloseEvent *event) {
    entityView.confirmClose(event, settingsGroup());
    if (event->isAccepted()) emit closed(this);
}

void AppWindow::keyPressEvent(QKeyEvent *event) {
    if (!entityView.focusFilter(event)) QMainWindow::keyPressEvent(event);
}
