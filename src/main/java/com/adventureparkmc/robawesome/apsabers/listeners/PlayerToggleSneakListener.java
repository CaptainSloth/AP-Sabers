package com.adventureparkmc.robawesome.apsabers.listeners;

import com.adventureparkmc.robawesome.apsabers.APSabers;
import com.adventureparkmc.robawesome.apsabers.SaberType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

public class PlayerToggleSneakListener implements Listener {

    private final APSabers plugin;
    private final NamespacedKey saberTypeKey;
    private final NamespacedKey saberStateKey;
    private final long CLOAK_SNEAK_HOLD_DURATION_TICKS = 40L; // 2 seconds (20 ticks/sec)

    public PlayerToggleSneakListener(APSabers plugin) {
        this.plugin = plugin;
        this.saberTypeKey = new NamespacedKey(plugin, "saber_type");
        this.saberStateKey = new NamespacedKey(plugin, "saber_state");
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (itemInHand.getType().isAir() || !itemInHand.hasItemMeta()) {
            plugin.getAbilityManager().cancelSneakCloakTask(player); // Cancel if they stop sneaking while not holding a saber
            return;
        }

        ItemMeta meta = itemInHand.getItemMeta();
        if (meta == null || !meta.getPersistentDataContainer().has(saberTypeKey, PersistentDataType.STRING)) {
            plugin.getAbilityManager().cancelSneakCloakTask(player);
            return;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String internalSaberName = pdc.get(saberTypeKey, PersistentDataType.STRING);
        SaberType saberType = plugin.getConfigManager().getSaberType(internalSaberName);

        if (saberType == null || !saberType.isStealthCloakEnabled()) {
            plugin.getAbilityManager().cancelSneakCloakTask(player);
            return;
        }

        String saberState = pdc.getOrDefault(saberStateKey, PersistentDataType.STRING, "inactive");
        if (!"active".equalsIgnoreCase(saberState)) {
            plugin.getAbilityManager().cancelSneakCloakTask(player);
            return;
        }

        if (event.isSneaking()) {
            // Player starts sneaking
            if (plugin.getAbilityManager().isCloaked(player)) return; // Already cloaked, do nothing

            // Start a task to check if they hold sneak for the duration
            BukkitRunnable cloakTask = new BukkitRunnable() {
                @Override
                public void run() {
                    // Check if player is still sneaking and holding the same active saber
                    if (player.isOnline() && player.isSneaking()) {
                        ItemStack currentItem = player.getInventory().getItemInMainHand();
                        if (currentItem.isSimilar(itemInHand)) { // Ensure it's still the same saber
                            ItemMeta currentMeta = currentItem.getItemMeta();
                            if(currentMeta != null && currentMeta.getPersistentDataContainer().has(saberStateKey, PersistentDataType.STRING) &&
                                    "active".equalsIgnoreCase(currentMeta.getPersistentDataContainer().get(saberStateKey, PersistentDataType.STRING))){
                                plugin.getAbilityManager().attemptStealthCloakFromSneak(player, currentItem);
                            }
                        }
                    }
                }
            };
            plugin.getAbilityManager().addSneakCloakTask(player, cloakTask);
            cloakTask.runTaskLater(plugin, CLOAK_SNEAK_HOLD_DURATION_TICKS);
            player.sendActionBar(Component.text("Hold sneak to cloak...").color(NamedTextColor.DARK_AQUA));

        } else {
            // Player stops sneaking
            plugin.getAbilityManager().cancelSneakCloakTask(player);
            // If they were in the process of holding sneak to cloak but hadn't activated yet
            player.sendActionBar(Component.text("Cloak attempt cancelled.").color(NamedTextColor.GRAY));
        }
    }
}