#include "statusbar.h"

StatusBar::StatusBar(QWidget *parent) : QStatusBar(parent) {}

void StatusBar::addMessage(const QString &message) {
    messages.append(message);
    if (messages.length() == 1) showMessage(message);
}

void StatusBar::removeMessage(const QString &message) {
    // TODO only call from EntityView and enable itemView when no more messages
    auto index = messages.indexOf(message);
    if (index >= 0) {
        messages.removeAt(index);
        if (messages.isEmpty()) showMessage(tr("Ready"), 1500);
        else showMessage(messages.first());
    }
}

void StatusBar::clear() {
    messages.clear();
    clearMessage();
}

bool StatusBar::isEmpty() const {
    return messages.isEmpty();
}
