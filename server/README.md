# FloodCue — optional IMD data source (HelioHost or any PHP hosting)

The **Flood Alerts** screen works out of the box using the NDMA SACHET feed,
which is fetched directly from the phone with no server and no key.

IMD district rainfall warnings are an optional extra. IMD's API
(<https://api.imd.gov.in/public/api_reference.html>) rejects unauthenticated
requests with `401` and binds each issued key to one server address, so the key
must stay on a server instead of inside the APK. `imd_proxy.php` is that server.

## 1. Get an IMD key

1. Register at <https://api.imd.gov.in/public/register.php>.
2. Create a development API key. When it asks for a server address, use the
   outgoing IP of your hosting account (HelioHost shows it in cPanel; you can
   also check it by loading `https://api.ipify.org` from a PHP file there).
3. Copy the key.

## 2. Upload the proxy

1. Open the HelioHost file manager (or FTP) and go to `public_html`.
2. Upload `imd_proxy.php` from this folder, keeping the file name.
3. Edit the file and replace `PASTE_YOUR_IMD_API_KEY_HERE` with your key.
4. Open `https://yourname.heliohost.org/imd_proxy.php` in a browser. IMD will
   still protect the data, so you may see a JSON error from IMD — that is
   normal and proves the proxy is reachable and the key is being sent. A PHP
   error means fix the file; a `404` means wrong folder.

If the account gives you a JWT as well as a key, put it in `$IMD_JWT`.

## 3. Point the app at it

In FloodCue: **Home → Flood Alerts → IMD data source**, paste the URL and
press Set. The key is never stored on the phone, only the proxy address.

Leave the field empty to hide the IMD section; SACHET alerts keep working.

## Notes

- Use `https://`. If HelioHost only gives you `http://` for this domain, tell
  me and I will add a narrow cleartext exception for that one host.
- `?endpoint=districtnowcast` returns IMD's short-nowcast product instead of
  the five-day district warning.
- IMD has no public flood/river-level API, and CWC does not publish a
  machine-readable flood forecast, so FloodCue links to the official portals
  for those instead of scraping them.
