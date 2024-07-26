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

ValidatorFactory::ValidatorFactory(bool multiRow): multiRow{multiRow} {}

const ValidationStatus *ValidatorFactory::validator(const QModelIndex &index, QObject *parent, QStatusBar *statusBar) const {
    return new ValidationStatus(this, index, parent, statusBar);
}

void ValidatorFactory::fixup(QString &) const {}

QList<QModelIndex> ValidatorFactory::revalidate(QHash<QModelIndex, QString> &errors, const QModelIndex &index) const {
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

QString ValidatorFactory::formatMessage(const char *format, const QModelIndex &index) {
    return tr(format).arg(index.model()->headerData(index.column(), Qt::Horizontal).toString());
}
