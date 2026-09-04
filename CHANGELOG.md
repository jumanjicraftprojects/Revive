# Changelog

## 1.2.2

- Fully removes downed-state tasks, effects, displays, carrier stands, boss bars, and inventory views on shutdown.
- Prevents duplicate downed states and cross-world revive distance errors.
- Marks temporary revive entities as non-persistent so they cannot survive a crash or restart.

## 1.2.1

- Reduced downed-player proximity checks from every tick to four times per second.
- Replaced broad entity scans with direct player distance checks.
- Simplified revive potion text and cleaned up command handling.

## 1.2.0

- Converted instant revive items into throwable splash potions that can revive multiple downed friends.
- Fixed a revive-cancellation task race that produced repeated null-pointer errors.
- Exposed the potion factory for JumanjiCraft Core's shared `/supplies` shop.

## 1.1.0

- Added consumable Instant Revive Potions and an operator give command.
- Added optional Atlas title-tier bleedout scaling.
- Fixed manual revive duration running five times slower than configured.
- Corrected Bukkit command metadata and modernized the Gradle wrapper/toolchain.
