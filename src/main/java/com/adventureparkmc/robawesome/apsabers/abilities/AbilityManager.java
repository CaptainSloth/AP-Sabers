package com.adventureparkmc.robawesome.apsabers.abilities;

import com.adventureparkmc.robawesome.apsabers.APSabers;
import com.adventureparkmc.robawesome.apsabers.SaberType;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle; // Ensure this import is present and correct
import org.bukkit.SoundCategory;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.HashSet;

public class AbilityManager {

    private final APSabers plugin;
    private final Map<UUID, Map<String, Long>> playerAbilityCooldowns;
    private final Map<UUID, BukkitRunnable> activeSaberThrowTasks;
    private final Map<UUID, Long> recentlyLeapedPlayers;
    private final Map<UUID, BukkitRunnable> sneakCloakTasks;
    private final Set<UUID> cloakedPlayers;
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacyAmpersand();

    public AbilityManager(APSabers plugin) {
        this.plugin = plugin;
        this.playerAbilityCooldowns = new HashMap<>();
        this.activeSaberThrowTasks = new HashMap<>();
        this.recentlyLeapedPlayers = new HashMap<>();
        this.sneakCloakTasks = new HashMap<>();
        this.cloakedPlayers = new HashSet<>();
    }

    public boolean canUseAbility(Player player, String abilityName, long cooldownMillis) {
        long currentTime = System.currentTimeMillis();
        Map<String, Long> abilityCooldowns = playerAbilityCooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
        long cooldownEndTime = abilityCooldowns.getOrDefault(abilityName, 0L);

        if (currentTime >= cooldownEndTime) {
            return true;
        }
        long timeLeft = (cooldownEndTime - currentTime) / 1000;
        String formattedAbilityName = abilityName.replace("_", " ");
        formattedAbilityName = Character.toUpperCase(formattedAbilityName.charAt(0)) + formattedAbilityName.substring(1);
        player.sendActionBar(Component.text(formattedAbilityName + " on cooldown: " + timeLeft + "s").color(NamedTextColor.RED));
        return false;
    }

    public void setCooldown(Player player, String abilityName, long cooldownMillis) {
        long currentTime = System.currentTimeMillis();
        Map<String, Long> abilityCooldowns = playerAbilityCooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
        abilityCooldowns.put(abilityName, currentTime + cooldownMillis);
    }

    private SaberType getSaberTypeFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey typeKey = new NamespacedKey(plugin, "saber_type");
        if (pdc.has(typeKey, PersistentDataType.STRING)) {
            String internalName = pdc.get(typeKey, PersistentDataType.STRING);
            return plugin.getConfigManager().getSaberType(internalName);
        }
        return null;
    }

    public void tryBlockDeflection(Player player, Projectile projectile, ItemStack saberItem) {
        SaberType saberType = getSaberTypeFromItem(saberItem);
        if (saberType == null || !saberType.isBlockDeflectionEnabled()) return;

        Location impactLocation = projectile.getLocation();
        projectile.remove();
        player.getWorld().playSound(impactLocation, saberType.getClashSound(), SoundCategory.PLAYERS, 1f, 1.5f);
        // Corrected: CRIT_MAGIC (line 99 context)
        player.getWorld().spawnParticle(Particle.CRIT, impactLocation, 15, 0.2, 0.2, 0.2, 0.1);
        // Corrected: FIREWORK_SPARK (line 100 context)
        player.getWorld().spawnParticle(Particle.FLASH, impactLocation, 5, 0.1, 0.1, 0.1, 0.05);
        player.sendActionBar(Component.text("Deflected!").color(NamedTextColor.BLUE));
    }

    public void tryForcePush(Player player, ItemStack saberItem) {
        SaberType saberType = getSaberTypeFromItem(saberItem);
        if (saberType == null || !saberType.isForcePushEnabled()) return;
        if (!canUseAbility(player, "FORCE_PUSH", 5000L)) return;

        player.getWorld().playSound(player.getLocation(), "sabers.ability.force_push", SoundCategory.PLAYERS, 1f, 1.0f);
        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getEyeLocation().add(player.getLocation().getDirection()), 5, 0.5, 0.2, 0.5, 0.01);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getEyeLocation().add(player.getLocation().getDirection()), 10, 0.5, 0.2, 0.5, 0.05);

        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection();
        List<Entity> nearbyEntities = player.getNearbyEntities(6, 4, 6);

        int pushedCount = 0;
        for (Entity entity : nearbyEntities) {
            if (entity instanceof LivingEntity && entity != player) {
                Vector toEntity = entity.getLocation().toVector().subtract(eyeLocation.toVector());
                if (toEntity.normalize().dot(direction) > 0.6) {
                    Vector pushVelocity = direction.clone().multiply(2.0).setY(0.6);
                    entity.setVelocity(pushVelocity);
                    // Corrected: EXPLOSION (line 136 context) - assuming a small visual puff, not a damaging explosion
                    entity.getWorld().spawnParticle(Particle.EXPLOSION, entity.getLocation().add(0,1,0), 1, 0.1,0.1,0.1, 0); // Small count, low speed for puff
                    pushedCount++;
                }
            }
        }
        if (pushedCount > 0) {
            player.sendActionBar(Component.text("Force Pushed " + pushedCount + " entit" + (pushedCount == 1 ? "y" : "ies") + "!").color(NamedTextColor.AQUA));
        } else {
            player.sendActionBar(Component.text("Force Push didn't hit anything.").color(NamedTextColor.GRAY));
        }
        setCooldown(player, "FORCE_PUSH", 5000L);
    }

    public void trySaberThrow(Player player, ItemStack saberItem) {
        SaberType saberType = getSaberTypeFromItem(saberItem);
        if (saberType == null || !saberType.isSaberThrowEnabled()) return;
        if (activeSaberThrowTasks.containsKey(player.getUniqueId())) {
            player.sendMessage(Component.text("Your saber is already thrown!").color(NamedTextColor.RED));
            return;
        }
        if (!canUseAbility(player, "SABER_THROW", 20000L)) return;

        player.getWorld().playSound(player.getLocation(), "sabers.ability.saber_throw", SoundCategory.PLAYERS, 1f, 0.9f);

        ItemStack thrownSaberModel = new ItemStack(saberType.getItemMaterial());
        ItemMeta meta = thrownSaberModel.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(saberType.getActiveModelId());
            thrownSaberModel.setItemMeta(meta);
        }

        ArmorStand saberStand = player.getWorld().spawn(player.getEyeLocation().add(player.getLocation().getDirection().multiply(0.8)), ArmorStand.class, as -> {
            as.setVisible(false);
            as.setGravity(false);
            as.setSmall(true);
            as.setMarker(true);
            as.getEquipment().setHelmet(thrownSaberModel);
            as.setHeadPose(new EulerAngle(Math.toRadians(player.getLocation().getPitch() + 90), 0, 0));
        });

        SaberThrowRunnable runnable = new SaberThrowRunnable(plugin, player, saberStand, saberType, this);
        runnable.runTaskTimer(plugin, 0L, 1L);
        activeSaberThrowTasks.put(player.getUniqueId(), runnable);
        setCooldown(player, "SABER_THROW", 20000L);
        player.sendActionBar(Component.text("Saber Thrown!").color(NamedTextColor.BLUE));
    }

    public void completeSaberThrow(UUID playerId, boolean hitSomething) {
        BukkitRunnable task = activeSaberThrowTasks.remove(playerId);
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
        Player player = plugin.getServer().getPlayer(playerId);
        if (player != null && player.isOnline()) {
            player.getWorld().playSound(player.getLocation(), "sabers.ability.saber_return", SoundCategory.PLAYERS, 0.8f, 1.2f);
            if (!hitSomething) { // Message for hit is in SaberThrowRunnable
                player.sendActionBar(Component.text("Saber returned.").color(NamedTextColor.GRAY));
            }
        }
    }

    public void tryBlasterBoltBlockEffect(Player player, ItemStack saberItem, Location impactLocation) {
        SaberType saberType = getSaberTypeFromItem(saberItem);
        if (saberType == null || !saberType.isBlasterBoltBlockEnabled()) return;

        player.getWorld().playSound(impactLocation, "sabers.ability.bolt_block", SoundCategory.PLAYERS, 1f, 1.5f);
        // Corrected: CRIT_MAGIC (line 204 context)
        player.getWorld().spawnParticle(Particle.CRIT, impactLocation, 15, 0.1, 0.1, 0.1, 0.1);
        player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, impactLocation, 8, 0.1, 0.1, 0.1, 0.05);
    }

    public void tryForceLeap(Player player, ItemStack saberItem) {
        SaberType saberType = getSaberTypeFromItem(saberItem);
        if (saberType == null || !saberType.isForceLeapEnabled()) return;
        if (!canUseAbility(player, "FORCE_LEAP", 8000L)) return;

        player.getWorld().playSound(player.getLocation(), "sabers.ability.force_leap", SoundCategory.PLAYERS, 1f, 0.9f);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().subtract(0,0.3,0), 20, 0.4, 0.1, 0.4, 0.05);
        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation(), 3, 0.2, 0.1, 0.2, 0);

        Vector leapVelocity = player.getLocation().getDirection().multiply(2.2).setY(1.0);
        player.setVelocity(leapVelocity);
        recentlyLeapedPlayers.put(player.getUniqueId(), System.currentTimeMillis() + 5000L);

        setCooldown(player, "FORCE_LEAP", 8000L);
        player.sendActionBar(Component.text("Force Leap!").color(NamedTextColor.AQUA));
    }

    public boolean didRecentlyLeap(Player player) {
        return recentlyLeapedPlayers.getOrDefault(player.getUniqueId(), 0L) > System.currentTimeMillis();
    }
    public void removeRecentLeap(Player player) {
        recentlyLeapedPlayers.remove(player.getUniqueId());
    }

    public void attemptStealthCloakFromSneak(Player player, ItemStack saberItem) {
        SaberType saberType = getSaberTypeFromItem(saberItem);
        if (saberType == null || !saberType.isStealthCloakEnabled()) return;
        if (cloakedPlayers.contains(player.getUniqueId())) {
            player.sendActionBar(Component.text("Already cloaked!").color(NamedTextColor.GRAY));
            return;
        }
        if (!canUseAbility(player, "STEALTH_CLOAK", 60000L)) return;

        player.getWorld().playSound(player.getLocation(), "sabers.ability.cloak_on", SoundCategory.PLAYERS, 0.8f, 1f);
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20 * 15, 0, false, true, true));
        cloakedPlayers.add(player.getUniqueId());
        player.sendMessage(Component.text("You fade into the shadows...").color(NamedTextColor.DARK_AQUA));
        // Corrected: LARGE_SMOKE (line 247 context)
        player.getWorld().spawnParticle(Particle.LARGE_SMOKE, player.getLocation().add(0,1,0), 30, 0.4, 0.6, 0.4, 0.01);
        player.getWorld().spawnParticle(Particle.SQUID_INK, player.getLocation().add(0,1,0), 15, 0.5, 0.5, 0.5, 0.1);

        setCooldown(player, "STEALTH_CLOAK", 60000L);
    }

    public void deactivateStealthCloak(Player player, String reason) {
        if (cloakedPlayers.remove(player.getUniqueId())) {
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            player.getWorld().playSound(player.getLocation(), "sabers.ability.cloak_off", SoundCategory.PLAYERS, 0.8f, 1f);
            player.sendMessage(Component.text("You are no longer cloaked. ").color(NamedTextColor.YELLOW).append(Component.text(reason, NamedTextColor.GRAY)));
            // Corrected: SMOKE_NORMAL (line 259 context)
            player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0,1,0), 30, 0.4, 0.6, 0.4, 0.05);
        }
    }

    public boolean isCloaked(Player player) {
        return cloakedPlayers.contains(player.getUniqueId());
    }

    public void cancelSneakCloakTask(Player player) {
        BukkitRunnable task = sneakCloakTasks.remove(player.getUniqueId());
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }

    public void addSneakCloakTask(Player player, BukkitRunnable task) {
        cancelSneakCloakTask(player);
        sneakCloakTasks.put(player.getUniqueId(), task);
    }

    public void cleanupSaberThrows() {
        for (BukkitRunnable task : new HashMap<>(activeSaberThrowTasks).values()) {
            if (task instanceof SaberThrowRunnable) {
                ((SaberThrowRunnable) task).forceCleanup();
            } else {
                task.cancel();
            }
        }
        activeSaberThrowTasks.clear();
    }

    public void cleanupPlayer(Player player){
        UUID playerId = player.getUniqueId();
        BukkitRunnable saberThrowTask = activeSaberThrowTasks.remove(playerId);
        if (saberThrowTask instanceof SaberThrowRunnable) {
            ((SaberThrowRunnable) saberThrowTask).forceCleanup();
        } else if (saberThrowTask != null && !saberThrowTask.isCancelled()) {
            saberThrowTask.cancel();
        }

        cancelSneakCloakTask(player);
        if (isCloaked(player)) {
            deactivateStealthCloak(player, "Logged out.");
        }
        recentlyLeapedPlayers.remove(playerId);
    }
}