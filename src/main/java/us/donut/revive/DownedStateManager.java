package us.donut.revive;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.*;

public class DownedStateManager {
    private static final Map<UUID, DownedState> downedStates = new HashMap<>();
    private static final Set<UUID> cooldowns = new HashSet<>();

    public static DownedState getState(HumanEntity player){
        return getState(player.getUniqueId());
    }

    public static DownedState getState(UUID id){
        if(!downedStates.containsKey(id)) return null;
        return downedStates.get(id);
    }

    public static void createDownedState(Player player, EntityDamageEvent.DamageCause cause){
        downedStates.put(player.getUniqueId(), new DownedState(player, cause));

        player.closeInventory();

        var entities = player.getNearbyEntities(100,100,100);
        var mobsTargetingPlayer =
                entities.stream()
                        .filter(x -> x instanceof Mob)
                        .map(x -> (Mob) x)
                        .filter(x -> x.getTarget() == player)
                        .toList();

        if(mobsTargetingPlayer.size() <= 0) return;


        for(var mob : mobsTargetingPlayer){
            setNewTarget(mob);
        }
    }

    public static void removeDownedState(Player player){
        if(downedStates.containsKey(player.getUniqueId())){
            InventoryManager.closeInventory(player);

            var state = downedStates.remove(player.getUniqueId());
            state.delete();

            cooldowns.add(player.getUniqueId());
            Bukkit.getScheduler().runTaskLater(Main.getInstance(),
                    () -> cooldowns.remove(player.getUniqueId()),
                    (long) (Main.getInstance().getConfig().getDouble("down-cooldown-seconds") * 20));
        }
    }

    public static boolean isOnCooldown(HumanEntity player){
        return cooldowns.contains(player.getUniqueId());
    }

    public static void setNewTarget(Mob mob){
        var range = mob.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).getValue();
        var close = mob.getNearbyEntities(range, range, range);
        if(close.size() <= 0) return;

        var availablePlayers = close.stream()
                .filter(x -> x instanceof Player)
                .map(x -> (Player) x)
                .filter(x -> DownedStateManager.getState(x) == null)
                .filter(mob::hasLineOfSight)
                .toList();

        if(availablePlayers.size() <= 0) return;

        mob.setTarget(availablePlayers.get(0));
    }
}
