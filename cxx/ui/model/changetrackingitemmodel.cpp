#include "changetrackingitemmodel.h"
#include "ui/model/sortfilterproxymodel.h"


ChangeTrackingItemModel::ChangeTrackingItemModel(QObject* parent) : QAbstractItemModel{parent} {}

ChangeHandler::ChangeHandler(QObject* parent, SortFilterProxyModel* sortModel, std::function<void (const ChangeTrackingItemModel*)> handleChange)
    : QObject{parent}
    , sortModel{sortModel}
    , handleChange{handleChange}
{
    modelChanged();
    connect(sortModel, SIGNAL(sourceModelChanged()), this, SLOT(modelChanged()));
}

void ChangeHandler::modelChanged() {
    for (auto& connection : std::as_const(connections)) disconnect(connection);
    connections.clear();
    // Need to connect to the source model because rowsRemoved is emitted by the proxy model before
    // the source model has completed the change.  Otherwise, the save button and change indicator
    // don't update when a singleton window with a new row is closed (discarding the changes) and reopened.
    auto model = sortModel->sourceModel();
    connections.append(connect(model, SIGNAL(dataChanged(QModelIndex,QModelIndex,QList<int>)), this, SLOT(dataChanged())));
    connections.append(connect(model, SIGNAL(rowsRemoved(QModelIndex,int,int)), this, SLOT(dataChanged())));
    connections.append(connect(model, SIGNAL(rowsInserted(QModelIndex,int,int)), this, SLOT(dataChanged())));
    connections.append(connect(model, SIGNAL(modelReset()), this, SLOT(dataChanged())));
}

void ChangeHandler::dataChanged() {
    const auto model = qobject_cast<ChangeTrackingItemModel*>(sortModel->sourceModel());
    if (model) handleChange(model);
}
