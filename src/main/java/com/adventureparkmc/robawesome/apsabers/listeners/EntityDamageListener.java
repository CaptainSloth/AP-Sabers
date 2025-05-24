package com.adventureparkmc.robawesome.apsabers.listeners;

import com.adventureparkmc.robawesome.apsabers.APSabers;
import com.adventureparkmc.robawesome.apsabers.SaberType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class EntityDamageListener implements Listener {

    private final APSabers plugin;
    private final NamespacedKey saberTypeKey;
    private final NamespacedKey saberStateKey;

    public EntityDamageListener(APSabers plugin) {
        this.plugin = plugin;
        this.saberTypeKey = new NamespacedKey(plugin, "saber_type");
        this.saberStateKey = new NamespacedKey(plugin, "saber_state");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity victim = event.getEntity();

        // --- Case 1: Player with Lightsaber attacks an entity ---
        if (damager instanceof Player) {
            Player attackingPlayer = (Player) damager;
            ItemStack itemInHand = attackingPlayer.getInventory().getItemInMainHand();

            if (itemInHand.getType().isAir() || !itemInHand.hasItemMeta()) {
                return;
            }
            ItemMeta meta = itemInHand.getItemMeta();
            if (meta == null) return;

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            if (pdc.has(saberTypeKey, PersistentDataType.STRING)) {
                String saberState = pdc.getOrDefault(saberStateKey, PersistentDataType.STRING, "inactive");

                if ("active".equalsIgnoreCase(saberState)) {
                    SaberType saberType = plugin.getConfigManager().getSaberType(pdc.get(saberTypeKey, PersistentDataType.STRING));
                    if (saberType == null) return;

                    if (plugin.getAbilityManager().isCloaked(attackingPlayer)) {
                        plugin.getAbilityManager().deactivateStealthCloak(attackingPlayer, "Attacked an entity.");
                    }

                    // --- PvP Damage Handling ---
                    if (victim instanceof Player) {
                        Player defendingPlayer = (Player) victim;
                        // TODO: Implement Duel System Check in DuelManager
                        // boolean inDuel = plugin.getDuelManager().arePlayersInDuel(attackingPlayer, defendingPlayer);
                        boolean inDuel = false; // Placeholder: by default, PvP is off. Set to true for testing duel damage.

                        if (inDuel) {
                            // PvP is allowed (duel ongoing) - Apply configured duel damage to actual player health
                            victim.getWorld().playSound(victim.getLocation(), saberType.getClashSound(), SoundCategory.PLAYERS, 1.0f, 1.0f);
                            double duelDamage = plugin.getConfigManager().getDuelDamagePerHit();
                            event.setDamage(duelDamage); // Apply the configured duel damage
                            // The event is NOT cancelled here, so normal health is affected.
                            // The DuelManager would later listen to PlayerDeathEvent.
                        } else {
                            // PvP is NOT allowed
                            event.setCancelled(true);
                            attackingPlayer.sendActionBar(Component.text("You cannot harm this player with a lightsaber outside of a duel!").color(NamedTextColor.RED));
                            victim.getWorld().playSound(victim.getLocation(), saberType.getClashSound(), SoundCategory.PLAYERS, 0.5f, 0.8f); // Softer clash
                            return;
                        }
                    } else {
                        // --- PvE Damage Handling (Player vs Mob) ---
                        victim.getWorld().playSound(victim.getLocation(), saberType.getClashSound(), SoundCategory.PLAYERS, 1.0f, 1.0f);
                        if (saberType.getDamage() > 0) { // Use SaberType's damage for PvE
                            event.setDamage(saberType.getDamage());
                        }
                    }
                }
            }
        }

        // --- Case 2: Player with Lightsaber is hit by a Projectile ---
        if (victim instanceof Player) {
            Player defendingPlayer = (Player) victim;
            if (!(damager instanceof Projectile)) { // Ensure damager is actually a projectile
                // If damager is another player (melee), it's handled by Case 1 for that player.
                return;
            }
            Projectile projectileDamager = (Projectile) damager;

            ItemStack itemInHand = defendingPlayer.getInventory().getItemInMainHand();
            if (itemInHand.getType().isAir() || !itemInHand.hasItemMeta()) return;

            ItemMeta meta = itemInHand.getItemMeta();
            if (meta == null) return;

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            if (pdc.has(saberTypeKey, PersistentDataType.STRING)) {
                String saberState = pdc.getOrDefault(saberStateKey, PersistentDataType.STRING, "inactive");
                SaberType saberType = plugin.getConfigManager().getSaberType(pdc.get(saberTypeKey, PersistentDataType.STRING));
                if (saberType == null) return;

                if ("active".equalsIgnoreCase(saberState)) {
                    if (projectileDamager instanceof Arrow && saberType.isBlasterBoltBlockEnabled()) {
                        plugin.getAbilityManager().tryBlasterBoltBlockEffect(defendingPlayer, itemInHand, projectileDamager.getLocation());
                        // Optionally slightly reduce damage even if not fully deflected by BlockDeflection
                        // event.setDamage(event.getDamage() * 0.75);
                    }
                    if (saberType.isBlockDeflectionEnabled()) {
                        plugin.getAbilityManager().tryBlockDeflection(defendingPlayer, projectileDamager, itemInHand);
                        if (event.isCancelled()) {
                            return;
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();

        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            if (plugin.getAbilityManager().didRecentlyLeap(player)) {
                event.setCancelled(true);
                player.sendMessage(Component.text("The Force cushioned your fall!").color(NamedTextColor.GREEN));
            }
        }
    }
}