package us.donut.revive;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ReviveListener implements Listener {

    private Map<Player, DownedState> downedStates = new HashMap<>();
    private Set<UUID> cooldownPlayers = new HashSet<>();

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (!e.isCancelled() && e.getEntity() instanceof Player && !e.getEntity().hasMetadata("NPC")) {
            Player player = (Player) e.getEntity();
            if (!player.hasPermission("revive.disable")
                    && player.getHealth() - e.getFinalDamage() <= 0
                    && !downedStates.containsKey(player)
                    && !cooldownPlayers.contains(player.getUniqueId())) {
                e.setCancelled(true);
                downedStates.put(player, new DownedState(player));
            } else if (downedStates.containsKey(player) && e.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractAtEntityEvent e) {
        DownedState downedState = downedStates.get(e.getRightClicked());
        if (downedState != null) {
            if (e.getPlayer().isSneaking()) {
                e.getPlayer().openInventory(downedState.getPlayer().getInventory());
            } else if (!downedState.isReviving()) {
                downedState.revive(e.getPlayer());
            }
        }
    }

    @EventHandler
    public void onArmorStandInteract(PlayerArmorStandManipulateEvent e) {
        for (Entity entity : e.getRightClicked().getPassengers()) {
            if (downedStates.containsKey(entity)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onHealthRegain(EntityRegainHealthEvent e) {
        if (downedStates.containsKey(e.getEntity())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        cooldownPlayers.remove(e.getEntity().getUniqueId());
        DownedState downedState = downedStates.get(e.getEntity());
        if (downedState != null) {
            downedState.delete();
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        if (downedStates.containsKey(e.getPlayer())) {
            e.getPlayer().setHealth(0);
        }
    }

    public Map<Player, DownedState> getDownedStates() {
        return downedStates;
    }

    public Set<UUID> getCooldownPlayers() {
        return cooldownPlayers;
    }
}
