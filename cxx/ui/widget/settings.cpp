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
    settings->setValue("width", widget->width());
    settings->setValue("height", widget->height());
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
    if (!geometry.isValid() || ! widget->restoreGeometry(geometry.toByteArray())) {
        auto width = settings->value(group + "/width", QVariant{defaultSize.width()});
        auto height = settings->value(group + "/height", QVariant{defaultSize.height()});
        widget->resize(QSize{width.toInt(), height.toInt()});
    }

    if (tableSort) tableSort->restore(group, settings);
}
