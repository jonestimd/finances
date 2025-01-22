#ifndef STATUSWINDOW_H
#define STATUSWINDOW_H

#include "statusbar.h"
#include <QMainWindow>

class StatusWindow : public QMainWindow {
    Q_OBJECT

protected:
    StatusBar statusBar{};

public:
    explicit StatusWindow();

    Q_INVOKABLE void enableUi();
    Q_INVOKABLE void addMessage(const QString &message);
    Q_INVOKABLE void removeMessage(const QString &message);
};

#endif // STATUSWINDOW_H
