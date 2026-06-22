#ifndef ENTITY_VIEW_H
#define ENTITY_VIEW_H

#include "filterinput.h"
#include "tableitemdelegate.h"
#include "ui/model/adapteritemmodel.h"
#include "ui/model/statusmessagestore.h"
#include <QStatusBar>
#include <QTableView>
#include <QTreeView>
#include <ui/model/sortfilterproxymodel.h>

class EntityView : public QObject {
    Q_OBJECT
    QWidget *const window;
    TableItemDelegate itemDelegate;
    /** @brief Indexes of the last selected row and its parents. */
    QList<int> lastSelection;

public:
    QStatusBar statusBar{};
    QHeaderView *const viewHeader;
    QAbstractItemView *const itemView;
    SortFilterProxyModel sortModel;
    FilterInput *const filterInput;
    QToolBar toolbar;
    QAction *const saveAction;

    EntityView(QWidget *window, StatusMessageStore* messageStore, AdapterItemModel *model, QAbstractItemView *itemView, QHeaderView *viewHeader, const QString &entityName);
    EntityView(QWidget *window, StatusMessageStore* messageStore, AdapterItemModel *model, QTableView *itemView, const QString &entityName);

    template<class T = AdapterItemModel>
    T *model() const {
        return static_cast<T*>(sortModel.sourceModel());
    }

    void addActions(const QList<QAction*> &actions);
    void insertAction(qsizetype index, QAction* action);

    QModelIndex selectedIndex();

    bool focusFilter(QKeyEvent *event);

    bool confirmLoadData();
    void confirmClose(QCloseEvent *event, const char *settingsGroup);

public Q_SLOTS:
    void showStatusMessage(const QString message);
    void clearStatusMessage();

public:
    void focusItemView();

private:
    void restoreSelection();

public Q_SLOTS:
    void dataChanged();
    void showValidation(const QModelIndex &index);
};

#endif // ENTITY_VIEW_H
