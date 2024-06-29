#ifndef STATUS_H
#define STATUS_H

#include <QStatusBar>
#include <QValidator>

class ValidationStatus : public QValidator
{
    QStatusBar *statusBar;
    QString message;
public:
    ValidationStatus(const QModelIndex &index, QObject *parent, QStatusBar *statusBar, const char *format);

    QValidator::State showStatus(bool valid) const;
};

#endif // STATUS_H
