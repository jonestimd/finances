#ifndef ENTITY_VIEW_H
#define ENTITY_VIEW_H

#include "filterinput.h"
#include "tableitemdelegate.h"
#include "ui/model/adapteritemmodel.h"
#include "ui/model/sortfilterproxymodel.h"
#include "ui/store/statusmessagestore.h"
#include <QStatusBar>
#include <QTableView>
#include <QTreeView>

#define SETTINGS_GROUP_PROP "settingsGroup"

class EntityView : public QObject {
    Q_OBJECT
protected:
    QWidget *const window;
    TableItemDelegate itemDelegate;
    /** @brief Index of the last selected cell. */
    QList<int> lastSelection;
    int lastColumn;

public:
    QStatusBar statusBar{};
    QHeaderView *const viewHeader;
    QAbstractItemView *const itemView;
    SortFilterProxyModel* sortModel;
    FilterInput *const filterInput;
    QToolBar toolbar;

    EntityView(QWidget *window, StatusMessageStore* messageStore, QAbstractItemModel *model,
               QAbstractItemView *itemView, QHeaderView *viewHeader, const QString &entityName);

    inline QAbstractItemModel* model() const {
        return sortModel->sourceModel();
    }
    void setModel(QAbstractItemModel* model);

    void addActions(const QList<QAction*> &actions);
    void insertAction(qsizetype index, QAction* action);

    QModelIndex selectedIndex();

    void focusItemView();

public Q_SLOTS:
    void showStatusMessage(const QString message);
    void clearStatusMessage();

protected:
    /**
     * @brief eventFilter Filters window events to handle "find" shortcut and save window's state when it closes.
     */
    virtual bool eventFilter(QObject* obj, QEvent* event) override;

private:
    void restoreSelection();
};

class EditEntityView : public EntityView {
    Q_OBJECT

public:
    QAction *const saveAction;

    EditEntityView(QWidget *window, StatusMessageStore* messageStore, AdapterItemModel *model,
               QAbstractItemView *itemView, QHeaderView *viewHeader, const QString &entityName);
    EditEntityView(QWidget *window, StatusMessageStore* messageStore, AdapterItemModel *model,
               QTableView *itemView, const QString &entityName);

    template<class T = AdapterItemModel>
    inline T *model() const {
        return static_cast<T*>(sortModel->sourceModel());
    }
    void setModel(AdapterItemModel* model);

    bool confirmLoadData();
    void confirmClose(QCloseEvent *event, const char *settingsGroup);

public Q_SLOTS:
    void dataChanged();
    void showValidation(const QModelIndex &index);

protected:
    /**
     * @brief eventFilter Confirms closing the window when there are unsaved changes.
     */
    virtual bool eventFilter(QObject* obj, QEvent* event) override;
};

#endif // ENTITY_VIEW_H
