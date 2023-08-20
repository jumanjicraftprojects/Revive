package us.donut.revive;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;


public class InventoryView {
    private Player owner;
    private Inventory view;
    private static final Material fill = Material.GREEN_STAINED_GLASS_PANE;
    private static final int helmetSlot = 38;
    private static final int chestplateSlot = helmetSlot + 1;
    private static final int leggingSlot = chestplateSlot + 1;
    private static final int bootSlot = leggingSlot + 1;
    private static final int offhandSlot = bootSlot + 1;

    public InventoryView(Player owner){
        this.owner = owner;
        view = Bukkit.createInventory(null, 54);

        setItems();
    }

    public void add(Player viewer){
        viewer.openInventory(view);
    }

    public void removeAllViewers(){
        for(var viewer : view.getViewers()){
            viewer.closeInventory();
        }
    }

    public void onInteract(InventoryClickEvent event){
        String typeName = null;
        var item = event.getCursor();
        var currentItem = event.getCurrentItem();
        if(item != null && !item.getType().isAir()){
            typeName = item.getType().name();
        }

        if(event.getClickedInventory() == view){
            if(event.getSlot() > offhandSlot || (event.getSlot() > 35 && event.getSlot() < helmetSlot)){
                // These slots are not part of the players
                event.setResult(Event.Result.DENY);
                event.setCancelled(true);
                return;
            }

            switch (event.getSlot()) {
                case helmetSlot -> {
                    if (typeName != null && !typeName.endsWith("_HELMET")) {
                        event.setResult(Event.Result.DENY);
                        event.setCancelled(true);
                    } else {
                        //owner.getInventory().setHelmet(item);
                    }
                }
                case chestplateSlot -> {
                    if (typeName != null && !typeName.endsWith("_CHESTPLATE")) {
                        event.setResult(Event.Result.DENY);
                        event.setCancelled(true);
                    } else {
                        //owner.getInventory().setChestplate(item);
                    }
                }
                case leggingSlot -> {
                    if (typeName != null && !typeName.endsWith("_LEGGINGS")) {
                        event.setResult(Event.Result.DENY);
                        event.setCancelled(true);
                    } else {
                        //owner.getInventory().setLeggings(item);
                    }
                }
                case bootSlot -> {
                    if (typeName != null && !typeName.endsWith("_BOOTS")) {
                        event.setResult(Event.Result.DENY);
                        event.setCancelled(true);
                    } else {
                        //owner.getInventory().setBoots(item);
                    }
                }
                case offhandSlot -> {}
                //owner.getInventory().setItemInOffHand(item);
                default -> { // Anything else
                /*var contentsCopy = new ItemStack[36];
                System.arraycopy(view.getContents(), 0, contentsCopy, 0, 36);
                owner.getInventory().setStorageContents(contentsCopy);*/
                }
            }
        }

        if(event.isCancelled()){
            Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                event.getWhoClicked().setItemOnCursor(item);
                view.setItem(event.getSlot(), currentItem);
            }, 1L);
        }
        else{
            Bukkit.getScheduler().runTaskLater(Main.getInstance(), this::saveInvChanges, 1L);
        }
    }

    private void saveInvChanges(){
        var contentsCopy = new ItemStack[36];
        System.arraycopy(view.getContents(), 0, contentsCopy, 0, 36);
        owner.getInventory().setStorageContents(contentsCopy);

        owner.getInventory().setHelmet(view.getItem(helmetSlot));
        owner.getInventory().setChestplate(view.getItem(chestplateSlot));
        owner.getInventory().setLeggings(view.getItem(leggingSlot));
        owner.getInventory().setBoots(view.getItem(bootSlot));
        owner.getInventory().setItemInOffHand(view.getItem(offhandSlot));
    }

    private void setItems(){
        var inv = owner.getInventory();
        var contents = inv.getStorageContents();
        for(var i = 0; i < contents.length; i++){
            var item = contents[i];
            if(item == null) continue;

            view.setItem(i, item);
        }

        var helmet = inv.getHelmet();
        var chestplate = inv.getChestplate();
        var pants = inv.getLeggings();
        var boots = inv.getBoots();
        var offhand = inv.getItemInOffHand();

        view.setItem(helmetSlot, helmet);
        view.setItem(chestplateSlot, chestplate);
        view.setItem(leggingSlot, pants);
        view.setItem(bootSlot, boots);
        view.setItem(offhandSlot, offhand);

        var itemStack = new ItemStack(fill);
        var meta = itemStack.getItemMeta();
        meta.setDisplayName(" ");
        itemStack.setItemMeta(meta);

        for(var i = 36; i < helmetSlot; i++){
            view.setItem(i, itemStack);
        }

        for(var i = offhandSlot + 1; i < view.getSize(); i++){
            view.setItem(i, itemStack);
        }
    }
}
