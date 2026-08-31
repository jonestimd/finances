#include "securitylottablemodel.h"
#include "amountcolumnadapter.h"
#include "formatcolumnadapter.h"
#include "ui/model/formats.h"
#include "ui/store/datastore.h"
#include "ui/titles.h"
#include "ui/validation/numeric.h"

namespace securitylottable {
    class SharesValidator : public NumberValidatorFactory {
        const SecurityLotTableModel* const model;
    public:
        SharesValidator(const SecurityLotTableModel* model) : NumberValidatorFactory{6, false, 0}, model{model} {}

        const QString isValid(const QModelIndex& index, QString& value) const override {
            auto purchase = model->getRow(index);
            auto shares = QDecNumber{value.toLocal8Bit().constData()};
            if (shares > purchase->availableShares() + model->lotShares(purchase->id.value())) {
                return tr("%1 must be less than the purchase shares").arg(columnHeader(index));
            }
            auto message = NumberValidatorFactory::isValid(index, value);
            return message;
        }
    };

    class AvailableSharesAdapter : public AmountColumnAdapter<SecurityPurchase, optional_id> {
        SecurityLotTableModel* const model;

    public:
        AvailableSharesAdapter(const QString title, SecurityLotTableModel* model)
            : AmountColumnAdapter{title, &SecurityPurchase::id, securityShares, false}
            , model{model} {}

        QVariant rowValue(const SecurityPurchase* row) const override {
            return QVariant::fromValue(model->availableShares(row));
        }
    };

    class AllocatedSharesAdapter : public AmountColumnAdapter<SecurityPurchase, optional_id> {
        SecurityLotTableModel* const model;

    public:
        AllocatedSharesAdapter(const QString title, SecurityLotTableModel* model)
            : AmountColumnAdapter{title, &SecurityPurchase::id, securityShares, true, new SharesValidator{model}}
            , model{model} {}

        QVariant rowValue(const SecurityPurchase* row) const override {
            return QVariant::fromValue(model->allocatedShares(row->id.value()));
        }
    };
}

using namespace securitylottable;

enum Column : int {Date, TotalShares, Price, AvailableShares, AllocatedShares};

SecurityLotTableModel::SecurityLotTableModel(DataStore* dataStore, const TransactionDetail* sale)
    : ChangeTrackingItemModel{}
    , dataStore{dataStore}
    , sale{sale}
    , columns{
        new FormatColumnAdapter{tr(DATE_TITLE), &SecurityPurchase::transactionDate, dateFormat, false},
        new AmountColumnAdapter{tr(SHARES_TITLE), &SecurityPurchase::totalShares, securityShares, false},
        new AmountColumnAdapter{tr("Price"), &SecurityPurchase::price, dollarFormat},
        new AvailableSharesAdapter{tr("Available Shares"), this},
        new AllocatedSharesAdapter{tr("Allocated Shares"), this},
    }
{}

SecurityLotTableModel::~SecurityLotTableModel() {
    qDeleteAll(columns);
    reset();
}

QVariant SecurityLotTableModel::headerData(int section, Qt::Orientation orientation, int role) const {
    if (role == Qt::DisplayRole && orientation == Qt::Horizontal) {
        if (section >= 0 && section < columns.size()) return columns[section]->title;
    }
    return QVariant{};
}

int SecurityLotTableModel::columnCount(const QModelIndex& parent) const {
    return parent.isValid() ? 0 : columns.size();
}

int SecurityLotTableModel::rowCount(const QModelIndex& parent) const {
    if (parent.isValid()) return 0;
    return purchases.size();
}

Qt::ItemFlags SecurityLotTableModel::flags(const QModelIndex& index) const {
    if (!index.isValid()) return Qt::NoItemFlags;
    return QAbstractItemModel::flags(index) | columns.at(index.column())->flags(purchases.at(index.row()), true);
}

QVariant SecurityLotTableModel::data(const QModelIndex& index, int role) const {
    if (index.isValid()) {
        auto purchase = purchases.at(index.row());
        switch (role) {
        case finances::ValidationMessageRole:
            break; // TODO
        case finances::UnsavedRole:
            if (index.column() == columns.size()-1 && sharesByPurchaseId.contains(purchase->id.value())) return finances::Update;
            return QVariant();
        }
        return columns.at(index.column())->value(purchase, index, {}, role);
    }
    return QVariant();
}

bool SecurityLotTableModel::setData(const QModelIndex& index, const QVariant& value, int role) {
    if (role == Qt::EditRole) {
        auto column = columns.at(index.column());
        auto parsed = column->parseValue(value).value<QDecNumber>();
        setAllocation(purchases.at(index.row())->id.value(), parsed);
        emit dataChanged(index, index, {role});
        return true;
    }
    return false;
}

void SecurityLotTableModel::setRows(const QList<const SecurityPurchase*> rows, const QList<const SecurityLot*> lots) {
    beginResetModel();
    reset();
    purchases.append(rows);
    for (auto lot : lots) lotsByPurchaseId.insert(lot->purchaseDetailId, lot);
    endResetModel();
}

QModelIndex SecurityLotTableModel::index(int row, int column, const QModelIndex& parent) const {
    return hasIndex(row, column, parent) ? createIndex(row, column) : QModelIndex{};
}

QModelIndex SecurityLotTableModel::parent(const QModelIndex& child) const {
    return QModelIndex{};
}

const SecurityPurchase* SecurityLotTableModel::getRow(const QModelIndex& index) const {
    return purchases.at(index.row());
}

bool SecurityLotTableModel::hasUnsavedChanges() const {
    return !sharesByPurchaseId.isEmpty();
}

void SecurityLotTableModel::clearChanges() {
    beginResetModel();
    sharesByPurchaseId.clear();
    endResetModel();
}

bool SecurityLotTableModel::isValid() const {
    return totalAllocatedShares() <= sale->assetQuantity.value().abs();
}

QDecNumber SecurityLotTableModel::lotShares(domain_id purchaseId) const {
    return lotsByPurchaseId.contains(purchaseId) ? lotsByPurchaseId.value(purchaseId)->adjustedShares : QDecNumber{0};
}

QDecNumber SecurityLotTableModel::availableShares(const SecurityPurchase* purchase) const {
    auto purchaseId = purchase->id.value();
    return purchase->availableShares() + lotShares(purchaseId) - allocatedShares(purchaseId);
}

QDecNumber SecurityLotTableModel::allocatedShares(domain_id purchaseId) const {
    if (sharesByPurchaseId.contains(purchaseId)) {
        return sharesByPurchaseId.value(purchaseId);
    }
    return lotShares(purchaseId);
}

QDecNumber SecurityLotTableModel::totalAllocatedShares() const {
    QDecNumber shares{0};
    for (auto purchase : std::as_const(purchases)) {
        shares += allocatedShares(purchase->id.value());
    }
    return shares;
}

void SecurityLotTableModel::allocateFirstIn() {
    allocateShares([](const SecurityPurchase* p1, const SecurityPurchase* p2) {
        return p1->transactionDate < p2->transactionDate;
    });
}

void SecurityLotTableModel::allocateLastIn() {
    allocateShares([](const SecurityPurchase* p1, const SecurityPurchase* p2) {
        return p1->transactionDate > p2->transactionDate;
    });
}

void SecurityLotTableModel::allocateLowestPrice() {
    allocateShares([](const SecurityPurchase* p1, const SecurityPurchase* p2) {
        return p1->price() < p2->price();
    });
}

void SecurityLotTableModel::allocateHighestPrice() {
    allocateShares([](const SecurityPurchase* p1, const SecurityPurchase* p2) {
        return p1->price() > p2->price();
    });
}

void SecurityLotTableModel::discardLots() {
    sharesByPurchaseId.clear();
    for (auto [purchaseId, lot] : lotsByPurchaseId.asKeyValueRange()) {
        sharesByPurchaseId.insert(purchaseId, QDecNumber{0});
    }
    emit dataChanged(index(0, AvailableShares, {}), index(purchases.size()-1, AllocatedShares, {}));
}

void SecurityLotTableModel::allocateShares(std::function<bool(const SecurityPurchase*, const SecurityPurchase*)> less) {
    auto sortedPurchases = QList{this->purchases};
    std::stable_sort(sortedPurchases.begin(), sortedPurchases.end(), less);
    auto remaining = sale->assetQuantity.value().abs() - totalAllocatedShares();
    for (auto purchase : std::as_const(sortedPurchases)) {
        auto available = availableShares(purchase);
        if (available > QDecNumber{0}) {
            auto purchaseId = purchase->id.value();
            auto allocation = available.min(remaining);
            setAllocation(purchaseId, sharesByPurchaseId.value(purchaseId) + allocation);
            remaining -= allocation;
            if (remaining.isZero()) break;
        }
    }
    emit dataChanged(index(0, AvailableShares, {}), index(purchases.size()-1, AllocatedShares, {}));
}

void SecurityLotTableModel::setAllocation(domain_id purchaseId, QDecNumber shares) {
    if (lotsByPurchaseId.contains(purchaseId)) {
        if (lotsByPurchaseId.value(purchaseId)->adjustedShares == shares) sharesByPurchaseId.remove(purchaseId);
        else sharesByPurchaseId.insertOrAssign(purchaseId, shares);
    } else {
        if (shares.isZero()) sharesByPurchaseId.remove(purchaseId);
        else sharesByPurchaseId.insertOrAssign(purchaseId, shares);
    }
}

void SecurityLotTableModel::reset() {
    qDeleteAll(purchases);
    qDeleteAll(lotsByPurchaseId);
    purchases.clear();
    lotsByPurchaseId.clear();
    sharesByPurchaseId.clear();
}
