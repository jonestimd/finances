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

protected:
    void disableUi(const QString &message);
};

#endif // STATUSWINDOW_H
