# FloodCue Frontend Design Guide

## Purpose

This document is the handoff reference for continuing FloodCue's Android UI work. FloodCue uses XML layouts and Java activities. Keep the existing emergency, authentication, navigation, offline, storage, and backend behavior intact while applying this visual language to remaining screens.

## Verified Git starting point

- Repository: `E:\FloodCue`
- Working branch: `contacts`
- Requested baseline commit: `28375c1` (`Working hmm`), also available as `origin/contacts`
- Current checkout at documentation time: `91167ad` (`feat(ui): complete FloodCue Aurora Blue design system and screen redesign`)
- The checkout already contained uncommitted UI changes before this document was added. They are preserved; do not reset, clean, or overwrite them.

## Design direction

Use a calm emergency-technology visual system: deep blue brand surfaces, clean light backgrounds, restrained gradients, rounded cards, clear hierarchy, and a red SOS treatment reserved for urgent actions. The UI should feel dependable rather than alarming. Avoid decorative elements that compete with emergency actions.

## Theme behavior

The app theme inherits from `Theme.MaterialComponents.DayNight.NoActionBar`, so Android's system light/dark setting selects the resource configuration automatically. Do not add a manual theme toggle. Put theme-dependent values in `values/` and `values-night/`; do not hardcode screen background or primary text colors in layouts.

### Core tokens

| Role | Light | Dark |
| --- | --- | --- |
| App background | `#F5F8FC` | `#0C1422` |
| Main surface | `#FFFFFF` | `#151F30` |
| Elevated surface | `#FFFFFF` | `#1B2940` |
| Primary text | `#152238` | `#F2F6FC` |
| Secondary text | `#5D6B82` | `#B0BCD0` |
| Border | `#DCE5F0` | `#2C3B52` |
| Brand blue | `#FF9800` | `#FF9800` |
| Brand pressed | `#F57C00` | `#F57C00` |
| SOS red | `#C62828` | `#C62828` |
| Success | `#16A34A` | `#16A34A` |
| Warning | `#F59E0B` | `#F59E0B` |

The canonical token resource is `app/src/main/res/values/colors_fc.xml`. Existing compatibility names in `colors.xml` are still used by older screens; migrate them only when touching that screen and verify contrast first.

## Layout and component rules

- Use XML and existing Material Components; do not introduce Compose or Kotlin.
- Use a root background token, then group content in surfaces/cards rather than stacking unrelated colors.
- Prefer 16dp outer spacing, 12–16dp internal spacing, and 12–20dp corner radii.
- Use at least 48dp touch targets for buttons and links.
- Keep primary headings visually prominent, supporting text quieter, and emergency actions unmistakable.
- Use `MaterialButton`, `TextInputLayout`, and existing drawable selectors where they already exist.
- Use icons with meaningful content descriptions and do not rely on color alone for status.
- Keep gradients subtle. Splash branding may use the existing layered gradient/glow resources, but preserve the existing splash timing and routing.

## Screens covered first

The current checkout contains visual work for:

- Splash: `app/src/main/res/layout/activity_splash.xml`, `app/src/main/java/com/floodcue/app/SplashActivity.java`, and splash drawables.
- Home: `app/src/main/res/layout/activity_home.xml` and the existing `HomeActivity.java` behavior.
- Login: `app/src/main/res/layout/activity_login.xml` and the existing `LoginActivity.java` behavior.
- Create account: `app/src/main/res/layout/activity_create_account.xml` and the existing `CreateAccountActivity.java` behavior.

The current working tree also has broader layout/resource changes from work already present before this handoff (`activity_setup.xml`, SOS, weather, settings, contacts, and other screens). Treat those changes as existing user work. Review before editing; do not silently revert them.

## Behavior that must remain untouched

- Splash delay, Firebase session detection, language selection, setup completion routing, and `finish()` behavior.
- Firebase email/password authentication, email verification, password reset, Google sign-in, guest mode, and navigation between login/create-account screens.
- Home SOS, siren, location permission, emergency messaging, offline maps, contacts, weather, safety, setup, and settings actions.
- Manifest, application identity, backend endpoints, Firebase configuration, persistence, and data models.

Java changes should be limited to presentation-only behavior such as view styling or existing status text unless a functional bug is explicitly requested.

## Files changed at handoff

The checkout was already dirty before this document was added. Confirm the exact file list with `git status` before committing. Known changed UI areas include the four screens above, shared colors/themes, reusable drawables, and several additional layouts. `SplashActivity.java` is also modified in the existing worktree; verify its routing against the baseline before changing it further. No backend change is intended by this task.

## Current redesign update

The first frontend pass now explicitly redesigns these four screens:

- Splash: added the FloodCue logo, emergency-readiness eyebrow, stronger brand lockup, updated tagline, status pill, and ambient glow while keeping `loadingStatus`, the spinner, the 1.5-second delay, and routing untouched.
- Login: enlarged the branded security header, refreshed copy, increased card hierarchy, reshaped form fields, and clarified the Google continuation section while keeping every authentication view ID unchanged.
- Create account: refreshed the account-building copy, spacing, card elevation, and form presentation while keeping Firebase-related view IDs and button behavior unchanged.
- Language: strengthened the onboarding header and globe treatment, increased title scale, and reshaped both language selection cards while keeping the picker IDs and save/restart flow unchanged.

The redesign uses existing resources such as `ic_floodcue_logo`, `ic_globe`, `bg_splash_brand_glow`, and the current theme tokens. No manual theme toggle was added.

The visual direction was then refined to a non-blue glassmorphism palette. Light mode uses warm ivory, muted terracotta, and soft sage-neutral surfaces. Dark mode uses charcoal, muted plum, and dusty rose accents. Cards and language selectors use translucent surfaces with quiet borders; avoid adding saturated gradients or high-contrast decoration.

Latest correction: the splash screen no longer displays the unapproved `ic_floodcue_logo`; it uses typography and ambient color only. The language content is inside a fill-viewport scroll container with reduced vertical spacing so the Continue action remains reachable on short screens. Language picker rows now use opaque theme-aware surfaces and border tokens so English and native-language labels remain readable in both modes.

Login polish: the security mark now has a larger, centered viewport; Change language is a dedicated bordered action with a globe icon; guest mode is a clear rounded action surface; and the Google button disables the default state animator/elevation so no unwanted outer shade appears.

Theme correction: day and night theme files now use the corresponding background, navigation-bar, primary-color, and system-bar icon resources instead of hardcoded white window colors. Splash text uses dedicated contrast tokens in both configurations so the light-mode copy remains readable over the warm gradient. System theme switching should be retested after a fresh install or activity recreation.

Login header icon correction: the security mark now uses an explicit Android vector `src` and theme-aware tint instead of relying on `app:srcCompat`, which was not rendering reliably in the login header.

The login header mark now uses a larger top margin so it sits lower within the scrollable content and remains visually clear below the screen edge.

Home screen update: the header, offline status chip, hero card, emergency actions, situation tools, survival tools, and settings/profile actions now follow the same warm glassmorphism spacing and copy style. All `HomeActivity` view IDs and click targets remain unchanged; SOS and siren colors retain their emergency meaning.

Latest navigation/theme polish: the language screen now shows bottom navigation only when opened from Settings, not when opened from Login. Login's header mark is positioned lower again. Home secondary tools use a second muted glass surface so the entire screen does not appear as one flat color.

Home and shared navigation polish: SOS and siren surfaces now use muted but readable warm emergency colors with light text, the hero heading uses a high-contrast splash token, and the shared bottom navigation now uses the app's theme-aware glass surfaces, border token, wider insets, taller touch area, and calmer label sizing across all screens.

Emergency Messages screen update: `activity_sos.xml` now uses dedicated rounded emergency panels, tighter vertical rhythm, a warm accent heading, and a clearly visible red Send SOS action. All `SosActivity` view IDs, message composition, recipient chips, location actions, queue controls, and send behavior remain unchanged.

Emergency action text uses a dedicated light token in both day and night resources so red SOS actions remain readable regardless of system theme.

Latest Home/navigation redesign: Home now has a raised glass header and refined hero spacing. Shared bottom navigation now uses dedicated vector icons for Home, Contacts, SOS, and Settings instead of emoji glyphs, with theme-aware tinting, larger touch targets, and the existing navigation destinations unchanged.

Home palette refinement: Home now uses dedicated light/dark sage, lilac, and sand surfaces for its tool groups, plus Home-specific readable text and section-label tokens. This keeps the screen aligned with the warm glass theme without making every card the same brown surface.

SOS screen correction: the duplicate in-content back control is hidden through the shared ribbon system, while the scroll content keeps extra bottom padding so final notes and queue controls remain reachable above bottom navigation.

Contacts reliability and theme correction: `item_call_row.xml` is now a normal self-contained row layout instead of an invalid `<merge>` screen layout, preventing Contacts row inflation crashes. `FloodCueActivity` explicitly recreates activities on configuration changes so Android system light/dark changes are reflected immediately without a manual toggle.

Offline Maps and Weather update: Offline Maps now uses the layered Home palette for its selection card and district actions. Weather uses a stronger dashboard heading and temperature emphasis while preserving refresh, location selection, cache, warning, hourly, and detail view IDs and behavior.

Weather/Flood Alert reliability and UI update: `item_weather_detail.xml` is now a self-contained RecyclerView row instead of an invalid `<merge>` layout, fixing the Weather screen launch crash. Flood Alert now uses a raised lilac panel, stronger heading hierarchy, and a distinct sage refresh action while preserving location, refresh, sachet, river, and external-link behavior.

Latest visibility/detail polish: Flood Alert and Weather scroll content now reserve space above the shared bottom navigation. Weather detail rows use interactive-looking glass cards with clear label/value hierarchy, pressed states, and a non-nested scrolling RecyclerView so the final content can be reached reliably.

Weather final polish: the heading and temperature scale are reduced for better hierarchy, the hourly forecast text is now presented in a padded glass forecast panel with increased line spacing, and the bottom location option has a reserved margin so it remains visible above navigation.

Checklist, Tips, and Emergency Guide update: each screen now uses a role-specific layered surface, smaller hierarchy-safe headings, and extra bottom space above navigation. Checklist emphasizes progress/tabs, Tips uses a contained warning legend, and Emergency Guide reserves the red action treatment for its SOS button; dynamic content containers and existing actions remain unchanged.

Naming and theme coverage update: Home now labels the actions `Contacts`, `Readiness Checklist`, and `Safety Guide`. Home action buttons use tighter horizontal padding and slightly smaller text for longer labels so text remains inside compact two-column cards across screen widths. Profile Details, Emergency Contacts, and Settings now use normalized inner spacing, card surfaces, and compact button typography while preserving their existing IDs and actions. Profile Details sets the shared toolbar title to `User Profile` and adds one visible `Edit` action on the toolbar right side; the legacy inline edit/back row is hidden to avoid duplicate or clipped actions. Its dynamically inflated field row is a simple vertical label/value item; it must not include the shared screen base layout, toolbar, or content container. The profile outer container stays transparent so each semantic section can render as its own card. Settings secondary action text uses the theme-aware `home_card_text` token. Checklist mode buttons now have 12dp horizontal text padding, 50dp height, and lilac/sand surfaces for a softer visual distinction; the same surfaces are reapplied when the mode changes. A complete `values-night/colors_fc.xml` token set was added so screens still using the older `fc_*` resources also switch to the dark palette when Android system theme changes.

Setup screen update: The three-step setup flow keeps its existing IDs, validation, skip/back/continue behavior, profile persistence, and Bridgefy action. Its header now uses the shared lilac palette with readable primary/secondary text, the progress bar uses theme-aware FloodCue tokens, and the bottom navigation uses the same secondary/primary button surfaces and padding as the rest of the app.

Setup header spacing correction: `Build your safety profile` and its supporting step content now sit lower with extra top padding, keeping the shared back arrow intact because it remains the screen's working navigation affordance.

Setup layout refinement: Each setup step now uses the same 8dp inner side spacing as other screens, while preserving the existing section order and IDs. This gives the form groups more usable width and keeps the three-step arrangement visually consistent without changing navigation or persistence behavior.

Setup back behavior: On the first setup page, the shared back arrow now explicitly returns to `LoginActivity` for the normal setup flow. When Setup was opened from Profile Details with `EXTRA_EDIT_PROFILE`, the same back action still finishes Setup and returns to the profile screen. Later setup pages continue to move back one step.

Setup header final spacing: The `Build your safety profile` header block has an additional 8dp top offset and slightly more reserved height, keeping it comfortably below the shared toolbar/back arrow.

Setup header box correction: The complete setup header container is now moved down with 28dp outer top spacing; this shifts the title, subtitle, progress indicator, and step label together instead of moving only the title text.

Setup toolbar overlap correction: The Setup content container now starts below the shared toolbar using `?attr/actionBarSize`, preventing the header card from covering the shared back arrow. Its own top padding is kept at 16dp so the title box does not become excessively low after the structural fix.

Pending screen refresh: Offline Map View now has slightly tighter map/status framing; SOS Emergency uses normalized content width, padded action buttons, and a shared toolbar title; About Bridgefy uses normalized content width, a smaller heading, padded actions, theme-aware secondary text, and a shared toolbar title. Existing map lifecycle, SOS delivery/siren/location logic, Bridgefy links, and back actions remain unchanged.

Offline map download progress: District downloads now report actual downloaded bytes through an optional `OfflineMapCatalog.DownloadListener`. `OfflineMapsActivity` switches from indeterminate to determinate progress when the server provides a content length, updates the district percentage text, opens the map only after completion, and restores buttons with an error message on failure. The original `download(context, map)` API remains available for compatibility.

About FloodCue update: The information card now uses normalized inner spacing, a smaller hierarchy-safe heading, a theme-aware version badge, and a shared toolbar title. Content, version lookup, and back navigation remain unchanged.

Offline Communication update: The content now uses normalized inner spacing so the Bridgefy action and explanatory cards get more usable width. The primary action has comfortable horizontal padding and stronger hierarchy, and the shared toolbar displays the screen title. Bridgefy launch/status behavior and dynamic steps remain unchanged.

Version update: The app `versionName` is now `1.0`; About FloodCue reads the package version at runtime, so its version badge will display `1.0` without a separate hardcoded UI value.

Shared back-ribbon correction: `layout_base_screen.xml` is now the single common orange ribbon with one working back arrow. `FloodCueActivity` wires that arrow to the normal back dispatcher and hides duplicate in-content back rows across Contacts, Maps, Weather, SOS, Settings, Guides, Alerts, Profile, and related screens. This prevents the ribbon/text vertical mismatch and duplicate arrow symbols.

Bottom navigation surface correction: the shared navigation bar no longer uses translucent glass colors. It now uses opaque, theme-specific `bottom_nav_surface` values so it has the same clear, grounded separation as the Home screen in both light and dark modes.

The application also explicitly selects `MODE_NIGHT_FOLLOW_SYSTEM` at process startup, ensuring AppCompat uses the Android system theme instead of retaining a previous mode.

Navigation reliability fix: bottom-nav destination launches now use `FLAG_ACTIVITY_REORDER_TO_FRONT` instead of `CLEAR_TOP|SINGLE_TOP`, so selecting Contacts or another destination does not clear the task stack and appear to navigate backward.

## Validation record

- Repository and Git state: inspected on 2026-10-29.
- Branch and requested baseline commit: verified.
- Source/resource inspection: completed for the theme, splash, home, login, and create-account areas.
- XML parsing: passed for all four redesigned layouts.
- Required Activity-facing IDs: confirmed present for splash, login, create-account, and language screens.
- Palette refinement: light/dark resource values updated to the non-blue glassmorphism combinations described above.
- Gradle build/tests: not run yet in this handoff. Do not describe the UI as build-verified until a real Gradle build completes.
- Runtime light/dark visual verification: not run yet. Check both system modes on an emulator/device, including small-height screens and keyboard-open forms.

- Theme audit correction: `values-night/colors.xml` now overrides Material aliases (`colorPrimary`, `colorPrimaryVariant`, `colorOnPrimary`) and glass highlight/shadow tokens. `values-night/colors_fc.xml` now also defines the previously missing semantic, SOS, Google, and splash compatibility tokens. This prevents legacy or Material widgets from falling back to day-mode colors during a system theme switch.

Localization audit update: Added translations for the recently introduced Home headings, Readiness Checklist, Safety Guide, Profile heading, and flood-alert severity labels in Telugu, Hindi, Tamil, Kannada, and Malayalam. Remaining localization work: migrate every hardcoded English layout/Java label into resource keys and audit generated/runtime messages; do not mark localization complete until that inventory is zero.

Localization phase 1: Home hardcoded labels are now resource-backed and translated in all five supported non-English locales. Continue the same pattern for authentication/onboarding layouts and Java-generated messages; never leave user-facing English literals in layouts or activities.

Localization phase 2: Login, Create Account, and Splash visible labels are now resource-backed with translations in Telugu, Hindi, Tamil, Kannada, and Malayalam. Remaining hardcoded strings are now primarily Java runtime messages and Setup screen literals.

Localization phase 3: Splash routing status, Home saved-profile messages, Login failure fallback, and Setup step/status/validation messages now use translated resource keys in all supported locales.

Localization phase 4: Setup header, page headings/intros, and navigation labels are resource-backed and translated in Telugu, Hindi, Tamil, Kannada, and Malayalam. Setup form labels/options and remaining Java-generated user-facing text still require the next audit batch; localization is not yet complete.

Localization phase 5: Setup subtitle/step text and the first personal/allergy section headings now reuse translated resource keys. The rest of the Setup form (field hints, checkbox options, contact/assistance sections, review copy) remains to be migrated; do not treat the app as fully localized yet.

Localization phase 6: Setup page-one full-name hint, blood-group guidance, allergy instruction, and allergy checkbox options are now resource-backed and translated in all supported locales. Medical fields and the remaining contact/assistance/review copy still need migration.

Localization phase 7: Setup medical heading, medication label/hint, and medical-notes label/hint are now resource-backed and translated in all supported locales. Contact, evacuation-assistance, region, and review copy remain in the next batches.

Localization phase 8: Primary/second/third emergency-contact section headings and primary contact labels now reuse translated resources. Required and alternate phone labels are resource-backed with locale translations; remaining contact hints and assistance/region/review copy still require migration.

Localization phase 9: Evacuation-assistance and region labels/options are now resource-backed in the base resource file. Locale translations for these new keys and the remaining contact hints must be added before this phase can be considered complete.

Localization phase 10: Evacuation-assistance and region resources now have Telugu, Hindi, Tamil, Kannada, and Malayalam translations. All six resource files parse successfully; remaining Setup contact hints/review copy and broader application hardcoded strings still need audit.

Localization phase 11: Emergency-contact input hints are resource-backed and translated in all five supported non-English locales. Remaining Setup review/Bridgefy copy and third-party screens still require the hardcoded-string audit.

Localization phase 12: Setup review/checklist, profile-save explanation, Bridgefy information/button, and Settings note are now resource-backed in the base resource file. Their five non-English locale translations still need to be appended and verified before this batch is complete.

Localization phase 13: Telugu translations for all Setup review/checklist, profile-save, Bridgefy, and Settings-note keys are now added. Hindi, Tamil, Kannada, and Malayalam entries remain to be appended; this phase is intentionally incomplete.

Localization phase 14: Hindi, Tamil, Kannada, and Malayalam translations for the Setup review/checklist, profile-save, Bridgefy, and Settings-note keys are now added. All six locale files must be validated together before moving on to the remaining application-wide hardcoded-string audit.

Localization phase 15: Remaining Setup third-contact hints, country/state/district labels, and Setup action buttons are resource-backed. Other-allergy and authentication field hints now use dedicated localized resource files for all supported locales. Full Java/runtime and language-screen audit remains.

Localization phase 16: Language-screen branding and English default labels are now resource-backed and localized. Layout audit now has no remaining literal `text`/`hint` values except values intentionally supplied by runtime data; Java runtime strings still require audit.

Localization phase 17: Setup sign-in/wait/Play Store/local-cache messages and Offline Map location-permission message are now resource-backed and translated in all supported locales. Remaining Java audit items are being checked for technical strings versus user-facing messages; no build has been run yet.

Localization phase 18: Setup blood-group and relationship spinner options now load from localized string arrays, so these options follow the selected app language instead of remaining hardcoded English. States/districts and remaining Java validation/service strings still require migration.

Localization phase 19: Setup state options now load from localized string arrays. A canonical-state mapping preserves the existing district/business logic while displaying the selected app-language state names. District option names remain the next data-localization task.

Localization phase 20: Shared bottom-navigation labels and accessibility descriptions now resolve through the app-language context even on survival-language screens. This preserves the requested split: navigation/options follow app language while safety content can follow survival language.

Localization phase 21: Login email/password and password-reset validation messages are now resource-backed and translated in all supported locales. Create-account validation and remaining service/error messages still require migration.

Localization phase 22: Create-account password-rule labels, required-field validation, password-length/spacing/mismatch messages are now resource-backed and translated in all supported locales. Account-creation service responses and remaining Java literals still require audit.

Localization phase 23: Create-account success/failure, verification-email, duplicate-email, Firebase-policy, rate-limit, network, and unavailable-service messages are now resource-backed and translated in all supported locales.

Localization phase 24: Login service, reset-email, verification, credential, network, and Google-authentication result messages are now resource-backed and translated in all supported locales.

Localization phase 25: Emergency Contacts official service titles/descriptions are now resource-backed; phone numbers and call behavior are unchanged. Locale translations for this catalog and the remaining district/service strings still require completion.

Localization phase 26: Emergency Contacts official service catalog now has Telugu, Hindi, Tamil, Kannada, and Malayalam translations. All catalog resource files parse successfully; district data and remaining Java user-facing strings still require audit.

Localization phase 27: Home local saved-profile detail text now reuses translated profile labels instead of hardcoded English labels. Profile data, SOS behavior, and storage are unchanged.

Localization phase 28: Offline Maps download failure/start messages are now resource-backed and translated in all supported locales. District names and the remaining Java hardcoded-string inventory still require completion.

Localization phase 29: Offline Map location-enable fallback is now resource-backed and translated in all supported locales. Remaining known localization gap is the large district-option dataset plus any final runtime literals found by audit.

Localization phase 30: Andhra Pradesh district options are now resource-backed, preserving the existing district selection logic. The remaining Telangana, Tamil Nadu, Karnataka, and Kerala district arrays still need the same migration.

Localization phase 31: Tamil Nadu, Karnataka, and Kerala district options now load from resource arrays. Telangana remains to be migrated; district names are preserved as proper place names.

Localization phase 32: Telangana district options now load from a resource array. All five state branches are now resource-backed; district names remain proper-name data and use the shared base array unless a locale-specific spelling is supplied.

Localization phase 33: SOS SMS content now resolves through the selected survival-language context for both the Home quick-send path and the SOS Emergency Message path. The emergency body, missing-GPS notice, and location label have localized resources for English, Telugu, Hindi, Tamil, Kannada, and Malayalam. Create-account minimum-password feedback also reuses the existing localized minimum-character resource. SOS recipients, queueing, location acquisition, and SMS dispatch behavior were not changed.

Localization phase 34: Flood-alert severity fallback values now reuse the localized severity resources instead of embedding English fallback labels in the Activity. The alert parsing, severity mapping, caching, and notification behavior are unchanged.

Localization phase 35: Language-screen unknown-code fallback now uses the localized English-language resource instead of an embedded English label. The language option arrays still require a final resource-backed audit so their display names can follow the selected app language without changing stored locale codes.

Localization phase 36: Language picker display names now resolve through string resources rather than a hardcoded English-name array. Locale codes and native-name presentation remain unchanged. Language-name resources now cover base English plus Telugu, Hindi, Tamil, Kannada, and Malayalam app-language variants.

Localization phase 37: Weather condition descriptions now load from localized string arrays instead of a hardcoded Java array. Arrays cover English, Telugu, Hindi, Tamil, Kannada, and Malayalam; weather-code indexing and forecast behavior are unchanged. Compass abbreviations remain language-neutral direction symbols.

Localization phase 38: Offline map style-rendering errors now use localized resources in all six supported locales. The underlying map renderer and error handling are unchanged; the technical exception detail remains interpolated for diagnosis.

Localization phase 39: Create-account invalid-email validation now reuses the existing localized email-validation resource in both validation paths. Account creation behavior is unchanged.

Localization phase 40: Login and create-account navigation prompts now use their localized resources while deriving clickable-span ranges from the localized action labels. Navigation behavior and styling are preserved across all supported locales.

Validation note: all Android resource XML files currently parse successfully (`bad_xml=0`). No Gradle/Android build has been run in this phase, so compilation and device rendering remain to be verified locally.

## Instructions for the next AI

1. Start with `git status`, preserve all pre-existing edits, and compare against `28375c1` when behavior is unclear.
2. Read this guide and inspect the target layout plus its Activity before editing.
3. Reuse `colors_fc.xml`, `values-night`, existing drawables, and Material Components.
4. Change one screen at a time, keeping all view IDs and click-listener targets required by the Activity.
5. Check light and dark contrast, content descriptions, text wrapping, scrolling, and 48dp touch targets.
6. Run the narrowest useful resource/compile validation, then a broader build when practical; record the exact result here.
7. Add every changed file and any incomplete or uncertain item to this document. Never claim runtime or build success without evidence.
