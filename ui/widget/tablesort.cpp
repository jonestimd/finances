#include "tablesort.h"
#include "styleproxy.h"
#include "tableitemdelegate.h"
#include <QHeaderView>
#include <QKeyEvent>
#include <QTimer>

TableSort::TableSort(QWidget *parent, AdapterTableModel *model, const char * filterLabel, const char *defaultSort)
    : model{model}
    , sortModel{parent}
    , table{parent}
    , filterInput{filterLabel, &sortModel, parent}
{
    if (defaultSort) this->defaultSort = parent->tr(defaultSort);
    sortModel.setSourceModel(model);
    sortModel.setSortRole(finances::SortRole);
    sortModel.setFilterKeyColumn(-1);

    table.setModel(&sortModel);
    table.setStyle(new StyleProxy(&table));
    table.setItemDelegate(new TableItemDelegate(&table));

    table.resizeColumnsToContents();
    table.setAlternatingRowColors(true);
    table.setSortingEnabled(true);

    auto header = table.horizontalHeader();
    header->setSectionsMovable(true);
    header->setSortIndicatorShown(true);
    header->setSortIndicator(0, Qt::SortOrder::AscendingOrder);

    QTimer::singleShot(0, parent, [this] { filterInput.setFocus(); });
}

int TableSort::columnIndex(const QString name) const {
    return model->columnIndex(name);
}

void TableSort::enableColumnResize() {
    table.horizontalHeader()->setStretchLastSection(true);
}

void TableSort::setColumnResize(const std::vector<int> stretchColumns) {
    auto header = table.horizontalHeader();
    header->setSectionResizeMode(QHeaderView::ResizeToContents);
    for (int i : stretchColumns) {
        header->setSectionResizeMode(i, QHeaderView::Stretch);
    }
}

bool TableSort::focusFilter(QKeyEvent *event) {
    if (event->matches(QKeySequence::Find) && !filterInput.hasFocus()) {
        filterInput.setFocus();
        return true;
    }
    return false;
}

void TableSort::saveSort(QSettings *settings) {
    auto header = table.horizontalHeader();
    settings->setValue("sort.column", model->headerData(header->sortIndicatorSection(), Qt::Horizontal));
    settings->setValue("sort.order", header->sortIndicatorOrder());
}

void TableSort::saveSizes(QString group, QSettings *settings) {
    auto header = table.horizontalHeader();
    settings->beginGroup(QString(group).append(".columns"));
    for (int section = 0; section < header->count(); ++section) {
        auto name = model->headerData(section, Qt::Horizontal).toString();
        auto width = header->sectionSize(section);
        settings->setValue(name + ".width", width);
        settings->setValue(name + ".pos", header->visualIndex(section));
    }
    settings->endGroup();
}

void TableSort::restore(QString group, QSettings *settings) {
    auto model = table.model();
    auto header = table.horizontalHeader();
    if (!defaultSort.isEmpty()) {
        auto sortColumn = settings->value(group + "/sort.column", defaultSort).toString();
        auto sortOrder = settings->value(group + "/sort.order", 0).toInt();
        auto index = columnIndex(sortColumn);
        header->setSortIndicator(index, static_cast<Qt::SortOrder>(sortOrder));
    }
    for (int section = 0; section < header->count(); ++section) {
        bool ok;
        auto name = model->headerData(section, Qt::Horizontal, Qt::DisplayRole).toString();
        QString column = group + ".columns/" + name;
        auto width = settings->value(column + ".width").toInt(&ok);
        if (ok) header->resizeSection(section, width);
        auto pos = settings->value(column + ".pos").toInt(&ok);
        if (ok) header->moveSection(header->visualIndex(section), pos);
    }
}
