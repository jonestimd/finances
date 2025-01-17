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

// TODO - break up?
//   - save/restore
//   - actions (toolbar?)
class EntityView : public QObject {
    Q_OBJECT
    QWidget *const window;
    TableItemDelegate itemDelegate;
    StatusBar *statusBar;
    QHeaderView *viewHeader;
    AdapterItemModel *const model; // TODO move to window save/restore class?
    QAbstractItemView *const itemView;

    EntityView(QWidget *window, AdapterItemModel *model, QAbstractItemView *itemView, QHeaderView *viewHeader,
               StatusBar *statusBar, const QString filterLabel, const QString defaultSort,
               const char *saveSlot, const char *loadSlot, QList<QAction*> actions);

public:
    SortFilterProxyModel sortModel;
    FilterInput *const filterInput;
    QToolBar toolbar;
    QAction *const saveAction;
    const QString defaultSort;

    EntityView(QWidget *window, AdapterItemModel *model, QTableView *itemView, StatusBar *statusBar,
               const QString filterLabel, const QString defaultSort,
               const char *saveSlot, const char *loadSlot, QList<QAction*> actions = QList<QAction*>{});

    EntityView(QWidget *window, AdapterItemModel *model, QTreeView *itemView, StatusBar *statusBar,
               const QString filterLabel, const QString defaultSort,
               const char *saveSlot, const char *loadSlot, QList<QAction*> actions = QList<QAction*>{});

    int columnIndex(const QString name) const;

    QModelIndex selectedIndex();

    void enableColumnResize();

    void setColumnResize(const std::vector<int> stretchColumns);

    bool focusFilter(QKeyEvent *event);

    void saveSort(QSettings *settings);

    void saveSizes(QString group, QSettings *settings);

    void restore(QString group, QSettings *settings);

    void startEdit(int rowIndex);

    bool confirmLoadData(QString loadingMessage);

    void enableUi();

private:
    QAction *addAction(const QString text);
    QAction *deleteAction(const QString text, std::function<bool(const QModelIndex &)> enableDelete = nullptr);
    QAction *undoAction();

public Q_SLOTS:
    void dataChanged();
    void showValidation(const QModelIndex &index);
    void addRow();
    void queueDeletes();
    void undoChanges();
};

#endif // ENTITY_VIEW_H
