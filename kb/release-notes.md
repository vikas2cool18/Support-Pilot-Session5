# Acme Cloud Release Notes

## Version 3.4 — September 2026

### New
- Scheduled reports can now be sent to Slack channels as well as email.
- Bulk invoice export as a zip of PDFs (**Export → PDF (zip)**).
- Bengali added as a dashboard language.

### Improved
- Contact imports up to 50,000 rows (previously 20,000).
- Faster dashboard loading for accounts with more than 100,000 invoices.

### Fixed
- The invoice **Download PDF** button did nothing in Firefox 131 and later.
- Two-factor SMS codes were delayed for some Jio and Vi numbers.

### Known issues
- Reports covering more than two years can take up to a minute to load. Workaround: narrow the date range.

## Version 3.3 — July 2026

### New
- **Dark mode in the mobile app** (Android and iOS): **Profile → Appearance → Dark**.
- UPI AutoPay for subscription payments.
- Marathi added as a dashboard language.

### Improved
- API rate limit for PRO increased to 600 requests per minute.

### Fixed
- Scheduled reports were sent one hour late after daylight-saving changes for recipients outside India.

## Version 3.2 — April 2026

### New
- Read-only role for users who only need to view data and download reports.
- Razorpay payment gateway integration.
- Hindi and Tamil dashboard languages.

### Changed
- Minimum password length increased from 8 to 12 characters for new passwords.

### Fixed
- GSTIN was missing on invoices for some customers in Union Territories.

## Version 3.1 — January 2026

### New
- Public API v2 (`/v2/...` endpoints). API v1 is deprecated and will be switched off on 31 March 2027.
- Activity log for admins in **Settings → Activity**.

## Version 3.0 — October 2025

### New
- Completely redesigned dashboard.
- Microsoft 365 single sign-on.
- Data residency in India (Mumbai region) for all new Indian accounts; see Security and Data Residency.
