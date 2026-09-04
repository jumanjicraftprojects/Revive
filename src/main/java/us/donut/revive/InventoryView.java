package us.donut.revive;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.ClickType;
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
        var item = event.getCursor();
        var currentItem = event.getCurrentItem();

        if(event.getAction() == InventoryAction.COLLECT_TO_CURSOR){
            deny(event);
            return;
        }

        // A shift-click from the viewer can otherwise spill into the equipment or
        // decorative slots of this chest-shaped player inventory.
        if(event.getClickedInventory() != view && event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY){
            deny(event);
            return;
        }

        if(event.getClickedInventory() == view){
            if(event.getSlot() > offhandSlot || (event.getSlot() > 35 && event.getSlot() < helmetSlot)){
                deny(event);
                return;
            }

            ItemStack incoming = item;
            if(event.getHotbarButton() >= 0)
                incoming = event.getWhoClicked().getInventory().getItem(event.getHotbarButton());
            else if(event.getClick() == ClickType.SWAP_OFFHAND && event.getWhoClicked() instanceof Player viewer)
                incoming = viewer.getInventory().getItemInOffHand();
            else if(event.getClick().isKeyboardClick()) incoming = null;
            String typeName = incoming == null || incoming.getType().isAir() ? null : incoming.getType().name();

            switch (event.getSlot()) {
                case helmetSlot -> {
                    if (typeName != null && !typeName.endsWith("_HELMET")) {
                        deny(event);
                    }
                }
                case chestplateSlot -> {
                    if (typeName != null && !typeName.endsWith("_CHESTPLATE")) {
                        deny(event);
                    }
                }
                case leggingSlot -> {
                    if (typeName != null && !typeName.endsWith("_LEGGINGS")) {
                        deny(event);
                    }
                }
                case bootSlot -> {
                    if (typeName != null && !typeName.endsWith("_BOOTS")) {
                        deny(event);
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

    public void onDrag(InventoryDragEvent event){
        for(var entry : event.getNewItems().entrySet()){
            int slot = entry.getKey();
            if(slot >= view.getSize()) continue;
            if(slot > offhandSlot || (slot > 35 && slot < helmetSlot)
                    || !validEquipment(slot, entry.getValue())){
                event.setCancelled(true);
                return;
            }
        }
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), this::saveInvChanges, 1L);
    }

    private boolean validEquipment(int slot, ItemStack item){
        if(item == null || item.getType().isAir()) return true;
        String type = item.getType().name();
        return switch(slot){
            case helmetSlot -> type.endsWith("_HELMET");
            case chestplateSlot -> type.endsWith("_CHESTPLATE");
            case leggingSlot -> type.endsWith("_LEGGINGS");
            case bootSlot -> type.endsWith("_BOOTS");
            default -> true;
        };
    }

    private void deny(InventoryClickEvent event){
        event.setResult(Event.Result.DENY);
        event.setCancelled(true);
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
