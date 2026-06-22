#include "statusmessagestore.h"

void StatusMessageStore::addMessage(const QString message) {
    messages.append(message);
    if (messages.size() == 1) emit statusMessage(message);
}

void StatusMessageStore::removeMessage(const QString message) {
    messages.removeOne(message);
    if (messages.isEmpty()) emit isReady();
    else emit statusMessage(messages.first());
}
