# Revive

Revive replaces immediate player death with a cooperative downed state. Nearby players can look at and right-click a downed teammate to revive them, or shift-right-click to inspect their inventory.

## Features

- Configurable bleed damage, revive duration/range/health, down range, and post-revive cooldown.
- Downed players ride an invisible support entity, receive blindness, cannot manipulate inventories or drop items, and stop attracting mobs.
- Manual revive progress appears to both players in a boss bar and now honors the configured real-time duration.
- Higher JumanjiAtlas title tiers bleed out more slowly. The default is 8% less damage per tier with a 40% minimum multiplier; Revive safely falls back to tier 1 when Atlas is absent or unloaded.
- Splash Revive Potions revive every downed friend caught in the splash. Operators obtain them with `/revivepotion [player] [amount]`, and players can buy them from JumanjiCraft Core's `/supplies` shop.

## Commands and permissions

| Command | Permission |
| --- | --- |
| `/reloadrevive` | `revive.reload` |
| `/revivepotion [player] [amount]` | `revive.potion.give` |

`revive.disable` opts a player out of the downed system.

## Build

Requires Java 21. Run `./gradlew clean build`; the jar is written to `build/libs/Revive-1.2.1.jar`. JumanjiAtlas is an optional soft dependency.

## Configuration

`config.yml` documents all timing and health values. Health entries use hearts, while the implementation converts them to Minecraft health points. Atlas integration uses reflection and never edits Atlas progression.
