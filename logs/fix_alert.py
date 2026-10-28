# Fixes for FloodAlertActivity.java (alert feature).
# Operates on the CRLF file exactly; fails loudly on any anchor mismatch.
import re
import sys

p = 'app/src/main/java/com/floodcue/app/FloodAlertActivity.java'
raw = open(p, encoding='utf-8', newline='').read()
crlf = raw.count('\r\n')
lf_total = raw.count('\n')
print("CRLF:", crlf, "LF-total:", lf_total, "uniform CRLF:", crlf == lf_total)
s = raw.replace('\r\n', '\n')


def rep(old, new, count=1, label=""):
    global s
    n = s.count(old)
    if n != count:
        raise SystemExit("ANCHOR FAIL [%s]: found %d expected %d\n%r" % (label, n, count, old[:90]))
    s = s.replace(old, new, count)


# 1. refresh(): splice out the broken thread (never fetches) and insert a real
#    fetch + cache-save + honest UI states, plus the notificationsBlocked() helper.
start = s.find("        new Thread(() -> {\n            boolean offline = false;")
if start < 0:
    raise SystemExit("refresh start anchor missing")
endm = s.find("        }).start();\n    }", start)
if endm < 0:
    raise SystemExit("refresh end anchor missing")
endm += len("        }).start();\n    }")
new_refresh = '''        new Thread(() -> {
            try {
                String rss = null;
                boolean offline = false;
                try {
                    rss = httpGet(SACHET_RSS_URL, FETCH_TIMEOUT_MS);
                } catch (IOException networkDown) {
                    offline = true;
                }
                // A body that small is an error page, not the feed.
                if (rss != null && rss.length() < 200) {
                    rss = null;
                }

                List<Alert> matched = new ArrayList<>();
                boolean parsed = false;
                if (rss != null) {
                    List<Alert> all = parseRss(rss);
                    parsed = !all.isEmpty();
                    if (parsed) {
                        // Known severity/area from an earlier read of these ids
                        // helps matching and the chips without extra fetches.
                        restoreSeverity(all);
                        matched = filterByDistrict(all, wantedDistrict);
                        enrich(matched);
                        // The cache only changes after a feed really arrived and
                        // parsed, so a failed refresh can never wipe saved alerts.
                        preferences().edit()
                                .putString(KEY_RSS_CACHE, rss)
                                .putLong(KEY_RSS_CACHE_AT, System.currentTimeMillis())
                                .apply();
                    }
                }

                final String freshRss = parsed ? rss : null;
                final List<Alert> freshMatched = matched;
                final boolean wasOffline = offline;

                runOnUiThread(() -> {
                    loading = false;
                    refreshButton.setEnabled(true);
                    locationButton.setEnabled(true);

                    if (freshRss == null) {
                        renderSachetFromCache();
                        showStatus(wasOffline
                                ? R.string.flood_alert_offline
                                : R.string.flood_alert_error);
                        return;
                    }

                    renderSachet(freshMatched, false, System.currentTimeMillis());
                    if (freshMatched.isEmpty()) {
                        showStatus(getString(R.string.flood_alert_no_matching, district));
                    } else {
                        showStatus(R.string.flood_alert_feed_updated);
                    }
                    notifyNew(this, freshMatched, district);
                    if (!freshMatched.isEmpty() && notificationsBlocked()) {
                        // The warnings are on screen but cannot announce themselves.
                        showStatus(R.string.flood_alert_notifications_blocked);
                    }
                });
            } catch (RuntimeException feedBroken) {
                // A parsing or prefs failure must not leave the buttons dead.
                runOnUiThread(() -> {
                    loading = false;
                    refreshButton.setEnabled(true);
                    locationButton.setEnabled(true);
                    renderSachetFromCache();
                    showStatus(R.string.flood_alert_error);
                });
            }
        }).start();
    }

    /** Android 13+ needs POST_NOTIFICATIONS for an alert to reach the shade. */
    private boolean notificationsBlocked() {
        return Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(this,
                        Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED;
    }'''
s = s[:start] + new_refresh + s[endm:]
print("refresh(): replaced broken fetch thread")

# 2. remove orphaned applyResults() (glued onto httpGet's closing brace)
start = s.find("    }private void applyResults(String freshRss,")
if start < 0:
    raise SystemExit("applyResults anchor missing")
endm = s.find("    // ----------------------------------------------------------------- parse", start)
if endm < 0:
    raise SystemExit("parse header anchor missing")
s = s[:start] + "    }\n\n" + s[endm:]
print("applyResults(): removed orphan")

# 3. remove dead showNotificationBlockedToast() + notifyNewSafe()
start = s.find("    /** Show a brief Snackbar-like toast if notifications are blocked")
if start < 0:
    raise SystemExit("blocked-toast anchor missing")
endm = s.find("    private static Notification buildNotification(", start)
if endm < 0:
    raise SystemExit("buildNotification anchor missing")
s = s[:start] + s[endm:]
print("showNotificationBlockedToast()+notifyNewSafe(): removed dead code")

# 4. remove ImdResult class (IMD never parsed anywhere in this file)
s2 = re.sub(r"\n\n    private static final class ImdResult \{.*?\n    \}\n", "\n", s, count=1, flags=re.S)
if s2 == s:
    raise SystemExit("ImdResult anchor missing")
s = s2
print("ImdResult: removed")

# 5. remove withEndpoint() (IMD proxy leftover, no callers)
s2 = re.sub(r"\n    private static String withEndpoint\(String proxy, String endpoint\) \{.*?\n    \}\n", "\n", s, count=1, flags=re.S)
if s2 == s:
    raise SystemExit("withEndpoint anchor missing")
s = s2
print("withEndpoint(): removed")

# 6. imports: drop unused and duplicated ones
rep("import androidx.appcompat.app.AppCompatActivity;\n", "", label="AppCompat")
rep("import com.google.android.material.dialog.MaterialAlertDialogBuilder;\n", "", label="MaterialDialog")
rep("import org.json.JSONArray;\n", "", label="JSONArray")
rep("import java.util.regex.Pattern;\n\nimport java.io.File;\nimport java.io.IOException;\nimport java.io.File;\nimport java.io.IOException;\nimport com.floodcue.app.OfflineMapCatalog;\n",
    "import java.util.regex.Pattern;\n", label="stray import block")
print("imports: cleaned")

# 7. class javadoc: honest sources list (IMD not wired into this screen)
rep(''' * 2. IMD district rainfall warnings (api.imd.gov.in) - IMD issues an API key
 *    that is bound to a single server address, so FloodCue calls it through
 *    the user's own proxy (server/imd_proxy.php). Until a proxy address is
 *    configured the section says so instead of pretending to have data.
 * 3. CWC river/flood forecasts have no verified machine-readable endpoint,
 *    so the official portal is linked rather than scraped.
''', ''' * 2. CWC river/flood forecasts have no verified machine-readable endpoint,
 *    so the official portal is linked rather than scraped.
 *
 * (IMD district warnings need an API key bound to one server address, so they
 * are only readable through a user-hosted proxy - server/imd_proxy.php - which
 * this screen does not wire up yet.)
''', label="javadoc")
print("javadoc: updated")

# 8. notified-ID persistence: write comma-separated; still read the legacy
#    " . " separator so alerts announced before the fix stay de-duplicated.
rep('''        for (String id in-placeholder''', '', label="never") if False else None
rep('''        for (String id : prefs.getString(KEY_NOTIFIED, "").split(",")) {
            if (!id.trim().isEmpty()) {
                notified.add(id.trim());
            }
        }''', '''        // Older versions joined ids with " . "; split both ways so alerts
        // announced before that fix are still de-duplicated.
        for (String chunk : prefs.getString(KEY_NOTIFIED, "").split(",")) {
            for (String id : chunk.split("\\u00b7")) {
                if (!id.trim().isEmpty()) {
                    notified.add(id.trim());
                }
            }
        }''', label="notified read")
rep('''        prefs.edit().putString(KEY_NOTIFIED, join(new ArrayList<>(notified))).apply();''',
    '''        StringBuilder stored = new StringBuilder();
        for (String id : notified) {
            if (stored.length() > 0) {
                stored.append(',');
            }
            stored.append(id);
        }
        prefs.edit().putString(KEY_NOTIFIED, stored.toString()).apply();''', label="notified write")
print("notifyNew(): dedup persistence fixed")

# 9. renderSachetFromCache: restore severity/area BEFORE district filtering
rep('''        List<Alert> matched = filterByDistrict(parseRss(cached), district);
        restoreSeverity(matched);
        renderSachet(matched, true, preferences().getLong(KEY_RSS_CACHE_AT, 0L));''',
    '''        List<Alert> all = parseRss(cached);
        // Severity and area come back from the cache before matching, so cached
        // area names also count for "is this alert for my district".
        restoreSeverity(all);
        List<Alert> matched = filterByDistrict(all, district);
        renderSachet(matched, true, preferences().getLong(KEY_RSS_CACHE_AT, 0L));''',
    label="cache restore order")
print("renderSachetFromCache(): restore-before-filter")

# 10. severity chip names from string resources instead of hardcoded English
for word, key in (("Red", "red"), ("Orange", "orange"), ("Yellow", "yellow"), ("Green", "green")):
    rep('return "%s";' % word, 'return getString(R.string.flood_alert_severity_%s);' % key, label="sev %s" % word)
print("severityName(): localized")

# tidy runs of 4+ newlines introduced by removals
s = re.sub(r"\n{4,}", "\n\n", s)

if crlf == lf_total:
    s = s.replace('\n', '\r\n')
open(p, 'w', encoding='utf-8', newline='').write(s)
print("WROTE", p, "bytes:", len(s.encode('utf-8')))
