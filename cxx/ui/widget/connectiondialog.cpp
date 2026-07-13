#include "connectiondialog.h"
#include "service/database/dbdialect.h"
#include "ui/finances.h"
#include "ui/uicontext.h"
#include "ui/widget/dialog.h"

#include <QDialogButtonBox>
#include <QFile>
#include <QFormLayout>
#include <QLabel>
#include <QSqlError>
#include <QPushButton>
#include <QVBoxLayout>
#include <QThreadPool>

#define MYSQL_TYPE_NAME "MySQL"
#define PG_TYPE_NAME "PostgreSQL"
#define SQLITE_TYPE_NAME "SQLite3"

#define NAME_PROP "InputName"
#define PORT_INPUT "port"
#define SCHEMA_INPUT "schema/file"

#define TEST_DB_NAME "test connection"

#define SOCKET_WHATS_THIS "<div>Linux socket file for\n" \
    "the admin connection.\n" \
    "If not provided then <b>Host</b>\n" \
    "and <b>Port</b> will be used\n" \
    "for the admin connection.</div>"
#define ADMIN_WHATS_THIS "Database super user for\ncreating the schema."

#define DEFAULT_SQLITE_EXT ".dbfin"
#define SQLITE_EXT_FILTER "*" DEFAULT_SQLITE_EXT " *.db"

using namespace finances;

static const QList<QString> sqliteExtensions{DEFAULT_SQLITE_EXT, ".db"};

static const QHash<const QString, const char*> typeMap{
    {MYSQL_TYPE_NAME, MYSQL_DRIVER},
    {PG_TYPE_NAME, PG_DRIVER},
    {SQLITE_TYPE_NAME, SQLITE_DRIVER},
};

static const QHash<const QString, const char*> defaultPort{
    {MYSQL_TYPE_NAME, "3306"},
    {PG_TYPE_NAME, "5432"},
};

static const QHash<const QString, const char*> defaultAdmin{
    {MYSQL_DRIVER, "root"},
    {PG_DRIVER, "postgres"},
};

template<class Settings, class Value>
QLineEdit *ConnectionDialog::connectInput(QLineEdit* input, Value Settings::*field, const QString& name) {
    if (!name.isEmpty()) input->setProperty(NAME_PROP, name);
    connect(input, &QLineEdit::textChanged, [=, this](const QString& value) {
        if constexpr (std::is_same_v<Value, QString>) {
            &settings->*field = value.trimmed();
        } else if constexpr (std::is_same_v<Value, int>) {
            auto x = value.toInt();
            &settings->*field = x > 0 ? x : -1;
        } else static_assert(false);
        inputChanged(input);
    });
    return input;
}

ConnectionDialog::ConnectionDialog(QWidget *parent, Mode mode)
    : QDialog(parent)
    , mode{mode}
    , settings{}
{
    setWindowModality(Qt::ApplicationModal);
    setWindowTitle(tr("Database Connection"));
    status.setWordWrap(true);
    status.setTextInteractionFlags(Qt::TextSelectableByMouse);
    auto buttonBox = new QDialogButtonBox(this);
    testButton = buttonBox->addButton(tr("&Test Connection"), QDialogButtonBox::ApplyRole);
    connect(testButton, SIGNAL(clicked()), this, SLOT(testConnection()));
    if (mode & Mode::Open) {
        openButton = buttonBox->addButton(QDialogButtonBox::Open);
        connect(openButton, SIGNAL(clicked()), this, SLOT(openDatabase()));
    }

    auto layout = new QVBoxLayout(this);
    auto formLayout = new QFormLayout();
    formLayout->setLabelAlignment(Qt::AlignRight);
    layout->addLayout(formLayout);

    typeInput.addItems({MYSQL_TYPE_NAME, PG_TYPE_NAME, SQLITE_TYPE_NAME});
    formLayout->addRow(tr("Database T&ype:"), &typeInput);

    formLayout->addRow(tr("&Host:"), connectInput(new QLineEdit(this), &ConnectionSettings::host));
    formLayout->addRow(tr("Po&rt:"), connectInput(maskInput(this, "00009"), &ConnectionSettings::port, PORT_INPUT));
    auto caption = tr("Select database file");
    auto filters = tr("Databases (%1);;All files(%2)").arg(SQLITE_EXT_FILTER, "*");
    auto schemaInput = mode & Create ? saveFileInput(this, caption, filters, &replaceConfirmed) : openFileInput(this, caption, filters);
    formLayout->addRow("dummy", connectInput(schemaInput, &ConnectionSettings::schema, SCHEMA_INPUT));
    formLayout->addRow(tr("U&ser"), connectInput(new QLineEdit(this), &ConnectionSettings::user));
    formLayout->addRow(tr("P&assword"), connectInput(::passwordInput(this), &ConnectionSettings::password));
    if (mode & Create) {
        adminUserInput = connectInput(whatsThisInput(this, tr(ADMIN_WHATS_THIS)), &AdminConnectionSettings::adminUser);
        formLayout->addRow(tr("Admin &User:"), adminUserInput);
        formLayout->addRow(tr("Admin &Password"), connectInput(::passwordInput(this), &AdminConnectionSettings::adminPassword));
        formLayout->addRow(tr("Admin Soc&ket"), connectInput(whatsThisInput(this, tr(SOCKET_WHATS_THIS)), &AdminConnectionSettings::adminSocket));
        createButton = buttonBox->addButton(tr("&Create"), QDialogButtonBox::YesRole);
        connect(createButton, SIGNAL(clicked(bool)), this, SLOT(createDatabase()));
    }

    layout->addWidget(&status);
    layout->addWidget(buttonBox);

    connect(&typeInput, &QComboBox::currentTextChanged, this, &ConnectionDialog::typeChanged);
    connect(this, SIGNAL(statusChanged(QString)), this, SLOT(setStatus(QString)));
    typeChanged(typeInput.currentText());
}

void ConnectionDialog::testConnection() {
    setDisabled(true);
    status.setText(tr("Trying connection..."));
    QThreadPool::globalInstance()->start([=, this] {
        {
            // TODO also check non-admin for create?
            ConnectionSettings testSettings = mode & Create ? settings.toAdminSchema() : settings;
            auto db = QSqlDatabase::addDatabase(testSettings.dbType, TEST_DB_NAME);
            if (testSettings.openDatabase(db)) {
                emit statusChanged(tr("Successful connection"));
                db.close();
            } else {
                emit statusChanged(tr("Error: %1").arg(ConnectionSettings::lastError(db)));
            }
        }
        QSqlDatabase::removeDatabase(TEST_DB_NAME);
    });
}

void ConnectionDialog::typeChanged(const QString &value) {
    settings.dbType = typeMap.value(value);
    bool isSqlite = value == SQLITE_TYPE_NAME;
    const auto labels = findChildren<QLabel*>();
    for (auto label : labels) {
        auto input = qobject_cast<QLineEdit*>(label->buddy());
        if (input) {
            input->clear();
            auto inputName = input->property(NAME_PROP).toString();
            if (inputName != SCHEMA_INPUT) {
                label->setVisible(!isSqlite);
                input->setVisible(!isSqlite);
                if (inputName == PORT_INPUT) input->setText(defaultPort.value(value));
            } else {
                label->setText(isSqlite ? tr("&File:") : tr("Sche&ma:"));
                input->actions().constFirst()->setVisible(isSqlite);
            }
        }
    }
    if (adminUserInput) adminUserInput->setText(defaultAdmin.value(settings.dbType, ""));
    inputChanged(&typeInput);
    adjustSize();
}

void ConnectionDialog::inputChanged(QWidget* input) {
    if (input->property(NAME_PROP).toString() ==  SCHEMA_INPUT) replaceConfirmed = false;
    bool enable = settings.isComplete(mode & Create);
    auto isSqlite = typeInput.currentText() == SQLITE_TYPE_NAME;
    if (openButton) openButton->setEnabled(enable);
    if (createButton) {
        auto haveUser = !settings.adminUser.isEmpty();
        createButton->setEnabled(enable && (isSqlite || haveUser));
        testButton->setEnabled(enable && haveUser && !isSqlite);
    } else testButton->setEnabled(enable && !isSqlite);
    status.clear();
}

void ConnectionDialog::setStatus(const QString message) {
    status.setText(message);
    adjustSize();
    setDisabled(false);
}

void ConnectionDialog::handleOpenResult(DataStore *dataStore, const QString &error) {
    if (error.isEmpty()) {
        auto context = new UiContext(dataStore);
        context->start();
        App::addConnection(settings);
        accept();
    } else {
        emit statusChanged(error);
    }
}

void ConnectionDialog::createDatabase() {
    if (typeInput.currentText() == SQLITE_TYPE_NAME) {
        replaceConfirmed &= ensureExtension(settings.schema, sqliteExtensions);
        auto dbfile = QFile{settings.schema};
        if (dbfile.exists()) {
            if (!replaceConfirmed) {
                if (!dialog::confirmReplaceFile(this, settings.schema)) return;
                replaceConfirmed = true;
            }
            dbfile.remove();
        }
    }
    setDisabled(true);
    status.setText(tr("Creating database..."));
    QThreadPool::globalInstance()->start([=, this]() {
        DaoContext daos{settings.dbType};
        try {
            daos.createDatabase(settings);
            QMetaObject::invokeMethod(this, &ConnectionDialog::openDatabase, Qt::QueuedConnection);
        } catch(const QString error) {
            emit statusChanged(error);
        }
    });
}

void ConnectionDialog::openDatabase() {
    setDisabled(true);
    auto dataStore = new DataStore(this->settings);
    status.setText(tr("Loading Accounts..."));
    dataStore->loadAccounts(std::bind_front(&ConnectionDialog::handleOpenResult, this));
}
