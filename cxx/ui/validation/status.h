#ifndef STATUS_H
#define STATUS_H

#include <QStatusBar>
#include <QValidator>

class ValidationStatus : public QValidator
{
    QStatusBar *statusBar;

    State showStatus(const QString message) const;
public:
    ValidationStatus(const QModelIndex &index, QObject *parent, QStatusBar *statusBar = nullptr);

    State validate(QString &value, int &pos) const override;

    virtual const QString isValid(QString &value) const = 0;

    static QString formatMessage(const char *format, const QModelIndex &index);

    virtual void fixup(QString &) const override;
};


#endif // STATUS_H
