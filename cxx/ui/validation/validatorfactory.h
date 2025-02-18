#ifndef FACTORY_H
#define FACTORY_H

#include <QModelIndex>
#include <QStatusBar>
#include <QValidator>

class ValidatorFactory;

/**
 * @brief The ValidationStatus class implements `QValidator` and displays validation errors on a `QStatusBar`.
 * The validation logic is delegated to a `ValidationFactory`.
 */
class ValidationStatus : public QValidator {
    const ValidatorFactory *const factory;
    QStatusBar *const statusBar;

    State showStatus(const QString message) const;

protected:
    const QModelIndex index;

public:
    ValidationStatus(const ValidatorFactory *factory, const QModelIndex &index, QObject *parent, QStatusBar *statusBar = nullptr);

    /**
     * @brief validate calls `factory::isValid` and updates the status bar.
     */
    State validate(QString &value, int &pos) const override;

    virtual const QString isValid(QString &value) const;

    virtual void fixup(QString &text) const override;
};

/**
 * @brief The ValidatorFactory class is used by `TableItemDelegate` to create a validator when
 * editing a cell.
 */
struct ValidatorFactory : public QObject {
    typedef std::function<const ValidationStatus*(QObject*,QStatusBar*)> Factory;
    typedef std::function<const QString(const QModelIndex &)> GetTitle;

    /**
     * @brief global Indicates the validator factory is a global/reusable instance.
     * Non-global validator factories are owned/deleted by the column adapter.
     */
    const bool global;
    /**
     * @brief multiRow Indicates whether the validation depends on multiple rows.
     */
    const bool multiRow;

    ValidatorFactory(bool multiRow = false, bool global = false);

    virtual void initialize(QAbstractItemModel *model);

    const Factory factory(const QModelIndex &index) const;

    virtual const QString isValid(const QModelIndex &index, QString &value) const = 0;

    virtual void fixup(QString &) const;

    /**
     * @brief revalidateRows Revalidates a row and all of its siblings.
     * @param errors The existing errors (to be updated)
     * @param index The index of the row
     * @return a list of modified indexes.
     */
    virtual QModelIndexList revalidateRows(QHash<QModelIndex, QString> &errors, const QModelIndex &index) const;

    static const QString columnHeader(const QModelIndex &index);
};

Q_DECLARE_OPAQUE_POINTER(ValidatorFactory::Factory)

#endif // FACTORY_H
