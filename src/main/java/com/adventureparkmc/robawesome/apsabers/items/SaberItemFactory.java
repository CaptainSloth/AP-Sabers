package com.adventureparkmc.robawesome.apsabers.items;

import com.adventureparkmc.robawesome.apsabers.APSabers;
import com.adventureparkmc.robawesome.apsabers.SaberType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class responsible for building lightsaber {@link ItemStack}s.
 * Provides methods for creating the actual usable saber item as well as
 * display icons used in menus.
 */
public final class SaberItemFactory {

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    private SaberItemFactory() {
        // Utility class
    }

    /**
     * Creates a functional lightsaber {@link ItemStack} for the supplied {@link SaberType}.
     * The returned item includes all persistent data required by the plugin to track state.
     *
     * @param plugin    plugin instance used for logging and namespaced keys
     * @param saberType configuration describing the saber to create
     * @return fully configured {@link ItemStack} ready to be given to a player
     */
    public static ItemStack createSaber(APSabers plugin, SaberType saberType) {
        ItemStack saberItem = new ItemStack(saberType.getItemMaterial());
        ItemMeta meta = saberItem.getItemMeta();

        if (meta == null) {
            plugin.getLogger().severe("Could not obtain ItemMeta for material "
                    + saberType.getItemMaterial().name() + ". Using fallback STICK item.");
            return new ItemStack(Material.STICK);
        }

        applyBaseMeta(meta, saberType);

        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(new NamespacedKey(plugin, "saber_type"), PersistentDataType.STRING, saberType.getInternalName());
        container.set(new NamespacedKey(plugin, "saber_state"), PersistentDataType.STRING, "inactive");

        saberItem.setItemMeta(meta);
        return saberItem;
    }

    /**
     * Builds a decorative icon representing the provided {@link SaberType}. Icons are used inside
     * informational GUIs and omit persistent data that would otherwise mark them as functional sabers.
     *
     * @param saberType configuration describing the saber to display
     * @return {@link ItemStack} suitable for menu usage
     */
    public static ItemStack createSaberIcon(SaberType saberType) {
        ItemStack icon = new ItemStack(saberType.getItemMaterial());
        ItemMeta meta = icon.getItemMeta();

        if (meta == null) {
            return icon;
        }

        applyBaseMeta(meta, saberType);

        // Provide a little more context for players browsing the menu
        List<Component> lore = meta.lore();
        if (lore == null) {
            lore = new ArrayList<>();
        } else {
            lore = new ArrayList<>(lore);
        }

        if (!lore.isEmpty()) {
            lore.add(Component.empty());
        }

        lore.add(Component.text("Damage: " + saberType.getDamage(), NamedTextColor.GRAY));

        List<String> abilityLines = buildAbilityLore(saberType);
        if (!abilityLines.isEmpty()) {
            lore.add(Component.text("Abilities:", NamedTextColor.GOLD));
            for (String line : abilityLines) {
                lore.add(Component.text(line, NamedTextColor.DARK_AQUA));
            }
        }

        meta.lore(lore);
        icon.setItemMeta(meta);
        return icon;
    }

    private static void applyBaseMeta(ItemMeta meta, SaberType saberType) {
        meta.displayName(LEGACY_SERIALIZER.deserialize(saberType.getDisplayName()));

        List<Component> loreComponents = new ArrayList<>();
        for (String line : saberType.getLore()) {
            loreComponents.add(LEGACY_SERIALIZER.deserialize(line));
        }
        meta.lore(loreComponents);

        meta.setCustomModelData(saberType.getInactiveModelId());

        if (saberType.isEnchanted()) {
            meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
    }

    private static List<String> buildAbilityLore(SaberType saberType) {
        List<String> lines = new ArrayList<>();

        if (saberType.isBlockDeflectionEnabled()) {
            lines.add("• Block Deflection");
        }
        if (saberType.isForcePushEnabled()) {
            lines.add("• Force Push");
        }
        if (saberType.isSaberThrowEnabled()) {
            lines.add("• Saber Throw");
        }
        if (saberType.isBlasterBoltBlockEnabled()) {
            lines.add("• Blaster Bolt Block");
        }
        if (saberType.isForceLeapEnabled()) {
            lines.add("• Force Leap");
        }
        if (saberType.isStealthCloakEnabled()) {
            lines.add("• Stealth Cloak");
        }

        return lines;
    }
}
