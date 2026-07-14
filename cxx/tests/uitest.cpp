#include "uitest.h"

#include <QDir>
#include <QSettings>

void uitest::setConfigHome() {
    QSettings::setPath(QSettings::IniFormat, QSettings::UserScope, QDir{}.absoluteFilePath("config"));
}
