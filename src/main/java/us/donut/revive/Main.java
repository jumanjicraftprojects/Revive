package us.donut.revive;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private static Main instance;
    private ReviveListener reviveListener;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(reviveListener = new ReviveListener(), this);
    }
    
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        reloadConfig();
        sender.sendMessage(ChatColor.GREEN + "Successfully reloaded revive config.");
        return true;
    }

    public static Main getInstance() {
        return instance;
    }
}
