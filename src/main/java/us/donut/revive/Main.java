package us.donut.revive;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.Color;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private static Main instance;
    private ReviveListener reviveListener;
    private NamespacedKey instantPotionKey;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        instantPotionKey = new NamespacedKey(this,"instant_revive_potion");
        getServer().getPluginManager().registerEvents(reviveListener = new ReviveListener(), this);
    }
    
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(command.getName().equalsIgnoreCase("reloadrevive")){reloadConfig();sender.sendMessage(ChatColor.GREEN + "Successfully reloaded revive config.");return true;}
        Player target=sender instanceof Player player?player:null;if(args.length>0)target=Bukkit.getPlayerExact(args[0]);if(target==null){sender.sendMessage(ChatColor.RED+"Player not found.");return true;}int amount=1;if(args.length>1)try{amount=Math.max(1,Math.min(64,Integer.parseInt(args[1])));}catch(NumberFormatException ignored){}ItemStack potion=instantPotion();potion.setAmount(amount);for(ItemStack overflow:target.getInventory().addItem(potion).values())target.getWorld().dropItemNaturally(target.getLocation(),overflow);sender.sendMessage(ChatColor.GREEN+"Gave "+amount+" splash revive potion(s) to "+target.getName()+".");
        return true;
    }

    public static Main getInstance() {
        return instance;
    }
    public ItemStack instantPotion(){ItemStack item=new ItemStack(Material.SPLASH_POTION);PotionMeta meta=(PotionMeta)item.getItemMeta();meta.setDisplayName(ChatColor.LIGHT_PURPLE+"Splash Revive Potion");meta.setLore(java.util.List.of(ChatColor.GRAY+"Throw near downed friends",ChatColor.GRAY+"to revive everyone caught in the splash."));meta.setColor(Color.fromRGB(213,85,255));meta.getPersistentDataContainer().set(instantPotionKey,PersistentDataType.BYTE,(byte)1);item.setItemMeta(meta);return item;}
    public boolean isInstantPotion(ItemStack item){return item!=null&&item.hasItemMeta()&&item.getItemMeta().getPersistentDataContainer().has(instantPotionKey,PersistentDataType.BYTE);}
    public double bleedDamage(Player player){int tier=1;var atlas=Bukkit.getPluginManager().getPlugin("JumanjiAtlas");if(atlas!=null&&atlas.isEnabled())try{tier=(int)atlas.getClass().getMethod("atlasTitleTier",java.util.UUID.class).invoke(atlas,player.getUniqueId());}catch(ReflectiveOperationException ignored){}double reduction=getConfig().getDouble("atlas-bleedout-reduction-per-tier",.08),minimum=getConfig().getDouble("atlas-bleedout-minimum-multiplier",.4);return getConfig().getDouble("bleedout-damage-per-second",.5)*2*Math.max(minimum,1.0-(Math.max(1,tier)-1)*reduction);}
}
