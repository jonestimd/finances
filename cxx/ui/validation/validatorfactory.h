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

class ValidatorFactory : public QObject {
    Q_OBJECT
public:
    /*!
     * \brief multiRow indicates whether the validation depends on multiple rows.
     */
    const bool multiRow;

    ValidatorFactory(bool multiRow = false);

    virtual const ValidationStatus *validator(const QModelIndex &index, QObject *parent, QStatusBar *statusBar = nullptr) const;

    virtual const QString isValid(const QModelIndex &index, QString &value) const = 0;

    virtual void fixup(QString &) const;

    virtual QList<QModelIndex> revalidate(QHash<QModelIndex, QString> &errors, const QModelIndex &index) const;

    static QString formatMessage(const QString format, const QModelIndex &index);
};

Q_DECLARE_OPAQUE_POINTER(ValidatorFactory*)

#endif // FACTORY_H
