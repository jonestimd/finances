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
int main(int argc, char *argv[])
{
    finances::App app(argc, argv);
    QMetaType::registerConverter<QDecNumber, QString>(
        [](const QDecNumber &value) -> QString { return QString(value.toString()); }
    );
    QMetaType::registerConverter<const EnumValue*, QString>(
        [](const EnumValue *value) -> QString { return value ? value->name : ""; }
    );

    QThreadPool::globalInstance()->setMaxThreadCount(5);
    ConnectionPool connectionPool("QMYSQL", "hydra", 3306, DB_NAME, DB_USER, DB_USER);
    ServiceContext serviceContext(&connectionPool);
    DataStore dataStore(&serviceContext);

    AccountsWindow accountsWindow(&dataStore);
    accountsWindow.show();
    return app.exec();
}
