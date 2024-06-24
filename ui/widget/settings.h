#ifndef SETTINGS_H
#define SETTINGS_H

#include "tablesort.h"
#include <QWidget>

namespace settings {
    void saveWindowState(const char *group, QWidget *widget, TableSort *tableSort = nullptr);

    void restoreWindowState(QString group, QWidget *widget, QSize defaultSize, TableSort *tableSort = nullptr);
}

#endif // SETTINGS_H
