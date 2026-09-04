package us.donut.revive;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;


public class DownedState {

    private final Main plugin = Main.getInstance();
    private final Player player;
    private ArmorStand armorStand;
    private TextDisplay display;
    private BukkitTask groundTask;
    private BukkitTask downTask;
    private BukkitTask bleedoutTask;
    private BukkitTask reviveTask;
    private BossBar reviveBar;
    private final EntityDamageEvent.DamageCause downReason;

    public DownedState(Player player, EntityDamageEvent.DamageCause downReason) {
        this.player = player;
        this.downReason = downReason;

        double promptRange = plugin.bounded("down-range", 32, 0, 128);
        Location downedLocation = player.getLocation();
        player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1, false, false));
        double damage = plugin.bleedDamage(player);
        bleedoutTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> player.damage(damage), 0, 20);

        groundTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (isGrounded()) {
                    armorStand = player.getWorld().spawn(player.getLocation().getBlock().getLocation().subtract(0, 1, 0), ArmorStand.class, armorStand ->  {
                        armorStand.setVisible(false);
                        armorStand.setGravity(false);
                        armorStand.setInvulnerable(true);
                        armorStand.setSmall(true);
                        armorStand.setPersistent(false);
                        armorStand.addPassenger(player);
                    });

                    var displayLocation = player.getEyeLocation().add(0, 1.5, 0);
                    display = player.getWorld().spawn(displayLocation, TextDisplay.class, entity ->{
                        entity.setText(ChatColor.RED + "Revive");
                        entity.setBillboard(Display.Billboard.VERTICAL);
                        entity.setPersistent(false);
                    });

                    downTask = new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (promptRange >= 0) {
                                boolean nearPlayer = player.getWorld().getPlayers().stream()
                                        .anyMatch(other -> !other.equals(player) && !other.isDead()
                                                && other.getLocation().distanceSquared(player.getLocation()) <= promptRange * promptRange);
                                if (!nearPlayer) {
                                    player.teleport(downedLocation, TeleportCause.PLUGIN);
                                    killPlayer();
                                    cancel();
                                }
                            }
                            if (!armorStand.equals(player.getVehicle())) {
                            	player.teleport(downedLocation, TeleportCause.PLUGIN);
                                killPlayer();
                                cancel();
                            }
                        }
                    }.runTaskTimer(plugin, 0, 5);

                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    public void revive(Player reviver) {
        if (armorStand == null || display == null || !player.isOnline()) return;
        var reviveRange = plugin.bounded("revive-range", 3, 1, 12);

        if(!reviver.getWorld().equals(player.getWorld())
                || reviver.getLocation().distanceSquared(player.getLocation()) > reviveRange * reviveRange || !isLookingAt(reviver, player, reviveRange)){
            if(isReviving()){
                endRevive();
            }
            return;
        }

        bleedoutTask.cancel();
        bleedoutTask = null;
        display.setText(ChatColor.GREEN + "Reviving...");
        reviveBar = Bukkit.createBossBar(ChatColor.GREEN + "Reviving...", BarColor.GREEN, BarStyle.SOLID);
        reviveBar.addPlayer(player);
        reviveBar.addPlayer(reviver);
        double duration = plugin.bounded("revive-duration-seconds", 1, .1, 60) * 20;
        double finalReviveRange = reviveRange;
        reviveTask = new BukkitRunnable() {
            private double time = 0;
            @Override
            public void run() {
                if (time >= duration) {
                    finishRevive(plugin.bounded("revive-health", 3, .5, 100));
                } else {
                    reviveBar.setProgress(time / duration);
                    time++;

                    if(!reviver.isOnline() || !reviver.getWorld().equals(player.getWorld())
                            || reviver.getLocation().distanceSquared(player.getLocation()) > finalReviveRange * finalReviveRange || !isLookingAt(reviver, player, finalReviveRange)){
                        if(isReviving()){
                            endRevive();
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    public void instantRevive(Player reviver) {
        finishRevive(plugin.bounded("instant-revive-health", 6, .5, 100));
        player.sendMessage(ChatColor.GREEN+"You were revived instantly by "+reviver.getName()+".");
    }

    private void finishRevive(double healthHearts){
        double maximum=player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();player.setHealth(Math.min(maximum,Math.max(1,healthHearts*2)));
        if(armorStand!=null)armorStand.removePassenger(player);player.teleport(player.getLocation().add(0,.1,0));player.removePotionEffect(PotionEffectType.BLINDNESS);DownedStateManager.removeDownedState(player);
    }

    public void delete() {
        if (armorStand != null) armorStand.removePassenger(player);
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        if (armorStand != null) {
            armorStand.remove();
        }
        if (display != null) {
            display.remove();
        }
        if (groundTask != null) {
            groundTask.cancel();
        }
        if (downTask != null) {
            downTask.cancel();
        }
        if (bleedoutTask != null) {
            bleedoutTask.cancel();
        }
        if (reviveTask != null) {
            reviveTask.cancel();
        }
        if (reviveBar != null) {
            reviveBar.removeAll();
        }
    }

    public boolean isReviving() {
        return reviveTask != null && !reviveTask.isCancelled();
    }

    public Player getPlayer() {
        return player;
    }

    @SuppressWarnings({"deprecation", "removal"})
    private void endRevive(){
        if(reviveTask != null){
            reviveTask.cancel();
            reviveTask = null;
            reviveBar.removeAll();
        }

        double damage = plugin.bleedDamage(player);
        if(bleedoutTask == null || bleedoutTask.isCancelled()){
            bleedoutTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                var damageEvent = new EntityDamageEvent(player, downReason, damage);
                player.setLastDamageCause(damageEvent);
                player.damage(damage);
            }, 0, 20);
        }

        if (display != null) display.setText(ChatColor.RED + "Revive");
    }

    @SuppressWarnings({"deprecation", "removal"})
    private void killPlayer(){
        var damageEvent = new EntityDamageEvent(player, downReason, player.getHealth());
        player.setLastDamageCause(damageEvent);
        player.setHealth(0);
        DownedStateManager.removeDownedState(player);
    }

    private boolean isLookingAt(Player reviver, Player downed, double range) {
        var raycast =
                reviver.getWorld().rayTraceEntities(
                        reviver.getEyeLocation(),
                        reviver.getEyeLocation().getDirection(),
                        range,
                        entity -> entity.getUniqueId().equals(downed.getUniqueId())
                );
        return raycast != null && raycast.getHitEntity() != null;
    }

    /** Uses the server's block state instead of the deprecated client-reported ground flag. */
    private boolean isGrounded() {
        return player.getLocation().subtract(0, 0.05, 0).getBlock().getType().isSolid();
    }
}
