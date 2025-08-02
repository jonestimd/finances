#include "service/servicecontext.h"
#include "ui/model/datastore.h"
#include "ui/uicontext.h"
#include "ui/widget/settings.h"

#include <QApplication>
#include <QDecNumber.hh>
#include <QStyle>
#include <QStyleHints>

Q_LOGGING_CATEGORY(sqlLogger, "sql")

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

    ConnectionPool connectionPool(settings::connectionSettings());
    ServiceContext serviceContext(&connectionPool);
    DataStore dataStore(&serviceContext);

    UiContext uiContext{&dataStore};
    uiContext.start();
    return app.exec();
}
