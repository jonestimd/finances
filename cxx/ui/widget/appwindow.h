#ifndef APPWINDOW_H
#define APPWINDOW_H

#include "entityview.h"
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
    Q_INVOKABLE virtual void saveData();

signals:
    void closed(AppWindow*);

protected:
    void closeEvent(QCloseEvent *event) override;
};

template<class Model>
requires std::is_base_of_v<QAbstractItemModel, Model>
class EntityWindow : public AppWindow {
protected:
    EntityView entityView;

    explicit EntityWindow(const QString &entityName, Model *model, QAbstractItemView *itemView,
                          QHeaderView *viewHeader, StatusMessageStore* messageStore)
        : AppWindow{}
        , entityView{this, messageStore, model, itemView, viewHeader, entityName}
    {
        addToolBar(&entityView.toolbar);
        setCentralWidget(itemView);
        setStatusBar(&entityView.statusBar);
    }

    EntityWindow(const QString &entityName, Model *model, QTableView *itemView, StatusMessageStore* messageStore)
        : EntityWindow{entityName, model, itemView, itemView->horizontalHeader(), messageStore} {}
    EntityWindow(const QString &entityName, Model *model, QTreeView *itemView, StatusMessageStore* messageStore)
        : EntityWindow{entityName, model, itemView, itemView->header(), messageStore}
    {
        using enum QAbstractItemView::EditTrigger;
        itemView->setSelectionBehavior(QAbstractItemView::SelectItems);
        itemView->setEditTriggers(AllEditTriggers ^ CurrentChanged);
    }

public:
    Model* model() {
        return qobject_cast<Model*>(entityView.sortModel->sourceModel());
    }

    const Model* model() const {
        return qobject_cast<Model*>(entityView.sortModel->sourceModel());
    }
};

class EntityDialog : public QDialog {
    Q_OBJECT
protected:
    QVBoxLayout layout;
    EntityView entityView;

    explicit EntityDialog(QMainWindow* parent, const QString& entityName, const char* settingsGroup, ChangeTrackingItemModel* model,
                          QTableView* itemView, StatusMessageStore* messageStore);

    void keyPressEvent(QKeyEvent *event) override;

public:
    Q_INVOKABLE virtual void loadData() = 0;
    Q_INVOKABLE virtual void saveData() = 0;
};

#endif // APPWINDOW_H
