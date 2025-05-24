package us.donut.revive;

import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.Console;
import java.util.logging.Level;

public class ReviveListener implements Listener {

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (!e.isCancelled() && e.getEntity() instanceof Player player && !e.getEntity().hasMetadata("NPC")) {
            var isDowned = DownedStateManager.getState(player) != null;
            if (!player.hasPermission("revive.disable")
                    && player.getHealth() - e.getFinalDamage() <= 0
                    && !isDowned
                    && !DownedStateManager.isOnCooldown(player)) {
                e.setCancelled(true);
                DownedStateManager.createDownedState(player, e.getCause());
            } else if (isDowned && e.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractAtEntityEvent e) {
        DownedState downedState = DownedStateManager.getState(e.getRightClicked().getUniqueId());
        if (downedState != null) {
            if (e.getPlayer().isSneaking()) {
                InventoryManager.openInventory(e.getPlayer(), downedState.getPlayer());
            } else if (!downedState.isReviving()) {
                downedState.revive(e.getPlayer());
            }
        }
    }

    @EventHandler
    public void onArmorStandInteract(PlayerArmorStandManipulateEvent e) {
        for (Entity entity : e.getRightClicked().getPassengers()) {
            if (DownedStateManager.getState(entity.getUniqueId()) != null) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onHealthRegain(EntityRegainHealthEvent e) {
        if (DownedStateManager.getState(e.getEntity().getUniqueId()) != null) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        DownedStateManager.removeDownedState(e.getEntity());
    }

    @EventHandler
    public void onEntityTarget(EntityTargetLivingEntityEvent e){
        if(!(e.getTarget() instanceof Player player)) return;
        if(!(e.getEntity() instanceof Mob mob)) return;

        var state = DownedStateManager.getState(player);
        if(state != null){
            e.setCancelled(true);
            mob.setTarget(null);

            if(mob.getTarget() != null && mob.getTarget() != player){
                var target = mob.getTarget();
                if(!(target instanceof Player tPlayer)) return;

                var tState = DownedStateManager.getState(tPlayer);
                if(tState == null) return;
            }

            DownedStateManager.setNewTarget(mob);
        }
    }

    @EventHandler
    public void inventoryClickEvent(InventoryClickEvent event){
        var id = event.getWhoClicked().getUniqueId();
        if(DownedStateManager.getState(id) != null){
            event.setResult(Event.Result.DENY);
            event.setCancelled(true);
            return;
        }

        InventoryManager.passClickEvent(event);
    }

    @EventHandler
    public void inventoryCloseEvent(InventoryCloseEvent event){
        InventoryManager.removeViewer(event.getPlayer());
    }

    @EventHandler
    public void dropItemEvent(PlayerDropItemEvent event){
        if(DownedStateManager.getState(event.getPlayer()) != null){
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        if (DownedStateManager.getState(e.getPlayer()) != null) {
            e.getPlayer().setHealth(0);
            DownedStateManager.removeDownedState(e.getPlayer());
        }
    }
}
