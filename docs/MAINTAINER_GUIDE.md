# Maintainer guide

`DownedStateManager` is the lifecycle owner. Each `DownedState` owns its carrier, prompt, boss bar,
bleed task, ground task, and revive task; `delete()` must remain idempotent. `InventoryManager` maps
viewers to the downed player's equipment view and must reject shift, drag, hotbar, and offhand bypasses.
`ReviveListener` is the event boundary. The splash potion is identified by PDC, then resolves nearby
downed players directly because it intentionally has no vanilla potion effect.

Keep all configuration finite and bounded. Never leave a task, display, passenger, blindness effect,
or viewer mapping behind on death, quit, reload, or shutdown. Build with `./gradlew clean test build`;
test ordinary revive cancellation, splash revive, full inventory viewing, disconnects, and restart.
