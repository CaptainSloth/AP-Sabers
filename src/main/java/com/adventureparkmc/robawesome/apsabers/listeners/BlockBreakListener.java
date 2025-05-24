package com.adventureparkmc.robawesome.apsabers.listeners;

import com.adventureparkmc.robawesome.apsabers.APSabers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class BlockBreakListener implements Listener {

    private final APSabers plugin;
    private final NamespacedKey saberTypeKey;

    public BlockBreakListener(APSabers plugin) {
        this.plugin = plugin;
        this.saberTypeKey = new NamespacedKey(plugin, "saber_type");
    }

    @EventHandler(priority = EventPriority.HIGHEST) // High priority to ensure it overrides default behavior
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (itemInHand.getType().isAir() || !itemInHand.hasItemMeta()) {
            return;
        }

        ItemMeta meta = itemInHand.getItemMeta();
        // meta can be null if itemInHand has no meta, even after hasItemMeta() check if plugin modifies it async (unlikely here)
        if (meta == null || !meta.getPersistentDataContainer().has(saberTypeKey, PersistentDataType.STRING)) {
            // Not a lightsaber
            return;
        }

        // It's a lightsaber (active or inactive)
        event.setCancelled(true);
        // Optionally, send a message to the player
        player.sendActionBar(Component.text("Lightsabers cannot break blocks!").color(NamedTextColor.RED));
        // You might also play a "thud" or "ineffective" sound here
        // player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, SoundCategory.PLAYERS, 0.5f, 1.5f);
    }
}