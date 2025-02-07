#include "settings.h"
#include <QHeaderView>

#define APP_NAME "finances"

Q_GLOBAL_STATIC(QSettings, appSettings, QSettings::IniFormat, QSettings::UserScope, APP_NAME, APP_NAME);

void settings::saveSizes(QSettings *settings, const QString &group, QAbstractItemModel *model, QHeaderView *viewHeader) {
    settings->beginGroup(QString(group).append(".columns"));
    for (int section = 0; section < viewHeader->count(); ++section) {
        auto name = model->headerData(section, Qt::Horizontal).toString();
        auto width = viewHeader->sectionSize(section);
        settings->setValue(name + ".width", width);
        settings->setValue(name + ".pos", viewHeader->visualIndex(section));
    }
    settings->endGroup();
}

void settings::saveSort(QSettings *settings, const QString &group, QAbstractItemModel *model, QHeaderView *viewHeader) {
    if (viewHeader->sortIndicatorSection() >= 0) {
        settings->setValue("sort.column", model->headerData(viewHeader->sortIndicatorSection(), Qt::Horizontal));
        settings->setValue("sort.order", viewHeader->sortIndicatorOrder());
    }
}

void settings::saveWindowState(const QString &group, QWidget *widget, QAbstractItemModel *model, QHeaderView *viewHeader) {
    auto settings = appSettings();

    settings->beginGroup(group);
    settings->setValue("geometry", widget->saveGeometry());
    settings->setValue("width", widget->width());
    settings->setValue("height", widget->height());
    if (model && viewHeader) saveSort(settings, group, model, viewHeader);
    settings->endGroup();

    if (model && viewHeader) saveSizes(settings, group, model, viewHeader);
}

void settings::saveWindowState(const char *group, QWidget *widget, EntityView *entityView) {
    auto model = entityView ? entityView->model : nullptr;
    auto viewHeader = entityView ? entityView->viewHeader : nullptr;
    saveWindowState(group, widget, model, viewHeader);
}

static int columnIndex(QAbstractItemModel *model, const QString &name) {
    for (int col = 0; col < model->columnCount(); ++col) {
        if (model->headerData(col, Qt::Horizontal) == name) return col;
    }
    return -1;
}

void settings::restoreWindowState(const QString &group, QWidget *widget, QSize defaultSize, QAbstractItemModel *model, QHeaderView *viewHeader) {
    auto settings = appSettings();

    auto geometry = settings->value(group + "/geometry", QVariant{});
    if (!geometry.isValid() || ! widget->restoreGeometry(geometry.toByteArray())) {
        auto width = settings->value(group + "/width", QVariant{defaultSize.width()});
        auto height = settings->value(group + "/height", QVariant{defaultSize.height()});
        widget->resize(QSize{width.toInt(), height.toInt()});
    }

    if (model && viewHeader) {
        auto sortColumn = settings->value(group + "/sort.column", "").toString();
        if (!sortColumn.isEmpty()) {
            auto sortOrder = settings->value(group + "/sort.order", 0).toInt();
            auto index = columnIndex(model, sortColumn);
            viewHeader->setSortIndicator(index, static_cast<Qt::SortOrder>(sortOrder));
        }
        for (int section = 0; section < viewHeader->count(); ++section) {
            bool ok;
            auto name = model->headerData(section, Qt::Horizontal).toString();
            QString column = group + ".columns/" + name;
            auto width = settings->value(column + ".width").toInt(&ok);
            if (ok) viewHeader->resizeSection(section, width);
            auto pos = settings->value(column + ".pos").toInt(&ok);
            if (ok) viewHeader->moveSection(viewHeader->visualIndex(section), pos);
        }
    }
}

void settings::restoreWindowState(const QString &group, QWidget *widget, QSize defaultSize, EntityView *entityView) {
    auto model = entityView ? entityView->model : nullptr;
    auto viewHeader = entityView ? entityView->viewHeader : nullptr;
    restoreWindowState(group, widget, defaultSize, model, viewHeader);
}

Q_GLOBAL_STATIC(QSettings, dbSettings, QSettings::IniFormat, QSettings::UserScope, APP_NAME, "connection")

ConnectionSettings settings::connectionSettings(const QString &name) {
    return ConnectionSettings{
        dbSettings->value(name + "/type").toString(),
        dbSettings->value(name + "/host").toString(),
        dbSettings->value(name + "/port").toInt(),
        dbSettings->value(name + "/schema").toString(),
        dbSettings->value(name + "/user").toString(),
        dbSettings->value(name + "/password").toString(),
    };
}

QVariant settings::lastViewedAccount(const QString &connectionName) {
    return dbSettings->value(connectionName + "/last.viewed.account");
}

void settings::setLastViewedAccount(const QVariant &id, const QString &connectionName) {
    dbSettings->setValue(connectionName + "/last.viewed.account", id);
    dbSettings->sync();
}
