#ifndef FACTORY_H
#define FACTORY_H

#include <QModelIndex>
#include <QStatusBar>
#include <QValidator>

class ValidatorFactory;

class ValidationStatus : public QValidator
{
    const ValidatorFactory *const factory;
    QStatusBar *statusBar;

    State showStatus(const QString message) const;

protected:
    const QModelIndex index;

public:
    ValidationStatus(const ValidatorFactory *factory, const QModelIndex &index, QObject *parent, QStatusBar *statusBar = nullptr);

    State validate(QString &value, int &pos) const override;

    virtual const QString isValid(QString &value) const;

    virtual void fixup(QString &text) const override;
};

struct ValidatorFactory : public QObject {
    typedef std::function<const ValidationStatus*(QObject*,QStatusBar*)> Factory;

    /*!
     * \brief multiRow indicates whether the validation depends on multiple rows.
     *  Multi-row validators are owned/deleted by the column adapter and should not be global/shared.
     */
    const bool multiRow;

    ValidatorFactory(bool multiRow = false);

    virtual void initialize(QAbstractItemModel *model);

    virtual const Factory factory(const QModelIndex &index) const;

    virtual const QString isValid(const QModelIndex &index, QString &value) const = 0;

    virtual void fixup(QString &) const;

    virtual QList<QModelIndex> revalidate(QHash<QModelIndex, QString> &errors, const QModelIndex &index) const;

    static QString formatMessage(const QString format, const QModelIndex &index);
};

Q_DECLARE_OPAQUE_POINTER(ValidatorFactory::Factory)

#endif // FACTORY_H
