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

## Validation record

- Repository and Git state: inspected on 2026-10-29.
- Branch and requested baseline commit: verified.
- Source/resource inspection: completed for the theme, splash, home, login, and create-account areas.
- XML parsing: passed for all four redesigned layouts.
- Required Activity-facing IDs: confirmed present for splash, login, create-account, and language screens.
- Palette refinement: light/dark resource values updated to the non-blue glassmorphism combinations described above.
- Gradle build/tests: not run yet in this handoff. Do not describe the UI as build-verified until a real Gradle build completes.
- Runtime light/dark visual verification: not run yet. Check both system modes on an emulator/device, including small-height screens and keyboard-open forms.

## Instructions for the next AI

1. Start with `git status`, preserve all pre-existing edits, and compare against `28375c1` when behavior is unclear.
2. Read this guide and inspect the target layout plus its Activity before editing.
3. Reuse `colors_fc.xml`, `values-night`, existing drawables, and Material Components.
4. Change one screen at a time, keeping all view IDs and click-listener targets required by the Activity.
5. Check light and dark contrast, content descriptions, text wrapping, scrolling, and 48dp touch targets.
6. Run the narrowest useful resource/compile validation, then a broader build when practical; record the exact result here.
7. Add every changed file and any incomplete or uncertain item to this document. Never claim runtime or build success without evidence.
