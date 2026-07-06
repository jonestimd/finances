#ifndef CONNECTIONDIALOG_H
#define CONNECTIONDIALOG_H

#include "service/database/connectionpool.h"
#include "ui/model/datastore.h"
#include <QComboBox>
#include <QDialog>
#include <QLabel>
#include <QMenu>

class ConnectionDialog : public QDialog {
    Q_OBJECT
    QComboBox typeInput;
    QPushButton *testButton;
    QPushButton *openButton;
    QLabel status{};
    ConnectionSettings settings;

public:
    ConnectionDialog(QWidget *parent = nullptr);

    const ConnectionSettings connectionSettings() const;

    void handleOpenResult(DataStore* dataStore, const QString& error);

private slots:
    void testConnection();

private:
    template<typename Value>
    QLineEdit* initInput(QLineEdit* input, Value ConnectionSettings::*field, bool sqliteInput = false);

    void typeChanged(const QString& value);
    void inputChanged();
    void updateInput(QLineEdit* input, bool enable);

public slots:
    virtual void accept() override;
};

#endif // CONNECTIONDIALOG_H