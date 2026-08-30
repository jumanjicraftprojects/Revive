# Changelog

## 1.2.0

- Converted instant revive items into throwable splash potions that can revive multiple downed friends.
- Fixed a revive-cancellation task race that produced repeated null-pointer errors.
- Exposed the potion factory for JumanjiCraft Core's shared `/supplies` shop.

## 1.1.0

- Added consumable Instant Revive Potions and an operator give command.
- Added optional Atlas title-tier bleedout scaling.
- Fixed manual revive duration running five times slower than configured.
- Corrected Bukkit command metadata and modernized the Gradle wrapper/toolchain.
