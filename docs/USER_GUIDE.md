# Identra user guide

Identra changes what apps can learn about your phone: its model, IDs, SIM, network, region and
location. It works through Vector (LSPosed) and needs root.

## Before you start

1. Install Identra and enable it in the Vector manager. Scope: **System Framework**,
   **Settings Storage** (com.android.providers.settings) and **Identra**. A soft reboot is needed
   once so the system part loads; later Identra updates don't need one.
2. Open Identra. The **Home** tab says "Protection is active" when everything is running. If it
   shows a warning, follow the button on the card.

## Protect an app

1. Open the **Apps** tab and find the app (search, or the Protected / User / System chips).
2. Flip its switch. That turns on Identra's recommended set of protections for it.
3. For more control, tap the app. Its page has one switch per area (Device, Network, Location,
   Phone & SIM, Apps & accounts, Sensors & clipboard, Trackers, Stealth) and an **Advanced**
   section with every hook group, the app's own values, logs and "force stop on change".
4. Restart the app (Identra usually stops it for you) so the change takes effect.

A switch that is greyed out can't be changed for that app (for example force-stop for system apps
that always run).

## Set a fake location

1. Open the **Location** tab.
2. Search a place (or type coordinates like `59.33, 18.07`), or tap / long-press the map.
3. Press **Set for all apps** or **Set for an app…**.
4. The app must have location protection on. If it doesn't, Identra offers to turn it on.

Useful buttons on the map:
- **My location** (target icon) shows where your phone really is. Identra asks for location
  permission the first time. If Location is off or there is no fix, it says so — it never shows a
  made-up place.
- **Show selected place** (pin icon) jumps back to the place you picked, or to the location apps
  currently get.

Star a place to keep it in **Favourites**. **Advanced** has "exact" vs "coarse" (blur your real
location by a radius) and an optional small random offset.

## Profiles: a whole fake phone per app

A profile is a complete device: phone model and Android build, Android ID and other IDs, IMEI,
SIM operator and phone number, Wi-Fi and Bluetooth addresses, language, time zone, location,
screen and memory. Values in one profile fit together (a Swedish SIM has a Swedish number,
language and time zone; the IMEI belongs to that phone model).

### Create one
**Profiles** tab → **New profile**:
- **Random device** – a real phone model with everything matching.
- **Random device for a country…** – same, for the country you choose.
- **From this phone** – your phone's own model and build (Android doesn't let apps read the IMEI
  or SIM numbers, so those stay empty).
- **Duplicate** or **Empty**.

### Edit it
Tap a profile. Values are grouped (Device, Identifiers, Phone & SIM, Network, Location & region,
Display & hardware, Apps & accounts). Tap a value to change it or press **Randomize** for a new
one. The dice next to a section re-rolls that section; **Randomize all** re-rolls everything.
Tap the **lock** next to a value to keep it when re-rolling. **Change country…** gives the profile
a new SIM, number, language, time zone and location for another country; **Use a saved place**
takes a place from the Location tab. An empty value is not spoofed (the app sees the real one).

### Give it to apps
- From the profile: **Apply to apps…**, tick the apps, continue.
- From an app's page: tap the **profile chip** under the app name.

You then choose what happens to the app's own data:
- **Keep the app's data** – only the values change. The app may still remember you (logins,
  saved IDs in its files).
- **Fresh app data** – the app is reset like a new install. You lose its logins and files.
- **Keep separate data per profile** (needs root) – Identra saves the app's current data and
  gives it this profile's own copy (or a fresh start). Switching back to the old profile brings the
  old data back. Nothing is deleted until the save has worked.

Leave **Turn on matching protections** on so the app actually reads the new values.

The app list then shows "Protected · Profile: Work".

### Change a profile that apps use
Edit it as usual. Identra asks whether to update the apps that use it now, or later (a banner in
the editor keeps reminding you).

### Global default
Make a profile the **global default** to use it for every app that has the matching protections
on but no profile of its own.

### Backup
Use **export** (one profile, or all from the Profiles tab) to save profiles as a JSON file, and
**import** to load them on another phone. Saved app data copies stay on the phone
(`/data/adb/identra/appdata`).

## Tips

- After changing anything, fully close and reopen the app you are testing.
- If an app breaks, open its page and turn categories off one at a time, or remove its profile
  (profile chip → Remove profile).
- **Settings → Advanced tools** has the classic tools: collections, hooks, all values, CPU maps,
  database, backup & restore, logs.

## Instances: the same app several times

**Settings → Tools → Instances** (or the Instances button on Home).

1. Create an instance, pick the apps that should run in it, and name it.
2. Each instance is a hidden copy with its own data and its own identity (a random one, or a profile).
3. Turn on **Play Services** for an instance when its apps need Google sign‑in or Play licensing.
   Sign in to Google inside the instance once after turning it on.
4. Open a copy from the Instances screen, or use **Add to home screen** for a shortcut.
5. **Real device identity** reverts a copy to your phone's own values.

Instances are only visible inside Identra. Removing an instance deletes its copies and their data.

## Updates

Identra checks for new releases by itself. Use **Check for updates** in the side menu any time,
or turn **Auto‑check updates** off there.
