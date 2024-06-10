#include "database/dbcontext.h"
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

    DbContext *dbContext = new DbContext("QMYSQL", "hydra", DB_NAME, DB_USER, DB_USER);

    AccountsWindow accountsWindow(&app, dbContext);
    accountsWindow.show();
    return app.exec();
}
