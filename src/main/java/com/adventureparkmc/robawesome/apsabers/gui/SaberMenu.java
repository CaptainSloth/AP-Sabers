package com.adventureparkmc.robawesome.apsabers.gui;

import com.adventureparkmc.robawesome.apsabers.APSabers;
import com.adventureparkmc.robawesome.apsabers.SaberType;
import com.adventureparkmc.robawesome.apsabers.items.SaberItemFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Builds and opens the menu used to showcase available lightsabers.
 */
public final class SaberMenu {

    private static final Component MENU_TITLE = Component.text("AP-Sabers Armory", NamedTextColor.AQUA);

    private SaberMenu() {
        // Utility class
    }

    public static void open(APSabers plugin, Player player) {
        Set<String> saberKeys = plugin.getConfigManager().getAllSaberTypeNames();
        if (saberKeys.isEmpty()) {
            player.sendMessage(Component.text("No lightsabers are currently configured.", NamedTextColor.YELLOW));
            return;
        }

        List<SaberType> saberTypes = new ArrayList<>();
        for (String key : saberKeys) {
            SaberType type = plugin.getConfigManager().getSaberType(key);
            if (type != null) {
                saberTypes.add(type);
            }
        }

        if (saberTypes.isEmpty()) {
            player.sendMessage(Component.text("Unable to load any configured lightsabers.", NamedTextColor.RED));
            return;
        }

        saberTypes.sort(Comparator.comparing(SaberType::getInternalName));

        int size = ((saberTypes.size() - 1) / 9 + 1) * 9;
        size = Math.max(size, 9);

        MenuHolder holder = new MenuHolder();
        Inventory inventory = Bukkit.createInventory(holder, size, MENU_TITLE);
        holder.setInventory(inventory);

        for (SaberType type : saberTypes) {
            inventory.addItem(SaberItemFactory.createSaberIcon(type));
        }

        player.openInventory(inventory);
    }

    /**
     * Simple holder used to identify the menu during inventory events.
     */
    public static final class MenuHolder implements InventoryHolder {

        private Inventory inventory;

        private void setInventory(Inventory inventory) {
            this.inventory = Objects.requireNonNull(inventory, "inventory");
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
