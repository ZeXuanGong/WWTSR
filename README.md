# WWTSR

English | [简体中文](README.zh-CN.md)

A desktop pet written in Java (JavaFX): a small creature sits in the corner of the screen and
taps with alternating hands on every key press and mouse click. Taps unlock chests; chests give
roles, accessories and emotes to collect, wear and combine.

All artwork is original placeholder art drawn in code (`GeneratedAssetPack`); replace it by
writing another `AssetPack`.

## Run

Needs JDK 25 or newer and Maven 3.9.

```sh
mvn test                                  # unit tests
mvn javafx:run                            # start the pet
mvn javafx:run -Djavafx.args=--demo       # start it tapping by itself (no input, nothing saved)
```

Developer tools that draw to PNG files without opening a window:

```sh
mvn javafx:run -Dmain.class=com.wwtsr/com.wwtsr.app.ContactSheet -Djavafx.args=sheet.png
mvn javafx:run -Dmain.class=com.wwtsr/com.wwtsr.app.PanelSheet "-Djavafx.args=outdir zh"
```

**Debug and release.** `mvn javafx:run` is the debug run: it sets `-Dwwtsr.debug=true`, which
switches on a watchdog that writes to `watchdog.log` in the data folder when the user
interface stops answering for 3 seconds (with what every thread was doing). The packaged
release doesn't watch the interface and keeps no log. Both restart the input hooks if Windows
has dropped them; only the debug run writes that down.

## Package

```powershell
$env:JAVA_HOME = '<a JDK 25 or newer>'
powershell -ExecutionPolicy Bypass -File package.ps1 -Maven <path to mvn.cmd>
```

This runs the tests, then builds `target\dist\WWTSR\` (`WWTSR.exe` with its own Java runtime;
nothing needs installing) and `target\dist\WWTSR-<version>-windows-x64.zip`. To give the app to
someone, send the zip; they unzip it anywhere and run `WWTSR.exe`.

## Using it

**The pet.** Drag it to move it; it remembers where you left it. Click it and its emotes fly
out in an arc up its left side; click one to pop it up, click the pet again to put them away.
Right-click the pet, or its icon in the system tray, for the menu.

**Chests.** There are two chest slots. Opening a chest costs 1,000 taps, taken off the count
on the pet's bar (Settings shows the lifetime count, which never goes down). With 1,000 taps
one chest shows; with 2,000 both do, and both can be opened one after the other. A chest gives
one random collectible (common, uncommon, rare or epic), revealed above the pet with more
fanfare the rarer it is, and its slot is ready again 30 minutes later. A slot that is still
waiting shows, with its countdown, only while the mouse is on the pet, unless you choose to
always show it; with fewer than 1,000 taps no chest shows at all.

**The panel** (from the menu, or the ≡ button on the pet's bar) has four pages:

- **Bag** – everything you have, by tab (recent, roles, accessories, emotes) and filter
  (series, rarity). Click a card to use it: a role becomes the pet, an accessory is put on or
  taken off, an emote goes into one of five emote slots. Star what you like. Five presets keep
  whole looks (role + accessory + emotes).
- **Atlas** – every collectible there is, with what you haven't found drawn faint.
- **Synthesis** – put 10 spare copies of one kind and rarity into the slots to make one of the
  next rarity, or 5 to make one of the same rarity; "pick for me" fills the slots. Only spares
  are used: you always keep one of everything.
- **Settings** – nickname, size, opacity, screen, language (中文 / English), lock position,
  turn faint under the mouse, always show chests, game mode, taskbar button, start with
  Windows (installed app only), and the three shortcut keys.

**Shortcut keys.** F9 shows or hides the pet, F8 moves it to the next screen and F7 switches
game mode; all can be changed or cleared in Settings. A shortcut is a function key alone, or
another key with Ctrl, Alt or Win held (Shift may be added). They work anywhere and are not
taken away from the program you are using.

**Dragging.** The pet stays on its screen while you drag it and settles above the taskbar when
you let go. Its place is remembered as a share of the screen, so it lands in the matching spot
after the screen's size changes or the pet moves to another screen.

**Game mode.** The pet stays on screen but the mouse passes straight through it, except while a
chest is ready. By default it switches on by itself while a full-screen program is in front.

**What it listens to.** On Windows it installs low-level keyboard and mouse hooks. It passes on
only "a key was pressed" or "a button was clicked": which key is used for an instant to ignore
the repeats of a held key and to recognise the shortcut keys, and is never stored, logged
or sent anywhere. Presses in windows that run as administrator aren't heard unless WWTSR is run
as administrator too.

**What it saves**, in `%APPDATA%\WWTSR\`: `taps.properties` (two counts), `settings.properties`
and `player.properties` (chest times, the bag, the look, presets). Only one copy runs at a time.

## Layout

| Package | What it holds |
|---|---|
| `com.wwtsr.pet` | The pet itself, with no UI: body states (state pattern), blinking, tap count, listeners |
| `com.wwtsr.input` | `InputSource` (where presses come from) and the Windows hook behind it |
| `com.wwtsr.stats` | The tap counts, and `TapStore` (where they are kept between runs) |
| `com.wwtsr.collect` | Collectibles (`Role`, `Accessory`, `Emote`), the `Catalog`, the bag (`Inventory`), a look (`Loadout`), `Synthesis` |
| `com.wwtsr.chest` | Chest slots and what a chest gives |
| `com.wwtsr.player` | `Player`: chests, bag, look and presets, and everything done with them; `PlayerStore` |
| `com.wwtsr.settings` | `Settings` (immutable), `AppSettings` (the one in use, with listeners), `SettingsStore` |
| `com.wwtsr.menu` | The menu described without a UI toolkit (`MenuEntry`), built by `AppMenu` |
| `com.wwtsr.panel` | The panel window and its pages |
| `com.wwtsr.view` | The pet's picture, counter, chests, emote fan, dragging and window |
| `com.wwtsr.tray` | The system-tray icon (AWT); a right-click opens the same JavaFX menu as the pet's |
| `com.wwtsr.i18n` | The app's text in Chinese and English |
| `com.wwtsr.platform` | Windows-only parts behind interfaces: window styles, full-screen detection, start at sign-in, single instance |
| `com.wwtsr.assets` | `AssetPack` (every picture) and the generated placeholder pack |
| `com.wwtsr.io` | Safe reading and writing of the small data files |
| `com.wwtsr.app` | Start-up, wiring and the developer tools |
