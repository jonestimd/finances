#ifndef ENTITY_VIEW_H
#define ENTITY_VIEW_H

#include "filterinput.h"
#include "ui/model/changetrackingitemmodel.h"
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

    template<class Model = QAbstractItemModel>
    inline Model* model() const requires std::is_base_of_v<QAbstractItemModel, Model>{
        return qobject_cast<Model*>(sortModel->sourceModel());
    }

    void addActions(const QList<QAction*> &actions);
    void insertAction(qsizetype index, QAction* action);

    QModelIndex selectedIndex();
    void selectIndex(QModelIndex index);

    void focusItemView();

public Q_SLOTS:
    void showStatusMessage(const QString message);
    void clearStatusMessage();
private Q_SLOTS:
    void showValidation(const QModelIndex& index);

protected:
    /**
     * @brief eventFilter Filters window events to handle "find" shortcut and save window's state when it closes.
     */
    virtual bool eventFilter(QObject* obj, QEvent* event) override;

private:
    void saveSelection(QModelIndex index);
    void restoreSelection();
};

#endif // ENTITY_VIEW_H
