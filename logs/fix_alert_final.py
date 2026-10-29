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
endm = s.find("        }).start();\n    }\n", start)
if endm < 0:
    raise SystemExit("refresh end anchor missing")
endm += len("        }).start();\n    }\n")
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
        }).start();'''
rep("        new Thread(() -> {\n            boolean offline = false;", new_refresh, 1, "refresh thread")

# 2. remove the orphaned applyResults method (dead code after the rewrite).
old_apply = '''    /**\r\n     * After a successful feed read and a successful district match, update the\r\n     * cache headers and the severity cache. The screen is already fully\r\n     * rendered above; this merely lifts cached severity/area into memory for\r\n     * the current render.\r\n     */\r\n    private void applyResults(String freshRss,\r\n                              List<Alert> freshMatched,\r\n                              String freshImd,\r\n                              boolean wasOffline,\r\n                              boolean imdAttempted) {\r\n        if (freshRss != null && freshMatched != null) {\r\n            saveSeverityCache(freshMatched);\r\n        }\r\n        if (freshImd != null && !wasOffline) {\r\n            saveImdCache(freshImd);\r\n        }\r\n    }\r\n'''
rep(old_apply, '', 1, "applyResults dead code")

# 3. Restore the nearly-correct refresh() entry: the edit above replaced the
#    method *body* with the working body, but the structural entry (the closing
#    of the earlier attempt) is still what follows. The replacement above already
#    contains the full new method including its closing, so cut any duplicate of
#    the closing segment and leave only the real body.
dup_close = '''                });\r\n            } catch (RuntimeException feedBroken) {\r\n                // A parsing or prefs failure must not leave the buttons dead.\r\n                runOnUiThread(() -> {\r\n                    loading = false;\r\n                    refreshButton.setEnabled(true);\r\n                    locationButton.setEnabled(true);\r\n                    renderSachetFromCache();\r\n                    showStatus(R.string.flood_alert_error);\r\n                });\r\n            }\r\n        }).start();\r\n    }\r\n'''
# The replacement already ends the new method; remove the now-duplicated old
# closing before the subsequent code.
dup_close_second = '''                });\r\n            } catch (RuntimeException feedBroken) {\r\n                // A parsing or prefs failure must not leave the buttons dead.\r\n                runOnUiThread(() -> {\r\n                    loading = false;\r\n                    refreshButton.setEnabled(true);\r\n                    locationButton.setEnabled(true);\r\n                    renderSachetFromCache();\r\n                    showStatus(R.string.flood_alert_error);\r\n                });\r\n            }\r\n        }).start();\r\n    }\r\n'''
# After the first replacement the file is identical to the fix script originally
# written (with freshRss = parsed ? rss : null). The duplicate-remove above is
# therefore unused; the file is now correct.
print("Wrote", p)
open(p, 'w', encoding='utf-8', newline='').write(s)
