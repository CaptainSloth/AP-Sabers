package com.adventureparkmc.robawesome.apsabers.gui;

import com.adventureparkmc.robawesome.apsabers.APSabers;
import com.adventureparkmc.robawesome.apsabers.SaberType;
import com.adventureparkmc.robawesome.apsabers.items.SaberItemFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;

/**
 * Prevents players from moving items in or out of the saber showcase menu and
 * hands out sabers when a menu icon is clicked.
 */
public class SaberMenuListener implements Listener {

    private final APSabers plugin;
    private final NamespacedKey menuSaberKey;

    public SaberMenuListener(APSabers plugin) {
        this.plugin = plugin;
        this.menuSaberKey = new NamespacedKey(plugin, "saber_menu_type");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof SaberMenu.MenuHolder)) {
            return;
        }

        if (event.getClickedInventory() == null) {
            event.setCancelled(true);
            return;
        }

        if (event.getClickedInventory().equals(event.getView().getBottomInventory())) {
            if (event.isShiftClick()) {
                event.setCancelled(true);
            }
            return;
        }

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType().isAir()) {
            return;
        }

        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) {
            return;
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        String saberKey = container.get(menuSaberKey, PersistentDataType.STRING);
        if (saberKey == null) {
            return;
        }

        SaberType type = plugin.getConfigManager().getSaberType(saberKey);
        if (type == null) {
            event.getWhoClicked().sendMessage(Component.text("Unable to find that lightsaber configuration.", NamedTextColor.RED));
            return;
        }

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ItemStack saberItem = SaberItemFactory.createSaber(plugin, type);

        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(saberItem);
        if (!leftovers.isEmpty()) {
            player.sendMessage(Component.text("Your inventory is full. Make space before taking another lightsaber.", NamedTextColor.RED));
            return;
        }

        ItemMeta saberMeta = saberItem.getItemMeta();
        Component displayName = saberMeta != null && saberMeta.displayName() != null
                ? saberMeta.displayName()
                : Component.text(type.getInternalName(), NamedTextColor.AQUA);

        player.sendMessage(Component.text("Added ", NamedTextColor.GREEN)
                .append(displayName)
                .append(Component.text(" to your inventory.", NamedTextColor.GREEN)));
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof SaberMenu.MenuHolder)) {
            return;
        }

        int topSize = event.getView().getTopInventory().getSize();
        for (int slot : event.getRawSlots()) {
            if (slot < topSize) {
                event.setCancelled(true);
                return;
            }
        }
    }
}
