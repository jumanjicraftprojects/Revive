package us.donut.revive;

import java.util.List;
import java.util.UUID;

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
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;

public class DownedState {

    private Main plugin = Main.getInstance();
    private Player player;
    private ArmorStand armorStand;
    private Hologram hologram;
    private BukkitTask groundTask;
    private BukkitTask downTask;
    private BukkitTask bleedoutTask;
    private BukkitTask reviveTask;
    private BossBar reviveBar;

    public DownedState(Player player) {
    	double promptRange = plugin.getConfig().getDouble("revive-prompt-max-range");
        this.player = player;
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

                    hologram = HologramsAPI.createHologram(plugin, player.getEyeLocation().add(0, 1.5, 0));
                    hologram.appendTextLine(ChatColor.RED + "Revive");

                    downTask = new BukkitRunnable() {
                        @Override
                        public void run() {
                            hologram.teleport(player.getEyeLocation().add(0, 1.5, 0));
                        	if(promptRange >= 0) {
                        		List<Entity> near = player.getNearbyEntities(promptRange, promptRange, 256.0D);
                        		boolean nearPlayer = false;
                        		
                        		for(Entity entity : near) {
                        			//player.chat("Found entity of type " + entity.getType().getName());
                        			if(entity instanceof org.bukkit.entity.Player && !entity.isDead()) {
                        	        	nearPlayer = true;
                        	        	break;
                        			}
                        		}
                        		if(!nearPlayer) {
                                    player.teleport(downedLocation, TeleportCause.PLUGIN);
                        			player.setHealth(0);
                        			cancel();
                        		}
                        	}
                            if (!armorStand.equals(player.getVehicle())) {
                            	player.teleport(downedLocation, TeleportCause.PLUGIN);
                                player.setHealth(0);
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
        bleedoutTask.cancel();
        hologram.clearLines();
        hologram.appendTextLine(ChatColor.GREEN + "Reviving...");
        reviveBar = Bukkit.createBossBar(ChatColor.GREEN + "Reviving...", BarColor.GREEN, BarStyle.SOLID);
        reviveBar.addPlayer(player);
        reviveBar.addPlayer(reviver);
        double duration = plugin.getConfig().getDouble("revive-duration-seconds", 1) * 20;
        reviveTask = new BukkitRunnable() {
            private double time = 0;
            @Override
            public void run() {
                if (time >= duration) {
                    player.setHealth(plugin.getConfig().getDouble("revive-health") * 2);
                    armorStand.removePassenger(player);
                    player.teleport(player.getLocation().add(0, 0.1, 0));
                    player.removePotionEffect(PotionEffectType.BLINDNESS);
                    UUID uuid = player.getUniqueId();
                    plugin.getReviveListener().getCooldownPlayers().add(uuid);
                    Bukkit.getScheduler().runTaskLater(plugin,
                            () -> plugin.getReviveListener().getCooldownPlayers().remove(uuid),
                            (long) (plugin.getConfig().getDouble("down-cooldown-seconds") * 20));
                    delete();
                } else {
                    reviveBar.setProgress(time / duration);
                    time++;
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    public void delete() {
        plugin.getReviveListener().getDownedStates().remove(player);
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
}