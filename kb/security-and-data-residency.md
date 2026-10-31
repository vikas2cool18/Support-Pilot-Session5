# Security and Data Residency

This document describes where Acme Cloud stores customer data, how it is protected and how long it is kept.

## Where your data is stored

Acme Cloud runs in two cloud regions:

| Business country | Primary region | Backup region |
|---|---|---|
| India | Mumbai (ap-south-1) | Hyderabad (ap-south-2) |
| All other countries | Frankfurt (eu-central-1) | Dublin (eu-west-1) |

Data for accounts whose business country is India is stored and processed **only in India**: primary data in Mumbai and encrypted backups in Hyderabad. This applies to all Indian accounts created since version 3.0 (October 2025). Older Indian accounts were migrated to the Mumbai region by March 2026.

ENTERPRISE customers can request a written data-residency confirmation for audits from their account manager.

## Encryption

- All data is encrypted in transit with TLS 1.2 or higher.
- Data at rest — databases, file storage and backups — is encrypted with AES-256.
- Encryption keys are managed in a cloud key-management service and rotated every year. ENTERPRISE customers can use their own keys (BYOK).

## Access control

Only a small group of Acme Cloud engineers can access production systems, through single sign-on with two-factor authentication. All access is logged. Support staff can view your account data only after you grant temporary access from **Settings → Support access**, and that access expires automatically after 72 hours.

## Backups

Databases are backed up every hour and backups are kept for 35 days. Backups are tested by restoring them every month.

## Data retention and deletion

- While your account is active, your data is kept until you delete it.
- When an account is closed, data is kept for 30 days so the account can be reopened, then permanently deleted from live systems.
- Deleted data disappears from backups after the 35-day backup retention period.
- You can export all your data at any time from **Settings → Account → Export data**.

## Compliance

Acme Cloud processes personal data in line with India's Digital Personal Data Protection Act, 2023 and, for customers in the European Union, the GDPR. We act as a data processor for the customer data you store in Acme Cloud. Our data processing agreement is available from sales.

## Reporting a security issue

Report suspected vulnerabilities or account compromise to security@acmecloud.example. We acknowledge reports within one working day.
