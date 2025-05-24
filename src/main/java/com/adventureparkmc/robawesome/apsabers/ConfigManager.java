package com.adventureparkmc.robawesome.apsabers;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public class ConfigManager {

    private final APSabers plugin;
    private FileConfiguration config;
    private final Map<String, SaberType> saberTypes = new HashMap<>();
    private String activationKey;
    private double duelDamagePerHit;
    private int duelHealthPoints;

    public ConfigManager(APSabers plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        // Save the default config.yml from the plugin's resources folder if it doesn't exist
        plugin.saveDefaultConfig();
        // Reload the configuration from disk
        plugin.reloadConfig();
        // Get the loaded configuration
        config = plugin.getConfig();

        // Load activation key
        activationKey = config.getString("activationKey", "DROP_ITEM").toUpperCase();

        // Load saber types
        loadSaberTypes();

        plugin.getLogger().info("Configuration loaded. " + saberTypes.size() + " saber types found. Activation Key: " + activationKey);
    }

    private void loadSaberTypes() {
        saberTypes.clear(); // Clear existing types before reloading
        ConfigurationSection sabersSection = config.getConfigurationSection("sabers");

        if (sabersSection == null) {
            plugin.getLogger().warning("'sabers' section not found in config.yml!");
            return;
        }

        for (String internalName : sabersSection.getKeys(false)) {
            ConfigurationSection saberSection = sabersSection.getConfigurationSection(internalName);
            if (saberSection == null) {
                plugin.getLogger().warning("Invalid configuration for saber: " + internalName + ". Skipping.");
                continue;
            }

            try {
                String displayName = saberSection.getString("displayName", "&fUnnamed Saber");
                List<String> lore = saberSection.getStringList("lore");
                if (lore.isEmpty()) {
                    lore.add("&7A mysterious lightsaber.");
                }

                String materialName = saberSection.getString("item", "IRON_SHOVEL").toUpperCase();
                Material itemMaterial = Material.matchMaterial(materialName);
                if (itemMaterial == null) {
                    plugin.getLogger().warning("Invalid material '" + materialName + "' for saber '" + internalName + "'. Defaulting to IRON_SHOVEL.");
                    itemMaterial = Material.IRON_SHOVEL;
                }

                boolean enchanted = saberSection.getBoolean("enchanted", false);
                int inactiveModelId = saberSection.getInt("inactiveModelId", 0);
                int activeModelId = saberSection.getInt("activeModelId", 0);

                // Sounds
                String activateSound = saberSection.getString("sounds.activate", "minecraft:block.beacon.activate");
                String deactivateSound = saberSection.getString("sounds.deactivate", "minecraft:block.beacon.deactivate");
                String humSound = saberSection.getString("sounds.hum", "minecraft:block.beacon.ambient"); // Placeholder
                String swingSound = saberSection.getString("sounds.swing", "minecraft:entity.player.attack.sweep");
                String clashSound = saberSection.getString("sounds.clash", "minecraft:item.shield.block");

                // Optional Combat Stats
                double damage = saberSection.getDouble("damage", 1.0); // Default damage if not specified

                // Abilities
                ConfigurationSection abilitiesSection = saberSection.getConfigurationSection("enabled_abilities");
                boolean blockDeflection = false;
                boolean forcePush = false;
                boolean saberThrow = false;
                boolean blasterBoltBlock = false;
                boolean forceLeap = false;
                boolean stealthCloak = false;

                if (abilitiesSection != null) {
                    blockDeflection = abilitiesSection.getBoolean("block_deflection", false);
                    forcePush = abilitiesSection.getBoolean("force_push", false);
                    saberThrow = abilitiesSection.getBoolean("saber_throw", false);
                    blasterBoltBlock = abilitiesSection.getBoolean("blaster_bolt_block", false);
                    forceLeap = abilitiesSection.getBoolean("force_leap", false);
                    stealthCloak = abilitiesSection.getBoolean("stealth_cloak", false);
                } else {
                    plugin.getLogger().info("No 'enabled_abilities' section for saber '" + internalName + "'. All abilities defaulted to false.");
                }


                SaberType saberType = new SaberType(internalName, displayName, lore, itemMaterial,
                        enchanted, inactiveModelId, activeModelId,
                        activateSound, deactivateSound, humSound, swingSound, clashSound, damage,
                        blockDeflection, forcePush, saberThrow, blasterBoltBlock, forceLeap, stealthCloak);

                saberTypes.put(internalName.toLowerCase(), saberType); // Store with lowercase key for case-insensitive lookup
                plugin.getLogger().info("Loaded saber type: " + internalName);

            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error loading saber type '" + internalName + "': " + e.getMessage(), e);
            }
        }
    }

    public SaberType getSaberType(String internalName) {
        if (internalName == null) return null;
        return saberTypes.get(internalName.toLowerCase());
    }

    public Set<String> getAllSaberTypeNames() {
        return saberTypes.keySet(); // Returns the internal names (lowercase)
    }

    public String getActivationKey() {
        return activationKey;
    }

    // In loadConfig() method, after loading sabers and activationKey:
    private void loadDuelSettings() { // Call this from loadConfig()
        ConfigurationSection duelSection = config.getConfigurationSection("duelSettings");
        if (duelSection != null) {
            this.duelDamagePerHit = duelSection.getDouble("damagePerHit", 1.0);
            this.duelHealthPoints = duelSection.getInt("healthPoints", 10);
            plugin.getLogger().info("Duel Settings: Damage/Hit=" + duelDamagePerHit + ", Health=" + duelHealthPoints);
        } else {
            this.duelDamagePerHit = 1.0; // Default values
            this.duelHealthPoints = 10;  // Default values
            plugin.getLogger().warning("Could not find 'duelSettings' in config.yml. Using defaults (1 damage, 10 health).");
        }
    }

    // Add getters:
    public double getDuelDamagePerHit() {
        return duelDamagePerHit;
    }

    public int getDuelHealthPoints() {
        return duelHealthPoints;
    }

    // You might want a method to get the raw FileConfiguration if needed elsewhere,
    // but it's generally better to provide specific getters like getActivationKey().
    public FileConfiguration getRawConfig() {
        return config;
    }
}