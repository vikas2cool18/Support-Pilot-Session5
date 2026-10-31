# Acme Cloud User Guide

Version 3.4 · For account owners, admins and team members

Acme Cloud is an all-in-one platform for small businesses: contacts and customers, invoices and payments, reports and dashboards, and integrations with the tools you already use. This guide explains how to use the web dashboard and the mobile app.

## Getting started

### Creating your account

Go to app.acmecloud.example and choose **Sign up**. Enter your business name, work email and a password of at least 12 characters. We send a verification email; click the link within 24 hours to activate the account. The person who signs up becomes the **account owner**.

New accounts start on the FREE plan. You can upgrade to PRO or ENTERPRISE at any time from **Settings → Billing → Change plan**. Your data is kept when you upgrade or downgrade.

### Signing in

Sign in at app.acmecloud.example with your email and password, or with Google Workspace or Microsoft 365 single sign-on if your admin has enabled it. After five failed attempts the account is locked for 15 minutes to protect it.

### Resetting your password

1. On the sign-in page, click **Forgot password?**
2. Enter the email address you use for Acme Cloud.
3. Open the email from no-reply@acmecloud.example and click **Reset password**. The link is valid for 30 minutes.
4. Choose a new password of at least 12 characters. It must not match your last five passwords.

If the email does not arrive within five minutes, check your spam folder. Admins can also send a reset link from **Settings → Users**.

## Security settings

### Two-factor authentication (2FA)

We strongly recommend 2FA for every user and admins can make it mandatory.

1. Go to **Profile → Security → Two-factor authentication**.
2. Choose **Authenticator app** (recommended) or **SMS**.
3. For an authenticator app, scan the QR code with Google Authenticator, Microsoft Authenticator or a similar app and enter the six-digit code.
4. Save your ten **backup codes** somewhere safe. Each code works once.

If you lose your phone, sign in with a backup code and set up 2FA again. If you have no backup codes, ask your account owner to reset 2FA for you from **Settings → Users → Reset 2FA**. Acme Cloud support cannot bypass 2FA without verifying your identity with the account owner.

### Sessions and devices

**Profile → Security → Active sessions** lists every device signed in to your account. Click **Sign out** next to a device you do not recognise, then change your password.

## Users and roles

### Adding team members

Account owners and admins can add users from **Settings → Users → Invite user**. Enter one or more email addresses and choose a role. The invitation is valid for seven days. Each plan has a user limit; see the Plans and Pricing document.

### Roles

| Role | What they can do |
|---|---|
| Owner | Everything, including billing, closing the account and transferring ownership |
| Admin | Manage users, settings, integrations and all data; cannot close the account |
| Member | Create and edit contacts, invoices and reports |
| Read-only | View data and download reports; cannot change anything |

To give someone read-only access, invite them with the **Read-only** role, or change an existing user's role from **Settings → Users → Edit role**.

### Removing users

Removing a user signs them out of every device immediately. Records they created are kept and reassigned to the account owner.

## Contacts

### Importing contacts

Go to **Contacts → Import** and upload a CSV or Excel file of up to 50,000 rows. Map your columns to Acme Cloud fields, review the preview, then click **Start import**. Large imports run in the background; you get an email when they finish. If an import fails, the email lists the rows with errors. Nothing is deleted when an import fails.

### Exporting contacts

Go to **Contacts**, select the contacts you want (or **Select all**), then click **Export → CSV** or **Export → Excel**. Exports of more than 10,000 contacts are emailed to you as a download link valid for 48 hours. Read-only users can export too.

## Invoices and payments

### Creating an invoice

Go to **Invoices → New invoice**, choose a customer, add line items and taxes, and click **Send**. Invoices for Indian customers include your GSTIN and the customer's GSTIN when you have entered them in **Settings → Business details**.

### Downloading invoices

Open an invoice and click **Download PDF**. To download many invoices at once, select them in the list and choose **Export → PDF (zip)**. A single combined PDF of all invoices is not currently available.

### Online payments

Customers can pay invoices online by card, UPI or net banking when you connect a payment gateway under **Settings → Integrations → Payments**.

## Reports and dashboards

The dashboard shows revenue, outstanding invoices and new customers for the selected period. Reports can be filtered by date, customer and product, and downloaded as PDF or Excel.

### Scheduled reports

On PRO and ENTERPRISE plans you can email any report automatically: open the report, click **Schedule**, choose daily, weekly or monthly, and add recipients.

### Report performance

Reports covering more than two years of data can take up to a minute to load. Narrow the date range for faster results.

## Integrations

Acme Cloud integrates with Google Workspace, Microsoft 365, Slack, Razorpay, Stripe and Zapier. Go to **Settings → Integrations**, choose the tool and follow the steps. Admin rights are required. Accounting integrations currently available are Zoho Books and QuickBooks.

## Mobile app

The Acme Cloud app for Android and iOS lets you view contacts, create invoices and check the dashboard. Dark mode is available from version 3.3: **Profile → Appearance → Dark**. The app supports the same two-factor authentication as the web dashboard.

## Language and region

Change the dashboard language from **Profile → Preferences → Language**. Available languages are English, Hindi, Tamil, Marathi and Bengali. Currency and date formats follow your business country, set in **Settings → Business details**.

## Closing your account

Only the account owner can close an account, from **Settings → Account → Close account**. You can download a full export of your data first. After closing, data is kept for 30 days in case you change your mind, and then permanently deleted. See the Security and Data Residency document for details.
