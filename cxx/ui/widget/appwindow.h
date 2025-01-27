#ifndef APPWINDOW_H
#define APPWINDOW_H

#include "ui/model/adapteritemmodel.h"
#include "entityview.h"
#include <QBoxLayout>
#include <QDialog>
#include <QHeaderView>
#include <QKeyEvent>
#include <QMainWindow>
#include <QTableView>
#include <QTreeView>

class AppWindow : public QMainWindow {
    Q_OBJECT
    const char *const settingsGroup;

protected:
    EntityView entityView;

    explicit AppWindow(const QString &entityName, AdapterItemModel *model, QAbstractItemView *itemView, QHeaderView *viewHeader, const char *settingsGroup);

public:
    explicit AppWindow(const QString &entityName, AdapterItemModel *model, QTableView *itemView, const char *settingsGroup);
    explicit AppWindow(const QString &entityName, AdapterItemModel *model, QTreeView *itemView, const char *settingsGroup);

    Q_INVOKABLE virtual void loadData() = 0;
    Q_INVOKABLE virtual void saveData() = 0;
    Q_INVOKABLE void enableUi();
    Q_INVOKABLE void disableUi(const QString &message);

protected:
    void removeMessage(const QString &message);

    void closeEvent(QCloseEvent *event) override;
    void keyPressEvent(QKeyEvent *event) override;
};

#endif // APPWINDOW_H
