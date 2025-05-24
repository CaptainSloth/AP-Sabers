package com.adventureparkmc.robawesome.apsabers;

import org.bukkit.Material;
import org.bukkit.ChatColor; // For translating color codes in lore/displayName if needed here later

import java.util.List;
import java.util.ArrayList;

public class SaberType {

    private final String internalName;
    private final String displayName;
    private final List<String> lore;
    private final Material itemMaterial;
    private final boolean enchanted;
    private final int inactiveModelId;
    private final int activeModelId;

    // Sounds
    private final String activateSound;
    private final String deactivateSound;
    private final String humSound;
    private final String swingSound;
    private final String clashSound;

    // Optional Combat Stats
    private final double damage; // Default to a base value if not specified

    // Abilities
    private final boolean blockDeflectionEnabled;
    private final boolean forcePushEnabled;
    private final boolean saberThrowEnabled;
    private final boolean blasterBoltBlockEnabled;
    private final boolean forceLeapEnabled;
    private final boolean stealthCloakEnabled;

    public SaberType(String internalName, String displayName, List<String> lore, Material itemMaterial,
                     boolean enchanted, int inactiveModelId, int activeModelId,
                     String activateSound, String deactivateSound, String humSound,
                     String swingSound, String clashSound, double damage,
                     boolean blockDeflectionEnabled, boolean forcePushEnabled, boolean saberThrowEnabled,
                     boolean blasterBoltBlockEnabled, boolean forceLeapEnabled, boolean stealthCloakEnabled) {
        this.internalName = internalName;
        this.displayName = displayName;
        this.lore = new ArrayList<>(lore); // Create a new list to ensure immutability if original is modified
        this.itemMaterial = itemMaterial;
        this.enchanted = enchanted;
        this.inactiveModelId = inactiveModelId;
        this.activeModelId = activeModelId;
        this.activateSound = activateSound;
        this.deactivateSound = deactivateSound;
        this.humSound = humSound;
        this.swingSound = swingSound;
        this.clashSound = clashSound;
        this.damage = damage;
        this.blockDeflectionEnabled = blockDeflectionEnabled;
        this.forcePushEnabled = forcePushEnabled;
        this.saberThrowEnabled = saberThrowEnabled;
        this.blasterBoltBlockEnabled = blasterBoltBlockEnabled;
        this.forceLeapEnabled = forceLeapEnabled;
        this.stealthCloakEnabled = stealthCloakEnabled;
    }

    // --- Getters ---
    public String getInternalName() {
        return internalName;
    }

    public String getDisplayName() {
        // Color codes will be translated when creating the ItemStack
        return displayName;
    }

    public List<String> getLore() {
        // Color codes will be translated when creating the ItemStack
        return new ArrayList<>(lore); // Return a copy
    }

    public Material getItemMaterial() {
        return itemMaterial;
    }

    public boolean isEnchanted() {
        return enchanted;
    }

    public int getInactiveModelId() {
        return inactiveModelId;
    }

    public int getActiveModelId() {
        return activeModelId;
    }

    public String getActivateSound() {
        return activateSound;
    }

    public String getDeactivateSound() {
        return deactivateSound;
    }

    public String getHumSound() {
        return humSound;
    }

    public String getSwingSound() {
        return swingSound;
    }

    public String getClashSound() {
        return clashSound;
    }

    public double getDamage() {
        return damage;
    }

    public boolean isBlockDeflectionEnabled() {
        return blockDeflectionEnabled;
    }

    public boolean isForcePushEnabled() {
        return forcePushEnabled;
    }

    public boolean isSaberThrowEnabled() {
        return saberThrowEnabled;
    }

    public boolean isBlasterBoltBlockEnabled() {
        return blasterBoltBlockEnabled;
    }

    public boolean isForceLeapEnabled() {
        return forceLeapEnabled;
    }

    public boolean isStealthCloakEnabled() {
        return stealthCloakEnabled;
    }
}