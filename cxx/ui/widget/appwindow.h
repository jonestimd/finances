#ifndef APPWINDOW_H
#define APPWINDOW_H

#include "entityview.h"
#include "ui/model/adapteritemmodel.h"
#include "ui/store/statusmessagestore.h"
#include <QBoxLayout>
#include <QDialog>
#include <QHeaderView>
#include <QKeyEvent>
#include <QMainWindow>
#include <QTableView>
#include <QTreeView>

class AppWindow : public QMainWindow {
    Q_OBJECT
public:
    explicit AppWindow(QWidget* parent = nullptr);

    Q_INVOKABLE virtual void loadData() = 0;
    Q_INVOKABLE virtual void saveData() = 0;

signals:
    void closed(AppWindow*);

protected:
    void closeEvent(QCloseEvent *event) override;
};

template<class View = EditEntityView, class Model = AdapterItemModel>
    requires std::is_base_of_v<EntityView, View> && std::is_base_of_v<QAbstractItemModel, Model>
class EntityWindow : public AppWindow {
protected:
    View entityView;

    explicit EntityWindow(const QString &entityName, Model *model, QAbstractItemView *itemView,
                          QHeaderView *viewHeader, StatusMessageStore* messageStore)
        : AppWindow{}
        , entityView{this, messageStore, model, itemView, viewHeader, entityName}
    {
        addToolBar(&entityView.toolbar);
        setCentralWidget(itemView);
        setStatusBar(&entityView.statusBar);
    }

    explicit EntityWindow(const QString &entityName, Model *model, QTableView *itemView, StatusMessageStore* messageStore)
        : EntityWindow{entityName, model, itemView, itemView->horizontalHeader(), messageStore} {}
    explicit EntityWindow(const QString &entityName, Model *model, QTreeView *itemView, StatusMessageStore* messageStore)
        : EntityWindow{entityName, model, itemView, itemView->header(), messageStore}
    {
        using enum QAbstractItemView::EditTrigger;
        itemView->setSelectionBehavior(QAbstractItemView::SelectItems);
        itemView->setEditTriggers(AllEditTriggers ^ CurrentChanged);
    }
};

class ReadOnlyEntityWindow : public EntityWindow<EntityView, QAbstractItemModel> {
protected:
    explicit ReadOnlyEntityWindow(const QString& entityName, QAbstractItemModel* model, QTableView* itemView, StatusMessageStore* messageStore);
    explicit ReadOnlyEntityWindow(const QString& entityName, QAbstractItemModel* model, QTreeView* itemView, StatusMessageStore* messageStore);

public:
    Q_INVOKABLE void saveData() override;
};

class EntityDialog : public QDialog {
    Q_OBJECT
protected:
    QVBoxLayout layout;
    EditEntityView entityView;

    explicit EntityDialog(QMainWindow* parent, const QString& entityName, const char* settingsGroup, AdapterItemModel* model,
                          QTableView* itemView, StatusMessageStore* messageStore);

    void keyPressEvent(QKeyEvent *event) override;

public:
    Q_INVOKABLE virtual void loadData() = 0;
    Q_INVOKABLE virtual void saveData() = 0;
};

#endif // APPWINDOW_H
