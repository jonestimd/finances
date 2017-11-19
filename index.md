---
title: Overview
---

# Overview

Finances is an application for tracking personal finances using a SQL database.

It requires a [Java 8](https://java.com/en/download/) runtime environment.

## Database support

The application data is stored in a SQL database.  The supported databases are
[MySql](https://dev.mysql.com/downloads/mysql/), [PostgreSQL](https://www.postgresql.org/download/)
and [Derby](https://db.apache.org/derby/).

When using `MySql` or `PostgreSQL`, the database runs as a separate application
and must be installed separately.  The main advantage of using `MySql` or `PostgreSQL`
is that multiple instances of the **Finances** application can access the data concurrently.

When using `Derby`, the database runs as part of the **Finances** application
and does not require a separate installation.  However, only one instance of the
**Finances** application can access the data at any given time.
