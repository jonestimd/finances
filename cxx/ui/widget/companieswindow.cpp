#include "companieswindow.h"

#include "settings.h"
#include <QHeaderView>
#include <QKeyEvent>
#include <QTimer>

CompaniesWindow::CompaniesWindow(QMainWindow *parent, QList<Company*> companies)
    : QDialog(parent)
    , layout{this}
    , model{companies, this}
    , toolbar{this}
    , toolbarLayout{&toolbar}
    , sortModel{this}
    , tableSort{this, &model, "Company filter"}
{
    setWindowTitle(tr("Companies"));
    // todo add buttons to toolbar
    toolbarLayout.addWidget(&tableSort.filterInput);
    toolbarLayout.setContentsMargins(0, 6, 0, 6);

    layout.addWidget(&toolbar);
    layout.addWidget(&tableSort.table);
    layout.setSpacing(0);
    layout.setContentsMargins(0, 0, 0, 0);

    tableSort.setColumnResize({0});

    settings::restoreWindowState("companies", this, QSize{400, 500});
}

void CompaniesWindow::closeEvent(QCloseEvent *event) {
    settings::saveWindowState("companies", this);
}

void CompaniesWindow::keyPressEvent(QKeyEvent *event) {
    if (!tableSort.focusFilter(event)) QDialog::keyPressEvent(event);
}
