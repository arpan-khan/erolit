# Security policy

## Supported versions

Security fixes are applied to the latest release only. Please update to the newest version before reporting an issue.

## Reporting a vulnerability

Please do **not** open a public issue for a security problem. Use GitHub's private vulnerability reporting on the repository's **Security** tab:

https://github.com/arpan-khan/erolit/security/advisories/new

If private reporting is unavailable, contact the repository owner through GitHub and include enough detail to reproduce the issue.

## Secrets

Never commit any of the following:

- `keystore.properties`
- `*.jks`, `*.keystore`, or other signing files
- `local.properties`
- session IDs, bearer tokens, cookies, or account credentials
- captured pages, raw API responses, or story text containing personal data

If a signing key or password is exposed, replace the key and rotate the password immediately. If an account session is exposed, sign out of the account to invalidate the session.
