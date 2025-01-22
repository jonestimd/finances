#ifndef ENTITY_VIEW_H
#define ENTITY_VIEW_H

#include "filterinput.h"
#include "statusbar.h"
#include "tableitemdelegate.h"
#include "../model/adapteritemmodel.h"
#include <QStatusBar>
#include <QTableView>
#include <QTreeView>
#include <ui/model/sortfilterproxymodel.h>

class EntityView : public QObject {
    Q_OBJECT
    QWidget *const window;
    TableItemDelegate itemDelegate;

public:
    StatusBar statusBar{};
    QHeaderView *const viewHeader;
    AdapterItemModel *const model;
    QAbstractItemView *const itemView;
    SortFilterProxyModel sortModel;
    FilterInput *const filterInput;
    QToolBar toolbar;
    QAction *const saveAction;

    EntityView(QWidget *window, AdapterItemModel *model, QAbstractItemView *itemView, QHeaderView *viewHeader, const QString &entityName);
    EntityView(QWidget *window, AdapterItemModel *model, QTableView *itemView, const QString &entityName);

    void addActions(const QList<QAction*> &actions);
    void insertAction(qsizetype index, QAction* action);

    QModelIndex selectedIndex();

    bool focusFilter(QKeyEvent *event);

    bool confirmLoadData();
    void confirmClose(QCloseEvent *event, const char *settingsGroup);

    void enableUi();
    void disableUi(const QString &message);
    Q_INVOKABLE void removeMessage(const QString &message);

public Q_SLOTS:
    void dataChanged();
    void showValidation(const QModelIndex &index);
};

#endif // ENTITY_VIEW_H
