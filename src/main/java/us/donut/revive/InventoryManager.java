package us.donut.revive;

import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InventoryManager {
    private static final Map<UUID, InventoryView> viewByOwner = new HashMap<>();
    private static final Map<UUID, InventoryView> viewByViewer = new HashMap<>();

    public static void openInventory(Player opener, Player downed){
        if(!viewByOwner.containsKey(downed.getUniqueId())){
            var view = new InventoryView(downed);
            viewByOwner.put(downed.getUniqueId(), view);
        }

        var view = viewByOwner.get(downed.getUniqueId());
        view.add(opener);
        viewByViewer.put(opener.getUniqueId(), view);
    }

    public static void removeViewer(HumanEntity viewer){
        if(!viewByViewer.containsKey(viewer.getUniqueId())) return;

        viewByViewer.remove(viewer.getUniqueId());
    }

    public static void closeInventory(HumanEntity player){
        if(!viewByOwner.containsKey(player.getUniqueId())) return;

        var view = viewByOwner.remove(player.getUniqueId());
        view.removeAllViewers();
    }

    public static void passClickEvent(InventoryClickEvent e){
        var id = e.getWhoClicked().getUniqueId();

        if(!viewByViewer.containsKey(id)) return;
        var view = viewByViewer.get(id);
        view.onInteract(e);
    }
}
