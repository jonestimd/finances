#include "uitest.h"

#include <QDir>
#include <QSettings>

void ui_test::setConfigHome() {
    QSettings::setPath(QSettings::IniFormat, QSettings::UserScope, QDir{}.absoluteFilePath("config"));
}
