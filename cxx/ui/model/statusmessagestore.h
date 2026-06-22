#ifndef STATUSMESSAGESTORE_H
#define STATUSMESSAGESTORE_H

#include <QObject>

class StatusMessageStore : public QObject {
    Q_OBJECT
    QStringList messages;

public:
    StatusMessageStore() = default;

    void addMessage(const QString message);
    Q_INVOKABLE void removeMessage(const QString message);

signals:
    void statusMessage(const QString message);
    void isReady();
};

#endif // STATUSMESSAGESTORE_H