#ifndef SETTINGS_H
#define SETTINGS_H

#include "entitytable.h"
#include <QWidget>

namespace settings {
    void saveWindowState(const char *group, QWidget *widget, EntityTable *tableSort = nullptr);

    void restoreWindowState(QString group, QWidget *widget, QSize defaultSize, EntityTable *tableSort = nullptr);
}

#endif // SETTINGS_H
