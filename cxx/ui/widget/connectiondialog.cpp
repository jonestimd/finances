#include "connectiondialog.h"
#include "service/database/dbdialect.h"
#include "ui/finances.h"
#include "ui/uicontext.h"
#include "ui/widget/settings.h"

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

#define SQLITE_PROP "SqliteInput"
#define TEST_DB_NAME "test connection"

#define SCHEMA_LABEL "Sche&ma:"

using namespace finances;

static const QHash<const QString, const char*> typeMap{
    {MYSQL_TYPE_NAME, MYSQL_DRIVER},
    {PG_TYPE_NAME, PG_DRIVER},
    {SQLITE_TYPE_NAME, SQLITE_DRIVER},
};

static const QHash<const QString, const char*> defaultPort{
    {MYSQL_TYPE_NAME, "3306"},
    {PG_TYPE_NAME, "5432"},
};

template<typename Value>
QLineEdit *ConnectionDialog::connectInput(QLineEdit* input, Value ConnectionSettings::*field, bool sqliteInput) {
    input->setProperty(SQLITE_PROP, sqliteInput);
    connect(input, &QLineEdit::textChanged, [=, this](const QString& value) {
        if constexpr (std::is_same_v<Value, QString>) {
            &settings->*field = value.trimmed();
        } else if constexpr (std::is_same_v<Value, int>) {
            auto x = value.toInt();
            &settings->*field = x > 0 ? x : -1;
        } else static_assert(false);
        inputChanged();
    });
    return input;
}

static const char* labelText(const char* adminText, ConnectionDialog::Mode mode) {
    return mode == ConnectionDialog::Mode::Create ? adminText : adminText + sizeof("Admin ");
}

static QString userHelp(ConnectionDialog::Mode mode) {
    if (mode & ConnectionDialog::Create) return QObject::tr("Database super user for\ncreating the schema.");
    else return "";
}

ConnectionDialog::ConnectionDialog(QWidget *parent, Mode mode)
    : QDialog(parent)
    , mode{mode}
    , settings{}
{
    setWindowModality(Qt::ApplicationModal);
    setWindowTitle(tr("Database Connection"));
    status.setWordWrap(true);
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
    formLayout->addRow(tr("Po&rt:"), connectInput(maskInput(this, "00009"), &ConnectionSettings::port));
    auto caption = tr("Select database file");
    auto filters = tr("Databases (%1);;All files(%2)").arg("*.dbfin *.db", "*");
    formLayout->addRow(tr(SCHEMA_LABEL), connectInput(fileInput(this, caption, filters), &ConnectionSettings::schema, true));
    if (mode & Mode::Create) {
        userInput = new QLineEdit(this);
        passwordInput = ::passwordInput(this);
        formLayout->addRow(tr("U&ser"), userInput);
        formLayout->addRow(tr("P&assword"), passwordInput);
        connect(userInput, SIGNAL(textChanged(QString)), this, SLOT(inputChanged()));
        connect(passwordInput, SIGNAL(textChanged(QString)), this, SLOT(inputChanged()));
        createButton = buttonBox->addButton(tr("&Create"), QDialogButtonBox::YesRole);
        connect(createButton, SIGNAL(clicked(bool)), this, SLOT(createDatabase()));
    }
    formLayout->addRow(tr(labelText("Admin &User:", mode)), connectInput(whatsThisInput(this, userHelp(mode)), &ConnectionSettings::user));
    formLayout->addRow(tr(labelText("Admin &Password", mode)), connectInput(::passwordInput(this), &ConnectionSettings::password));

    layout->addWidget(&status);
    layout->addWidget(buttonBox);

    connect(&typeInput, &QComboBox::currentTextChanged, this, &ConnectionDialog::typeChanged);
    typeChanged(typeInput.currentText());
}

const ConnectionSettings ConnectionDialog::connectionSettings() const {
    return settings;
}

void ConnectionDialog::testConnection() {
    setDisabled(true);
    status.setText(tr("Connecting..."));
    QThreadPool::globalInstance()->start([=, this] {
        {
            auto db = QSqlDatabase::addDatabase(settings.dbType, TEST_DB_NAME);
            if (settings.openDatabase(db)) {
                status.setText(tr("Successful connection"));
                db.close();
            } else {
                status.setText(tr("Error: %1").arg(ConnectionSettings::lastError(db)));
                adjustSize();
            }
        }
        QSqlDatabase::removeDatabase(TEST_DB_NAME);
        setDisabled(false);
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
            if (!input->property(SQLITE_PROP).toBool()) {
                label->setVisible(!isSqlite);
                input->setVisible(!isSqlite);
                if (!input->inputMask().isEmpty()) input->setText(defaultPort.value(value));
            } else {
                label->setText(isSqlite ? tr("&File:") : tr(SCHEMA_LABEL));
                input->actions().constFirst()->setVisible(isSqlite);
            }
        }
    }
    inputChanged();
    adjustSize();
}

static inline bool notEmpty(QLineEdit* input) {
    return !input->text().trimmed().isEmpty();
}

void ConnectionDialog::inputChanged() {
    bool enable = settings.isComplete();
    auto isSqlite = typeInput.currentText() != SQLITE_TYPE_NAME;
    testButton->setEnabled(enable && isSqlite);
    if (openButton) openButton->setEnabled(enable);
    if (createButton) {
        auto haveUser = isSqlite || notEmpty(userInput) && notEmpty(passwordInput);
        createButton->setEnabled(enable && haveUser);
    }
    status.clear();
}

void ConnectionDialog::handleOpenResult(DataStore *dataStore, const QString &error) {
    if (error.isEmpty()) {
        auto context = new UiContext(dataStore);
        context->start();
        settings::addConnection(settings);
        accept();
    } else {
        status.setText(error);
        adjustSize();
        setDisabled(false);
    }
}

void ConnectionDialog::createDatabase() {
    setDisabled(true);
    status.setText(tr("Creating database..."));
    QThreadPool::globalInstance()->start([=, this]() {
        ServiceContext context{settings};
        try {
            context.createDatabase(userInput->text().trimmed(), passwordInput->text().trimmed());
            QMetaObject::invokeMethod(this, &ConnectionDialog::openDatabase, Qt::QueuedConnection);
        } catch(const QString error) {
            QMetaObject::invokeMethod(this, &ConnectionDialog::createFailed, Qt::QueuedConnection, error);
        }
        context.shutdown();
    });
}

void ConnectionDialog::createFailed(const QString message) {
    status.setText(message);
    setDisabled(false);
}

void ConnectionDialog::openDatabase() {
    setDisabled(true);
    auto settings = this->settings;
    if (mode & Mode::Create) {
        settings.user = userInput->text().trimmed();
        settings.password = passwordInput->text().trimmed();
    }
    auto dataStore = new DataStore(settings);
    status.setText(tr("Loading Accounts..."));
    dataStore->loadAccounts(std::bind_front(&ConnectionDialog::handleOpenResult, this));
}

