#include "detailvalidator.h"
#include "service/model/basedomain.h"
#include "service/model/category.h"
#include "ui/model/transactiontablemodel.h"
#include <QDecNumber.hh>

SharesValidatorFactory::SharesValidatorFactory(int categoryColumnIndex, int securityColumnIndex, int amountColumnIndex)
    : NumberValidatorFactory(std::bind_front(&SharesValidatorFactory::isRequired, this), 6)
    , categoryColumnIndex{categoryColumnIndex}
    , securityColumnIndex{securityColumnIndex}
    , amountColumnIndex{amountColumnIndex}
{}

const QString SharesValidatorFactory::isValid(const QModelIndex &index, QString &value) const {
    auto message = NumberValidatorFactory::isValid(index, value);
    if (message.isEmpty() && index.parent().isValid()) {
        double shares(value.toDouble());
        if (shares == 0) {
            if (isRequired(index)) return tr("%1 is required").arg(columnHeader(index));
        } else {
            auto securityId = index.parent().siblingAtColumn(securityColumnIndex).data();
            if (securityId.isNull() && !value.isEmpty()) {
                return tr("%1 requires a security").arg(columnHeader(index));
            }
            auto amount = index.siblingAtColumn(amountColumnIndex).data(Qt::EditRole).value<QDecNumber>();
            if (isTransfer(index)) {
                if (!amount.isZero()) {
                    return tr("Transfer with %1 and %2 not allowed").arg(columnHeader(index), columnHeader(index, amountColumnIndex));
                }
            }
            else if (amount.isNegative() == (shares < 0)) {
                return tr("%1 and %2 must have opposite signs").arg(columnHeader(index), columnHeader(index, amountColumnIndex));
            }
        }
    }
    return message;
}

bool SharesValidatorFactory::isRequired(const QModelIndex &index) const {
    if (index.parent().isValid()) {
        auto category = TransactionType::getCategory(index.siblingAtColumn(categoryColumnIndex).data(Qt::EditRole));
        if (category) return AmountType::valueOf(category->amountType)->sharesRequied;
    }
    return false;
}

bool SharesValidatorFactory::isTransfer(const QModelIndex &index) const {
    if (index.parent().isValid()) {
        auto tt = TransactionType::get(index.siblingAtColumn(categoryColumnIndex).data(Qt::EditRole));
        return tt && tt->transfer;
    }
    return false;
}

DetailAmountValidatorFactory::DetailAmountValidatorFactory()
    : NumberValidatorFactory{std::bind_front(&DetailAmountValidatorFactory::isRequired, this), 2}
{}

bool DetailAmountValidatorFactory::isRequired(const QModelIndex &index) const {
    auto model = static_cast<const TransactionTableModel*>(index.model());
    auto detail = model->getDetail(index);
    return detail->id.has_value() || !detail->isEmpty();
}
