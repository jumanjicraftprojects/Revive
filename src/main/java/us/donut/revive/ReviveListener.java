package us.donut.revive;

import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.GameMode;
import org.bukkit.inventory.EquipmentSlot;

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
        if (e.getHand() != EquipmentSlot.HAND) return;
        DownedState downedState = DownedStateManager.getState(e.getRightClicked().getUniqueId());
        if (downedState != null) {
            e.setCancelled(true);
            if (e.getPlayer().isSneaking()
                    && Main.getInstance().getConfig().getBoolean("allow-downed-inventory-access", false)) {
                InventoryManager.openInventory(e.getPlayer(), downedState.getPlayer());
            } else if (!downedState.isReviving()) {
                downedState.revive(e.getPlayer());
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPotionSplash(PotionSplashEvent event) {
        if (!Main.getInstance().isInstantPotion(event.getPotion().getItem())) return;
        Player thrower = event.getPotion().getShooter() instanceof Player player ? player : null;
        int revived = 0;
        // Custom potions intentionally carry no vanilla effect. Some server builds therefore
        // report an empty affected-entity list, so use the actual splash radius instead.
        for (Player player : event.getPotion().getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(event.getPotion().getLocation()) > 16.0) continue;
            DownedState state = DownedStateManager.getState(player);
            if (state == null) continue;
            state.instantRevive(thrower == null ? player : thrower);
            revived++;
        }
        if (thrower != null) {
            thrower.sendMessage(revived == 0
                    ? org.bukkit.ChatColor.RED + "The splash did not reach a downed player."
                    : org.bukkit.ChatColor.GREEN + "Revived " + revived + " downed player" + (revived == 1 ? "." : "s."));
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
    public void inventoryDragEvent(InventoryDragEvent event){
        if(DownedStateManager.getState(event.getWhoClicked().getUniqueId()) != null){
            event.setCancelled(true);
            return;
        }
        InventoryManager.passDragEvent(event);
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
