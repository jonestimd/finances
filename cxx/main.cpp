#include "ui/finances.h"
#include "qtcommon.h"

#include <QApplication>
#include <QStyle>
#include <QStyleHints>

// to load styles use --stylesheet finances.qss
int main(int argc, char *argv[]) {
    finances::App app(argc, argv);
    qtcommon::registerConverters();

    return app.start();
}
