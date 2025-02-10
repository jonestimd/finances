#include "appwindow.h"

AppWindow::AppWindow(const QString &entityName, AdapterItemModel *model, QAbstractItemView *itemView, QHeaderView *viewHeader)
    : QMainWindow{}
    , entityView{this, model, itemView, viewHeader, entityName}
{
    addToolBar(&entityView.toolbar);
    setCentralWidget(itemView);
    setStatusBar(&entityView.statusBar);
}

AppWindow::AppWindow(const QString &entityName, AdapterItemModel *model, QTableView *itemView)
    : AppWindow{entityName, model, itemView, itemView->horizontalHeader()}
{
    itemView->resizeColumnsToContents();
    // itemView->verticalHeader()->setDefaultSectionSize(5); // minimize row height
}

AppWindow::AppWindow(const QString &entityName, AdapterItemModel *model, QTreeView *itemView)
    : AppWindow{entityName, model, itemView, itemView->header()}
{
    using enum QAbstractItemView::EditTrigger;
    itemView->setSelectionBehavior(QAbstractItemView::SelectItems);
    itemView->setEditTriggers(AllEditTriggers ^ CurrentChanged);
}

void AppWindow::enableUi() {
    entityView.enableUi();
}

void AppWindow::closeEvent(QCloseEvent *event) {
    entityView.confirmClose(event, settingsGroup());
}

void AppWindow::keyPressEvent(QKeyEvent *event) {
    if (!entityView.focusFilter(event)) QMainWindow::keyPressEvent(event);
}

