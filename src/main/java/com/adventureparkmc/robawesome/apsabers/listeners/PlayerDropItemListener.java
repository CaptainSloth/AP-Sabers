package com.adventureparkmc.robawesome.apsabers.listeners;

import com.adventureparkmc.robawesome.apsabers.APSabers;
import com.adventureparkmc.robawesome.apsabers.SaberType;
import org.bukkit.NamespacedKey;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class PlayerDropItemListener implements Listener {

    private final APSabers plugin;
    private final NamespacedKey saberTypeKey;
    private final NamespacedKey saberStateKey;

    public PlayerDropItemListener(APSabers plugin) {
        this.plugin = plugin;
        this.saberTypeKey = new NamespacedKey(plugin, "saber_type");
        this.saberStateKey = new NamespacedKey(plugin, "saber_state");
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack itemStackFromEvent = event.getItemDrop().getItemStack(); // Get the ItemStack from the Item entity in the event

        String configuredActivationKey = plugin.getConfigManager().getActivationKey();
        if (!"DROP_ITEM".equalsIgnoreCase(configuredActivationKey)) {
            return;
        }

        if (itemStackFromEvent.hasItemMeta()) {
            ItemMeta itemMetaFromEvent = itemStackFromEvent.getItemMeta();
            if (itemMetaFromEvent == null) { // Should be rare if hasItemMeta is true
                plugin.getLogger().warning("[SaberDropListener] itemMetaFromEvent is null for item: " + itemStackFromEvent.getType() + " by " + player.getName());
                return;
            }

            PersistentDataContainer pdc = itemMetaFromEvent.getPersistentDataContainer();

            if (pdc.has(saberTypeKey, PersistentDataType.STRING)) {
                // It's a lightsaber!
                String internalSaberName = pdc.get(saberTypeKey, PersistentDataType.STRING);
                SaberType saberType = plugin.getConfigManager().getSaberType(internalSaberName);

                if (saberType == null) {
                    plugin.getLogger().warning("[SaberDropListener] Player " + player.getName() + " tried to toggle a saber with unknown type: " + internalSaberName);
                    return;
                }

                String currentState = pdc.getOrDefault(saberStateKey, PersistentDataType.STRING, "inactive");
                String newState;

                if ("inactive".equalsIgnoreCase(currentState)) {
                    newState = "active";
                    pdc.set(saberStateKey, PersistentDataType.STRING, newState);
                    itemMetaFromEvent.setCustomModelData(saberType.getActiveModelId());
                } else {
                    newState = "inactive";
                    pdc.set(saberStateKey, PersistentDataType.STRING, newState);
                    itemMetaFromEvent.setCustomModelData(saberType.getInactiveModelId());
                }

                // Apply the meta changes directly to the ItemStack object we got from the event
                itemStackFromEvent.setItemMeta(itemMetaFromEvent);

                // VERY IMPORTANT: Update the item stack within the event's Item entity.
                // This tells the server "the item that would have been dropped is now this modified version."
                event.getItemDrop().setItemStack(itemStackFromEvent);

                // Now, cancel the event. The server should ensure the player "keeps" the item
                // (which is now the modified itemStackFromEvent) in their inventory slot.
                event.setCancelled(true);

                // Play sounds based on the new state
                if ("active".equals(newState)) {
                    player.playSound(player.getLocation(), saberType.getActivateSound(), SoundCategory.PLAYERS, 1.0f, 1.0f);
                    player.playSound(player.getLocation(), saberType.getHumSound(), SoundCategory.PLAYERS, 0.7f, 1.0f);
                } else { // "inactive"
                    player.stopSound(saberType.getHumSound(), SoundCategory.PLAYERS);
                    player.playSound(player.getLocation(), saberType.getDeactivateSound(), SoundCategory.PLAYERS, 1.0f, 1.0f);
                }
                // No need to call player.getInventory().setItemInMainHand() here.
                // The cancellation of the event, after updating the event's item, should handle it.
            }
        }
    }
}