#include "connectiondialog.h"
#include "service/database/dbdialect.h"
#include "ui/finances.h"
#include "ui/widget/settings.h"

#include <QDialogButtonBox>
#include <QFile>
#include <QFormLayout>
#include <QLabel>
#include <QSqlError>
#include <QPushButton>
#include <QVBoxLayout>

#include <ui/uicontext.h>

#define MYSQL_TYPE_NAME "MySQL"
#define PG_TYPE_NAME "PostgreSQL"
#define SQLITE_TYPE_NAME "SQLite3"

#define SQLITE_PROP "SqliteInput"
#define TEST_DB_NAME "test connection"

using namespace finances;

static const QHash<const QString, QString> typeMap{
    {MYSQL_TYPE_NAME, MYSQL_DRIVER},
    {PG_TYPE_NAME, PG_DRIVER},
    {SQLITE_TYPE_NAME, SQLITE_DRIVER},
};

static const QHash<const QString, QString> defaultPort{
    {MYSQL_TYPE_NAME, "3306"},
    {PG_TYPE_NAME, "5432"},
};

template<typename Value>
QLineEdit *ConnectionDialog::initInput(QLineEdit* input, Value ConnectionSettings::*field, bool sqliteInput) {
    input->setProperty(SQLITE_PROP, sqliteInput);
    connect(input, &QLineEdit::textChanged, [=, this](const QString& value) {
        if constexpr (std::is_same_v<Value, QString>) {
            &settings->*field = value;
        } else if constexpr (std::is_same_v<Value, int>) {
            auto x = value.toInt();
            &settings->*field = x > 0 ? x : -1;
        } else static_assert(false);
        inputChanged();
    });
    return input;
}

ConnectionDialog::ConnectionDialog(QWidget *parent)
    : QDialog(parent)
    , settings{}
{
    setWindowModality(Qt::ApplicationModal);
    setWindowTitle(tr("Database Connection"));
    status.setWordWrap(true);

    auto layout = new QVBoxLayout(this);
    auto formLayout = new QFormLayout();
    formLayout->setLabelAlignment(Qt::AlignRight);
    layout->addLayout(formLayout);

    typeInput.addItems({MYSQL_TYPE_NAME, PG_TYPE_NAME, SQLITE_TYPE_NAME});
    formLayout->addRow(tr("Database &Type:"), &typeInput);

    formLayout->addRow(tr("&Host:"), initInput(new QLineEdit(this), &ConnectionSettings::host));
    formLayout->addRow(tr("Po&rt:"), initInput(maskInput(this, "00009"), &ConnectionSettings::port));
    auto caption = tr("Select database file");
    auto filters = tr("Databases (%1);;All files(%2)").arg("*.fdb *.db", "*");
    formLayout->addRow(tr("&Schema:"), initInput(fileInput(this, caption, filters), &ConnectionSettings::schema, true));
    formLayout->addRow(tr("&User:"), initInput(new QLineEdit(this), &ConnectionSettings::user));
    formLayout->addRow(tr("&Password:"), initInput(passwordInput(this), &ConnectionSettings::password));

    layout->addWidget(&status);
    auto buttonBox = new QDialogButtonBox(this);
    testButton = buttonBox->addButton(tr("&Test"), QDialogButtonBox::ApplyRole);
    openButton = buttonBox->addButton(QDialogButtonBox::Open);
    layout->addWidget(buttonBox);

    connect(testButton, SIGNAL(clicked()), this, SLOT(testConnection()));
    connect(buttonBox, SIGNAL(accepted()), this, SLOT(accept()));
    connect(buttonBox, SIGNAL(rejected()), this, SLOT(reject()));
    connect(&typeInput, &QComboBox::currentTextChanged, this, &ConnectionDialog::typeChanged);

    typeChanged(typeInput.currentText());
}

const ConnectionSettings ConnectionDialog::connectionSettings() const {
    return settings;
}

void ConnectionDialog::testConnection() {
    status.setText(tr("Connecting..."));
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
}

void ConnectionDialog::typeChanged(const QString &value) {
    settings.dbType = typeMap.value(value);
    bool isSqlite = value == SQLITE_TYPE_NAME;
    const auto labels = findChildren<QLabel*>();
    for (auto label : labels) {
        auto input = qobject_cast<QLineEdit*>(label->buddy());
        if (input) {
            if (!input->property(SQLITE_PROP).toBool()) {
                updateInput(input, !isSqlite);
                if (!input->inputMask().isEmpty()) input->setText(defaultPort.value(value));
            } else {
                label->setText(isSqlite ? tr("&File:") : tr("&Schema:"));
                input->actions().constFirst()->setVisible(isSqlite);
                input->clear();
            }
        }
    }
    inputChanged();
}

void ConnectionDialog::inputChanged() {
    bool enable = settings.isComplete();
    testButton->setEnabled(enable && typeInput.currentText() != SQLITE_TYPE_NAME);
    openButton->setEnabled(enable);
    status.clear();
}

void ConnectionDialog::updateInput(QLineEdit* input, bool enable) {
    if (input->isEnabled() != enable) {
        input->clear();
        input->setEnabled(enable);
        style()->unpolish(input);
    }
}

void ConnectionDialog::handleOpenResult(DataStore *dataStore, const QString &error) {
    if (error.isEmpty()) {
        auto context = new UiContext(dataStore);
        context->start();
        settings::addConnection(settings);
        QDialog::accept();
    } else {
        status.setText(error);
        adjustSize();
        setDisabled(false);
    }
}

void ConnectionDialog::accept() {
    auto dataStore = new DataStore(settings);
    status.setText(tr("Loading Accounts..."));
    setDisabled(true);
    dataStore->loadAccounts(this);
}
