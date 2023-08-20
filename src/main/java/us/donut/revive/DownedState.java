package us.donut.revive;

import java.util.List;

import me.filoghost.holographicdisplays.api.HolographicDisplaysAPI;
import me.filoghost.holographicdisplays.api.hologram.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;


public class DownedState {

    private Main plugin = Main.getInstance();
    private HolographicDisplaysAPI holoApi = HolographicDisplaysAPI.get(plugin);
    private Player player;
    private ArmorStand armorStand;
    private Hologram hologram;
    private BukkitTask groundTask;
    private BukkitTask downTask;
    private BukkitTask bleedoutTask;
    private BukkitTask reviveTask;
    private BossBar reviveBar;
    private EntityDamageEvent.DamageCause downReason;

    public DownedState(Player player, EntityDamageEvent.DamageCause downReason) {
        this.player = player;
        this.downReason = downReason;

    	double promptRange = plugin.getConfig().getDouble("down-range");
        Location downedLocation = player.getLocation();
        player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1, false, false));
        double damage = plugin.getConfig().getDouble("bleedout-damage-per-second") * 2;
        bleedoutTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> player.damage(damage), 0, 20);

        groundTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnGround()) {
                    armorStand = player.getWorld().spawn(player.getLocation().getBlock().getLocation().subtract(0, 1, 0), ArmorStand.class, armorStand ->  {
                        armorStand.setVisible(false);
                        armorStand.setGravity(false);
                        armorStand.setInvulnerable(true);
                        armorStand.setSmall(true);
                        armorStand.addPassenger(player);
                    });

                    hologram = holoApi.createHologram(player.getEyeLocation().add(0, 1.5, 0));
                    hologram.getLines().appendText(ChatColor.RED + "Revive");

                    downTask = new BukkitRunnable() {
                        @Override
                        public void run() {
                            hologram.setPosition(player.getEyeLocation().add(0, 1.5, 0));
                        	if(promptRange >= 0) {
                        		List<Entity> near = player.getNearbyEntities(promptRange, promptRange, 256.0D);
                        		boolean nearPlayer = false;
                        		
                        		for(Entity entity : near) {
                        			if(entity instanceof org.bukkit.entity.Player && !entity.isDead()) {
                        	        	nearPlayer = true;
                        	        	break;
                        			}
                        		}
                        		if(!nearPlayer) {
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
                    }.runTaskTimer(plugin, 0, 1);

                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    public void revive(Player reviver) {
        var reviveRange = plugin.getConfig().getDouble("revive-range");
        reviveRange = reviveRange <= 0 ? 1 : reviveRange;

        if(reviver.getLocation().distance(player.getLocation()) > reviveRange){
            if(isReviving()){
                endRevive();
            }
            return;
        }

        bleedoutTask.cancel();
        bleedoutTask = null;
        hologram.getLines().clear();
        hologram.getLines().appendText(ChatColor.GREEN + "Reviving...");
        hologram.getLines().appendText(String.valueOf(ChatColor.BOLD) + ChatColor.RED + "Don't move");
        reviveBar = Bukkit.createBossBar(ChatColor.GREEN + "Reviving...", BarColor.GREEN, BarStyle.SOLID);
        reviveBar.addPlayer(player);
        reviveBar.addPlayer(reviver);
        double duration = plugin.getConfig().getDouble("revive-duration-seconds", 1) * 20;
        double finalReviveRange = reviveRange;
        reviveTask = new BukkitRunnable() {
            private double time = 0;
            @Override
            public void run() {
                if (time >= duration) {
                    player.setHealth(plugin.getConfig().getDouble("revive-health") * 2);
                    armorStand.removePassenger(player);
                    player.teleport(player.getLocation().add(0, 0.1, 0));
                    player.removePotionEffect(PotionEffectType.BLINDNESS);

                    DownedStateManager.removeDownedState(player);
                } else {
                    reviveBar.setProgress(time / duration);
                    time++;

                    if(reviver.getLocation().distance(player.getLocation()) > finalReviveRange){
                        if(isReviving()){
                            endRevive();
                            reviveTask.cancel();
                            reviveTask = null;
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    public void delete() {
        if (armorStand != null) {
            armorStand.remove();
        }
        if (hologram != null) {
            hologram.delete();
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

    private void endRevive(){
        if(reviveTask != null){
            reviveTask.cancel();
            reviveTask = null;
            reviveBar.removeAll();
        }

        double damage = plugin.getConfig().getDouble("bleedout-damage-per-second") * 2;
        if(bleedoutTask == null || bleedoutTask.isCancelled()){
            bleedoutTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                var damageEvent = new EntityDamageEvent(player, downReason, damage);
                player.setLastDamageCause(damageEvent);
                player.damage(damage);
            }, 0, 20);
        }
        hologram.getLines().clear();
        hologram.getLines().appendText(ChatColor.RED + "Revive");
    }

    private void killPlayer(){
        var damageEvent = new EntityDamageEvent(player, downReason, player.getHealth());
        player.setLastDamageCause(damageEvent);
        player.setHealth(0);
        DownedStateManager.removeDownedState(player);
    }
}