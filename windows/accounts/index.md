---
title: Accounts window
sections:
- Account properties
- Window toolbar
---
# ![]({{ "/assets/images/accounts-icon.png" | relative_url }}) Accounts

<img class="screen-shot" src="accounts-window.png" width="379" title="Accounts Window"
     alt="Accounts Window"/>
Every transaction is associated with at least one account (transfers are associated with
two accounts).  Each account can be associated with a company.  The
combination of company and account name must be unique.  The **Accounts** window
displays all of the accounts and their balances and it can be used to add, delete
or modify accounts.  The window also shows the number of transactions for each
account.  The **Accounts** window can be accessed from a **Transactions** window
by clicking on the ![accounts]({{ "/assets/images/accounts-icon.png" | relative_url }}){:.button}
button in the toolbar.

## Account properties
Each account has the following properties.

| Property | Description |
|---|---|
| Company Name | name of the company that holds the account |
| Name | name of the account |
| Type | the type of the account (`Bank`, `Brokerage`, `Cash`, `Credit`, `Loan` or `401k`) |
| Description | description of the account |
| Account Number | the account number |
| Closed | true if the account has been closed |
{:.definitions}

## Window toolbar
The following actions are available on the **Accounts** window toolbar.

![Open]({{ "/assets/images/register-icon.png" | relative_url }}){:.button} Transactions...
: Open a **Transactions** window for the selected account.

![Add](newAccount.png){:.button} New Account
: Add a new account.

![Delete](deleteAccount.png){:.button} Delete
: Delete the selected account.  Only enabled for an empty account (no transactions).

![Save]({{ "/assets/images/save.png" | relative_url }}){:.button} Save
: Save changes to the accounts.

![Reload]({{ "/assets/images/reload.png" | relative_url }}){:.button} Reload Accounts
: Reload the accounts, discarding any unsaved changes.

![Companies](company.png){:.button} Companies...
: Display the **Companies** dialog which can be used to add, delete or rename
companies.

