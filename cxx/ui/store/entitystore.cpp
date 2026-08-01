#include "entitystore.h"
#include "ui/widget/dialog.h"

#include <QThreadPool>

AbstractEntityStore::AbstractEntityStore(StatusMessageStore* messageStore, QObject *parent)
    : QObject(parent)
    , messageStore{messageStore}
{}

const QString AbstractEntityStore::user{std::optional(std::getenv("USER")).value_or(std::getenv("USERNAME"))};

void AbstractEntityStore::doInBackground(QWidget* source, const QString& message, Runnable task, Runnable onError) {
    messageStore->addMessage(message);
    QThreadPool::globalInstance()->start([=, this]() {
        try {
            task();
        } catch(const QString error) {
            dialog::showError(source, error);
            if (onError) onError();
        }
        QMetaObject::invokeMethod(messageStore, &StatusMessageStore::removeMessage, Qt::QueuedConnection, message);
    });
}
