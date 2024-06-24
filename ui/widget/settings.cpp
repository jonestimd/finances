#include "settings.h"
#include "../finances.h"
#include <QHeaderView>

QSettings *appSettings() {
    return static_cast<finances::App*>(QApplication::instance())->settings;
}

void settings::saveWindowState(const char *group, QWidget *widget, TableSort *tableSort) {
    auto settings = appSettings();

    settings->beginGroup(group);
    settings->setValue("geometry", widget->saveGeometry());
    if (tableSort) tableSort->saveSort(settings);
    settings->endGroup();

    if (tableSort) tableSort->saveSizes(group, settings);
}

int columnIndex(QAbstractItemModel *model, const QString name) {
    auto count = model->columnCount();
    for (int i = 0; i < count; ++i) {
        auto header = model->headerData(i, Qt::Horizontal, Qt::DisplayRole);
        if (header == name) return i;
    }
    return -1;
}

void settings::restoreWindowState(QString group, QWidget *widget, QSize defaultSize, TableSort *tableSort) {
    auto settings = appSettings();

    auto geometry = settings->value(group + "/geometry", QVariant{});
    if (geometry.isValid()) widget->restoreGeometry(geometry.toByteArray());
    else widget->resize(defaultSize);

    if (tableSort) tableSort->restore(group, settings);
}
