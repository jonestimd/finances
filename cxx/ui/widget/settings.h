#ifndef SETTINGS_H
#define SETTINGS_H

#include "entityview.h"
#include "service/database/connectionpool.h"
#include <QWidget>

namespace settings {
    void saveSort(QSettings *settings, const QString &group, QAbstractItemModel* model, QHeaderView *viewHeader);

    void saveSizes(QSettings *settings, const QString &group, QAbstractItemModel* model, QHeaderView *viewHeader);

    void saveWindowState(const QString &group, QWidget *widget, QAbstractItemModel* model = nullptr, QHeaderView *viewHeader = nullptr);

    void saveWindowState(const char *group, QWidget *widget, EntityView *tableSort = nullptr);

    void restoreWindowState(const QString &group, QWidget *widget, QSize defaultSize, QAbstractItemModel *model, QHeaderView *viewHeader);

    void restoreWindowState(const QString &group, QWidget *widget, QSize defaultSize, EntityView *entityView= nullptr);

    ConnectionSettings connectionSettings(const QString &name = "default");
}

#endif // SETTINGS_H
