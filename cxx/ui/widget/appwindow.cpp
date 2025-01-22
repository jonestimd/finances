#include "appwindow.h"

AppWindow::AppWindow(const QString &entityName, AdapterItemModel *model, QAbstractItemView *itemView, QHeaderView *viewHeader, const char *settingsGroup)
    : QMainWindow{}
    , settingsGroup{settingsGroup}
    , entityView{this, model, itemView, viewHeader, entityName}
{
    addToolBar(&entityView.toolbar);
    setCentralWidget(itemView);
    setStatusBar(&entityView.statusBar);
    model->setParent(this);
}

AppWindow::AppWindow(const QString &entityName, AdapterItemModel *model, QTableView *itemView, const char *settingsGroup)
    : AppWindow{entityName, model, itemView, itemView->horizontalHeader(), settingsGroup}
{
    itemView->resizeColumnsToContents();
    // itemView->verticalHeader()->setDefaultSectionSize(5); // minimize row height
}

AppWindow::AppWindow(const QString &entityName, AdapterItemModel *model, QTreeView *itemView, const char *settingsGroup)
    : AppWindow{entityName, model, itemView, itemView->header(), settingsGroup}
{
    using enum QAbstractItemView::EditTrigger;
    itemView->setSelectionBehavior(QAbstractItemView::SelectItems);
    itemView->setEditTriggers(AllEditTriggers ^ CurrentChanged);
}

void AppWindow::enableUi() {
    entityView.enableUi();
}

void AppWindow::closeEvent(QCloseEvent *event) {
    entityView.confirmClose(event, settingsGroup);
}

void AppWindow::keyPressEvent(QKeyEvent *event) {
    if (!entityView.focusFilter(event)) QMainWindow::keyPressEvent(event);
}

