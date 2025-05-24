package com.adventureparkmc.robawesome.apsabers.listeners;

import com.adventureparkmc.robawesome.apsabers.APSabers;
import com.adventureparkmc.robawesome.apsabers.SaberType;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class PlayerInteractListener implements Listener {

    private final APSabers plugin;
    private final NamespacedKey saberTypeKey;
    private final NamespacedKey saberStateKey;

    public PlayerInteractListener(APSabers plugin) {
        this.plugin = plugin;
        this.saberTypeKey = new NamespacedKey(plugin, "saber_type");
        this.saberStateKey = new NamespacedKey(plugin, "saber_state");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack itemInHand = event.getItem(); // Item used for interaction

        // Ensure the interaction is with the main hand for most saber actions
        // Or if itemInHand is null (can happen with empty hand interactions)
        if (event.getHand() != EquipmentSlot.HAND || itemInHand == null || itemInHand.getType() == Material.AIR) {
            return;
        }

        ItemMeta meta = itemInHand.getItemMeta();
        if (meta == null || !meta.getPersistentDataContainer().has(saberTypeKey, PersistentDataType.STRING)) {
            // Not a lightsaber, or meta is missing (shouldn't happen for valid items)
            return;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String internalSaberName = pdc.get(saberTypeKey, PersistentDataType.STRING);
        SaberType saberType = plugin.getConfigManager().getSaberType(internalSaberName);

        if (saberType == null) {
            return; // Unknown saber type
        }

        String saberState = pdc.getOrDefault(saberStateKey, PersistentDataType.STRING, "inactive");
        boolean isActiveSaber = "active".equalsIgnoreCase(saberState);

        Action action = event.getAction();

        // --- Active Saber Logic ---
        if (isActiveSaber) {
            // 1. Left Click (Air or Block) - Swing Sound & Block Impact
            if (action == Action.LEFT_CLICK_AIR) {
                player.playSound(player.getLocation(), saberType.getSwingSound(), SoundCategory.PLAYERS, 0.8f, 1.0f);
            } else if (action == Action.LEFT_CLICK_BLOCK) {
                player.playSound(player.getLocation(), saberType.getSwingSound(), SoundCategory.PLAYERS, 0.8f, 1.0f);
                Block clickedBlock = event.getClickedBlock();
                if (clickedBlock != null) {
                    player.playSound(clickedBlock.getLocation().add(0.5, 0.5, 0.5), saberType.getClashSound(), SoundCategory.BLOCKS, 0.7f, 1.2f);
                }
                event.setCancelled(true); // Prevent block damage/breaking progress
            }

            // 2. Right Click - Abilities & Preventing Default Shovel Actions
            if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                // Prevent default shovel path creation if the item is a shovel
                if (itemInHand.getType().toString().endsWith("_SHOVEL")) {
                    if (action == Action.RIGHT_CLICK_BLOCK) {
                        // Check if the block is one that can be turned into a path, more specific if needed
                        Material blockType = event.getClickedBlock() != null ? event.getClickedBlock().getType() : null;
                        if (blockType == Material.GRASS_BLOCK || blockType == Material.DIRT_PATH ||
                                blockType == Material.DIRT || blockType == Material.COARSE_DIRT ||
                                blockType == Material.PODZOL || blockType == Material.MYCELIUM ||
                                blockType == Material.ROOTED_DIRT) {
                            event.setCancelled(true);
                        }
                    }
                }

                // --- Ability Triggers (Examples) ---
                // Force Push: Sneak + Right Click
                if (player.isSneaking()) {
                    // Check specific ability, e.g., Force Push
                    // The AbilityManager will handle if the saberType has this ability and cooldowns.
                    plugin.getAbilityManager().tryForcePush(player, itemInHand);
                    // If an ability was successfully triggered and should consume the event:
                    if(saberType.isForcePushEnabled()){ // Basic check, ability manager will do the full check
                        event.setCancelled(true); // Prevent any other right-click actions
                    }
                } else {
                    // Other right-click abilities that don't require sneaking can go here.
                    // For example, a "saber block" stance if you implement one.
                    // plugin.getAbilityManager().trySomeOtherRightClickAbility(player, itemInHand);
                }
            }
        } else {
            // --- Inactive Saber Logic (if any) ---
            // For example, preventing an inactive shovel-saber from making paths
            if ((action == Action.RIGHT_CLICK_BLOCK) && itemInHand.getType().toString().endsWith("_SHOVEL")) {
                Material blockType = event.getClickedBlock() != null ? event.getClickedBlock().getType() : null;
                if (blockType == Material.GRASS_BLOCK || blockType == Material.DIRT_PATH ||
                        blockType == Material.DIRT || blockType == Material.COARSE_DIRT ||
                        blockType == Material.PODZOL || blockType == Material.MYCELIUM ||
                        blockType == Material.ROOTED_DIRT) {
                    event.setCancelled(true);
                }
            }
        }
    }
}