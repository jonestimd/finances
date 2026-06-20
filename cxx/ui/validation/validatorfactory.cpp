#include "validatorfactory.h"

ValidationStatus::ValidationStatus(const ValidatorFactory *factory, const QModelIndex &index, QObject *parent, QStatusBar *statusBar)
    : QValidator{parent}
    , factory{factory}
    , index{index}
    , statusBar{statusBar}
{}

QValidator::State ValidationStatus::showStatus(const QString message) const {
    if (!message.isNull()) {
        if (statusBar) statusBar->showMessage(message);
        return QValidator::State::Intermediate;
    }
    if (statusBar) statusBar->clearMessage();
    return QValidator::State::Acceptable;
}

QValidator::State ValidationStatus::validate(QString &value, int &pos) const {
    return showStatus(isValid(value));
}

const QString ValidationStatus::isValid(QString &value) const {
    return factory->isValid(index, value);
}

void ValidationStatus::fixup(QString &text) const {
    factory->fixup(text);
}

ValidatorFactory::ValidatorFactory(bool multiRow, bool global): multiRow{multiRow}, global{global} {}

void ValidatorFactory::initialize(QAbstractItemModel *model) {}

const ValidatorFactory::Factory ValidatorFactory::factory(const QModelIndex &index) const {
    return [this, index](QObject *parent, QStatusBar *statusBar = nullptr) -> const ValidationStatus* {
        return new ValidationStatus(this, index, parent, statusBar);
    };
}

void ValidatorFactory::fixup(QString &) const {}

QModelIndexList ValidatorFactory::revalidateRows(QHash<const QModelIndex, QString> &errors, const QModelIndex &index) const {
    QList<QModelIndex> changes;
    if (multiRow) {
        QModelIndex i = index;
        for (int r = 0; r < index.model()->rowCount(); r++) {
            i = i.siblingAtRow(r);
            auto value = i.data().value<QString>();
            auto message = isValid(i, value);
            if (errors.value(i, nullptr) != message) {
                changes.append(i);
                if (!message.isEmpty()) errors.insert(i,  message);
                else errors.remove(i);
            }
        }
    }
    return changes;
}

const QString ValidatorFactory::columnHeader(const QModelIndex &index) const {
    return columnHeader(index, index.column());
}

const QString ValidatorFactory::columnHeader(const QModelIndex &index, int column) const {
    auto title = index.model()->headerData(column, Qt::Horizontal).toString();
    if (index.parent().isValid() && title.contains('\n')) return title.split('\n').at(1);
    return title;
}
