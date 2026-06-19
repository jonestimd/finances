#include "service/servicecontext.h"
#include "ui/model/datastore.h"
#include "ui/uicontext.h"
#include "ui/widget/settings.h"
#include "qtcommon.h"

#include <QApplication>
#include <QStyle>
#include <QStyleHints>

// to load styles use --stylesheet finances.qss
int main(int argc, char *argv[])
{
    finances::App app(argc, argv);
    qtcommon::registerConverters();

    ConnectionPool connectionPool(settings::connectionSettings());
    ServiceContext serviceContext(&connectionPool);
    DataStore dataStore(&serviceContext);

    UiContext uiContext{&dataStore};
    uiContext.start();
    return app.exec();
}
