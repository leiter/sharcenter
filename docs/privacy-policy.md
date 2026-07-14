# Privacy Policy for ShareCenter

**Last Updated: July 14, 2026**

## Overview

ShareCenter ("the App") is a personal social-content manager: you share links into it,
it builds a curated library, and you compose posts from what you saved. The App is
developed by **Silent Force**. We are committed to protecting your privacy. This
policy explains how the App handles your information.

## Data Collection

**We do not collect, store, or transmit any personal data to servers operated by us.**

There is no ShareCenter account and no ShareCenter backend. Your library, your drafts and
your settings live on your device.

## Information Stored Locally

The App stores the following information **locally on your device only**:

- **Links library**: the links you share into the App, together with the titles, authors, descriptions and thumbnails it fetched for them
- **Handles, hashtags and keywords**: the tags you attach to links, and the accounts saved when you share a profile
- **Post drafts**: the posts you compose, including their order
- **App preferences**: theme, accent color, display formats, default filters and sort orders
- **Backups and exports**: files the App writes at your request
- **Optional X session cookies**: if you choose to sign in to X inside the App, the resulting session cookies are stored on your device by the system WebView

This data is not accessible to us or to any third party we control.

## Network Requests

The App has no backend of its own, but it does contact **third-party servers directly
from your device** in order to work:

| When | Where the request goes | What is sent |
|------|------------------------|--------------|
| You share a link into the App | The public endpoint of the platform the link belongs to (e.g. YouTube, X, Bluesky, Mastodon, TikTok, Reddit), and the link's own host when a short URL has to be resolved | The shared URL |
| A saved link shows a thumbnail | The image host referenced by that link | A plain image request |
| You open the ECI module | The European Citizens' Initiative portal (`register.eci.ec.europa.eu`) | A request for public signature statistics |
| You sign in to X in the App (optional) | X | Whatever you enter in X's own login page, which is displayed in a WebView |

These requests are unauthenticated and use each platform's **public** endpoints. They
are made only in response to something you do. As with any network request, the
receiving platform can see your IP address and will handle it under **its own privacy
policy**, not ours.

## Device Permissions

The App requests the following permissions to function:

| Permission | Purpose |
|------------|---------|
| **Internet** | To fetch titles, authors and thumbnails for the links you share, and public statistics for the ECI module |
| **Read storage** | To import a library, restore a backup, and read images you share into the App (Android 9 and older) |
| **Write storage** | To export your library and write backups (Android 9 and older) |

These permissions are used solely for the stated purposes.

## Publishing

When you publish a draft, the App hands it to Android's system share sheet and you
choose the target app (for example WhatsApp, LinkedIn, Bluesky, Mastodon or Telegram).
The App does not post on your behalf and does not send your draft anywhere until you
pick a target.

## Third-Party Services

The App does not use any analytics, advertising, crash-reporting or tracking services.

## Data Sharing

We do not share any data with third parties, because we do not collect any.

## Data Security

Since all your content remains on your device, its security is protected by your
device's built-in security features (screen lock, encryption).

## Children's Privacy

The App does not knowingly collect any information from children. It does not require
any personal information to function.

## Changes to This Policy

We may update this Privacy Policy from time to time. Any changes will be reflected in
the "Last Updated" date above.

## Contact Us

If you have questions about this Privacy Policy, please contact us at:

**Email**: [sharecare@cutthecrap.link](mailto:sharecare@cutthecrap.link)

**Developer**: Silent Force

---

## Summary

- No account, no ShareCenter server
- No tracking
- No analytics
- No ads
- Your library stays on your device; links you share are enriched using the platforms' public endpoints
