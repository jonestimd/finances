#include "statusbar.h"

StatusBar::StatusBar(QWidget *parent) : QStatusBar(parent) {}

void StatusBar::addMessage(QString message) {
    messages.append(message);
    if (messages.length() == 1) showMessage(message);
}

void StatusBar::removeMessage(QString message) {
    auto index = messages.indexOf(message);
    if (index >= 0) {
        messages.removeAt(index);
        if (messages.isEmpty()) showMessage(tr("Ready"), 1500);
        else showMessage(messages.first());
    }
}

#include "statusbar.moc"
