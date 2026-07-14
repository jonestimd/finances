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
#include <QCheckBox>

#define TEST_DB_NAME "test connection"

#define MINIMUM_WIDTH 350

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
    {MYSQL_OPTION, MYSQL_DRIVER},
    {PG_OPTION, PG_DRIVER},
    {SQLITE_OPTION, SQLITE_DRIVER},
};

static const QHash<const QString, const char*> defaultPort{
    {MYSQL_OPTION, "3306"},
    {PG_OPTION, "5432"},
};

static const QHash<const QString, const char*> defaultAdmin{
    {MYSQL_DRIVER, MYSQL_ROOT_USER},
    {PG_DRIVER, PG_ROOT_USER},
};

#define CONNECT_INPUT(input, field) connectInput(input, &AdminConnectionSettings::field, #field)

template<class Settings, class Value>
QLineEdit *ConnectionDialog::connectInput(QLineEdit* input, Value Settings::*field, const QString& name) {
    if (!name.isEmpty()) input->setObjectName(name);
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
    , create{bool(mode & Create)}
    , settings{}
{
    setWindowModality(Qt::ApplicationModal);
    setWindowTitle(tr("Database Connection"));
    setMinimumWidth(MINIMUM_WIDTH);
    status.setWordWrap(true);
    status.setTextInteractionFlags(Qt::TextSelectableByMouse);
    auto buttonBox = new QDialogButtonBox(this);
    testButton = buttonBox->addButton(tr("&Test Connection"), TEST_BUTTON_ROLE);
    connect(testButton, SIGNAL(clicked()), this, SLOT(testConnection()));
    if (mode & Mode::Open) {
        openButton = buttonBox->addButton(QDialogButtonBox::Open);
        openButton->setVisible(!create);
        connect(openButton, SIGNAL(clicked()), this, SLOT(openDatabase()));
    }

    auto layout = new QVBoxLayout(this);
    if (mode == OpenOrCreate) {
        auto checkbox = new QCheckBox{tr("Create &new database"), this};
        checkbox->setChecked(true);
        connect(checkbox, SIGNAL(toggled(bool)), this, SLOT(modeChanged(bool)));
        layout->addWidget(checkbox);
    }
    auto formLayout = new QFormLayout();
    formLayout->setLabelAlignment(Qt::AlignRight);
    layout->addLayout(formLayout);

    typeInput.addItems({MYSQL_OPTION, PG_OPTION, SQLITE_OPTION});
    formLayout->addRow(tr("Database T&ype:"), &typeInput);

    formLayout->addRow(tr("&Host:"), CONNECT_INPUT(new QLineEdit(this), host));
    formLayout->addRow(tr("Po&rt:"), CONNECT_INPUT(maskInput(this, "00009"), port));
    auto caption = tr("Select database file");
    auto filters = tr("Databases (%1);;All files(%2)").arg(SQLITE_EXT_FILTER, "*");
    auto schemaInput = mode & Create
        ? saveFileInput(this, caption, filters, &replaceConfirmed, &create)
        : openFileInput(this, caption, filters);
    formLayout->addRow("Sche&ma:", CONNECT_INPUT(schemaInput, schema));
    formLayout->addRow(tr("U&ser"), CONNECT_INPUT(new QLineEdit(this), user));
    formLayout->addRow(tr("P&assword"), CONNECT_INPUT(passwordInput(this), password));
    if (mode & Create) {
        adminUserInput = CONNECT_INPUT(whatsThisInput(this, tr(ADMIN_WHATS_THIS)), adminUser);
        formLayout->addRow(tr("Admin &User:"), adminUserInput);
        formLayout->addRow(tr("Admin &Password"), CONNECT_INPUT(passwordInput(this), adminPassword));
        formLayout->addRow(tr("Admin Soc&ket"), CONNECT_INPUT(whatsThisInput(this, tr(SOCKET_WHATS_THIS)), adminSocket));
        createButton = buttonBox->addButton(tr("&Create"), CREATE_BUTTON_ROLE);
        connect(createButton, SIGNAL(clicked(bool)), this, SLOT(createDatabase()));
    }

    layout->addWidget(&status);
    layout->addWidget(buttonBox);

    connect(&typeInput, &QComboBox::currentTextChanged, this, &ConnectionDialog::typeChanged);
    connect(this, SIGNAL(statusChanged(QString)), this, SLOT(setStatus(QString)));
    typeChanged(typeInput.currentText());
}

QString ConnectionDialog::getStatus() {
    return status.text();
}

void ConnectionDialog::testConnection() {
    setDisabled(true);
    status.setText(tr("Trying connection..."));
    QThreadPool::globalInstance()->start([=, this] {
        {
            // TODO also check non-admin for create?
            ConnectionSettings testSettings = create ? settings.toAdminSchema() : settings;
            auto db = QSqlDatabase::addDatabase(testSettings.dbType, TEST_DB_NAME);// #define NAME_PROP "InputName"

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

void ConnectionDialog::modeChanged(bool create) {
    this->create = create;
    openButton->setVisible(!create);
    createButton->setVisible(create);
    auto showAdmin = create && typeInput.currentText() != SQLITE_OPTION;
    const auto labels = findChildren<QLabel*>();
    for (auto label : labels) {
        auto input = qobject_cast<QLineEdit*>(label->buddy());
        if (input && input->objectName().startsWith("admin")) {
            label->setVisible(showAdmin);
            input->setVisible(showAdmin);
        }
    }
}

void ConnectionDialog::typeChanged(const QString &value) {
    settings.dbType = typeMap.value(value);
    bool isSqlite = value == SQLITE_OPTION;
    const auto labels = findChildren<QLabel*>();
    for (auto label : labels) {
        auto input = qobject_cast<QLineEdit*>(label->buddy());
        if (input) {
            input->clear();
            auto inputName = input->objectName();
            if (inputName != "schema") {
                label->setVisible(!isSqlite && (create || !inputName.startsWith("admin")));
                input->setVisible(!isSqlite && (create || !inputName.startsWith("admin")));
                if (inputName == "port") input->setText(defaultPort.value(value));
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
    if (input->objectName() == "schema") replaceConfirmed = false;
    bool enable = settings.isComplete(create);
    auto isSqlite = typeInput.currentText() == SQLITE_OPTION;
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
    if (typeInput.currentText() == SQLITE_OPTION) {
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
