#include "tablesort.h"
#include "tableitemdelegate.h"
#include <QHeaderView>
#include <QKeyEvent>
#include <QTableWidget>
#include <QTimer>

TableSort::TableSort(QWidget *parent, AdapterTableModel *model, const char * filterLabel, const char *defaultSort, QStatusBar *statusBar)
    : QObject(parent)
    , model{model}
    , sortModel{parent}
    , table{parent}
    , filterInput{filterLabel, &sortModel, parent}
    , statusBar{statusBar}
    , itemDelegate{parent, statusBar}
{
    if (defaultSort) this->defaultSort = parent->tr(defaultSort);
    sortModel.setSourceModel(model);
    sortModel.setSortRole(finances::SortRole);
    sortModel.setFilterKeyColumn(-1);

    table.setModel(&sortModel);
    table.setItemDelegate(&itemDelegate);

    table.resizeColumnsToContents();
    table.setAlternatingRowColors(true);
    table.setSortingEnabled(true);
    // table.selectionModel()->select(sortModel.index(0, 0), QItemSelectionModel::Select);
    connect(table.selectionModel(), SIGNAL(currentChanged(QModelIndex,QModelIndex)), this, SLOT(showValidation(QModelIndex)));

    auto header = table.horizontalHeader();
    header->setSectionsMovable(true);
    header->setSortIndicatorShown(true);
    header->setSortIndicator(0, Qt::SortOrder::AscendingOrder);

    table.selectionModel()->select(sortModel.index(0, 0), QItemSelectionModel::Select);
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

void TableSort::startEdit(int rowIndex, int columnIndex) {
    auto index = sortModel.mapFromSource(model->index(rowIndex, columnIndex));
    table.selectionModel()->setCurrentIndex(index, QItemSelectionModel::ClearAndSelect);
    table.edit(index);
}

QAction *TableSort::addAction(const char *text) {
    auto addAction = finances::iconAction(finances::AddCircle, tr(text), QKeySequence::New, this);
    connect(addAction, SIGNAL(triggered(bool)), this, SLOT(triggerAdd()));
    connect(&itemDelegate, &TableItemDelegate::openEditor, addAction, [=]() { addAction->setEnabled(false); });
    connect(&itemDelegate, &TableItemDelegate::closeEditor, addAction, [=]() { addAction->setEnabled(true); });
    return addAction;
}

QAction *TableSort::deleteAction(const char *text, std::function<bool(int)> enableDelete) {
    auto action = finances::iconAction(finances::Trash, tr(text), QKeySequence::Delete, this, SLOT(triggerDelete()));
    auto setEnabled = [=, this]() {
        auto indexes = sortModel.mapSelectionToSource(table.selectionModel()->selection()).indexes();
        bool enabled = !indexes.empty();
        for (auto i = indexes.cbegin(); enabled && i != indexes.cend(); ++i) enabled &= enableDelete(i->row());
        action->setEnabled(enabled);
    };
    setEnabled();
    connect(table.selectionModel(), &QItemSelectionModel::selectionChanged, this, setEnabled);
    return action;
}

void TableSort::triggerAdd() {
    int rowIndex = model->queueAdd();
    startEdit(rowIndex, 0);
}

void TableSort::triggerDelete() {
    auto selection = sortModel.mapSelectionToSource(table.selectionModel()->selection());
    if (!selection .empty()) {
        for (auto i : selection.indexes()) model->queueDelete(i.row());
    }
}

void TableSort::showValidation(const QModelIndex &index) {
    auto range = table.selectionModel()->selection().indexes();
    if (range.length() == 1 && range.contains(index)) {
        auto message = index.data(finances::ValidationMessage);
        if (!message.isNull()) statusBar->showMessage(message.toString());
        else statusBar->clearMessage();
    }
    else statusBar->clearMessage();
}
