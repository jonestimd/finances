#include "appwindow.h"

AppWindow::AppWindow(QWidget *parent) : QMainWindow{parent} {}

void AppWindow::closeEvent(QCloseEvent *event) {
    emit closed(this);
}
