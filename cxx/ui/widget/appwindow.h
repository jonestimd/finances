#ifndef APPWINDOW_H
#define APPWINDOW_H

#include "entityview.h"
#include "ui/model/adapteritemmodel.h"
#include "ui/model/statusmessagestore.h"
#include <QBoxLayout>
#include <QDialog>
#include <QHeaderView>
#include <QKeyEvent>
#include <QMainWindow>
#include <QTableView>
#include <QTreeView>

class AppWindow : public QMainWindow {
    Q_OBJECT
protected:
    EntityView entityView;

    explicit AppWindow(const QString &entityName, AdapterItemModel *model, QAbstractItemView *itemView, QHeaderView *viewHeader, StatusMessageStore* messageStore);

public:
    explicit AppWindow(const QString &entityName, AdapterItemModel *model, QTableView *itemView, StatusMessageStore* messageStore);
    explicit AppWindow(const QString &entityName, AdapterItemModel *model, QTreeView *itemView, StatusMessageStore* messageStore);

    Q_INVOKABLE virtual void loadData() = 0;
    Q_INVOKABLE virtual void saveData() = 0;

    void show();

signals:
    void opened(AppWindow*);
    void closed(AppWindow*);

protected:
    virtual const char *settingsGroup() const = 0;

    void closeEvent(QCloseEvent *event) override;
    void keyPressEvent(QKeyEvent *event) override;
};

#endif // APPWINDOW_H
