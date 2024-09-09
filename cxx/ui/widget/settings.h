#ifndef SETTINGS_H
#define SETTINGS_H

#include "entityview.h"
#include <QWidget>

namespace settings {
    void saveWindowState(const char *group, QWidget *widget, EntityView *tableSort = nullptr);

    void restoreWindowState(QString group, QWidget *widget, QSize defaultSize, EntityView *entityView= nullptr);
}

#endif // SETTINGS_H
