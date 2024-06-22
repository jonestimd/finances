#include "service/servicecontext.h"
#include "ui/model/datastore.h"
#include "ui/widget/accountswindow.h"

#include <QApplication>
#include <QDecNumber.hh>
#include <QStyle>
#include <QStyleHints>

#define DB_NAME "finances_test"
#define DB_USER DB_NAME

// to load styles use --stylesheet finances.qss
// TODO load dark/light deficit colors from config (stylesheet?)
int main(int argc, char *argv[])
{
    Finances::App app(argc, argv);
    QMetaType::registerConverter<QDecNumber, QString>(
        [](const QDecNumber &value) -> QString { return QString(value.toString()); }
    );

    ConnectionPool connectionPool("QMYSQL", "hydra", DB_NAME, DB_USER, DB_USER);
    ServiceContext serviceContext(&connectionPool);
    DataStore dataStore(&serviceContext);

    AccountsWindow accountsWindow(&app, &dataStore);
    accountsWindow.show();
    return app.exec();
}
