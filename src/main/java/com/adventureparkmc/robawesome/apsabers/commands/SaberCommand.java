package com.adventureparkmc.robawesome.apsabers.commands;

import com.adventureparkmc.robawesome.apsabers.APSabers;
import com.adventureparkmc.robawesome.apsabers.SaberType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment; // Ensure this import is present
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map; // Required for accessing plugin.yml command attributes
import java.util.Set;
import java.util.stream.Collectors;

public class SaberCommand implements CommandExecutor {

    private final APSabers plugin;
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacyAmpersand();

    public SaberCommand(APSabers plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "give":
                handleGiveCommand(sender, args, command); // Pass the command object
                break;
            case "reload":
                handleReloadCommand(sender);
                break;
            case "list":
                handleListCommand(sender);
                break;
            default:
                sendHelpMessage(sender);
                break;
        }
        return true;
    }

    private void handleGiveCommand(CommandSender sender, String[] args, Command cmd) { // Added Command cmd parameter
        if (!sender.hasPermission("apsabers.admin.give") && !sender.hasPermission("apsabers.admin")) {
            sender.sendMessage(Component.text("You do not have permission to use this command.").color(NamedTextColor.RED));
            return;
        }

        if (args.length < 3) {
            // Fix for line 70: Get usage string more safely
            // The cmd.getUsage() often includes the /<command> prefix already.
            // If you want the raw string from plugin.yml to pick a specific line:
            String usageString = "";
            Map<String, Map<String, Object>> commandsMap = plugin.getDescription().getCommands();
            if (commandsMap != null && commandsMap.containsKey("apsabers")) {
                Object rawUsageObject = commandsMap.get("apsabers").get("usage");
                if (rawUsageObject instanceof String) {
                    usageString = (String) rawUsageObject;
                }
            }

            if (!usageString.isEmpty()) {
                String firstUsageLine = usageString.split("\n")[0].replace("<command>", cmd.getLabel());
                sender.sendMessage(legacySerializer.deserialize("Usage: " + firstUsageLine).colorIfAbsent(NamedTextColor.RED));
            } else {
                // Fallback if parsing plugin.yml fails for some reason
                sender.sendMessage(Component.text("Usage: /" + cmd.getLabel() + " give <player> <saber_type> [amount]").color(NamedTextColor.RED));
            }
            return;
        }

        Player targetPlayer = Bukkit.getPlayer(args[1]);
        if (targetPlayer == null) {
            sender.sendMessage(Component.text("Player '" + args[1] + "' not found.").color(NamedTextColor.RED));
            return;
        }

        String saberTypeName = args[2].toLowerCase();
        SaberType saberType = plugin.getConfigManager().getSaberType(saberTypeName);
        if (saberType == null) {
            sender.sendMessage(Component.text("Saber type '" + args[2] + "' not found. Use '/apsabers list' to see available types.").color(NamedTextColor.RED));
            return;
        }

        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
                if (amount < 1) {
                    sender.sendMessage(Component.text("Amount must be at least 1.").color(NamedTextColor.RED));
                    return;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("Invalid amount specified.").color(NamedTextColor.RED));
                return;
            }
        }

        ItemStack saberStack = createSaberItemStack(saberType);
        if (saberStack == null) {
            sender.sendMessage(Component.text("Failed to create lightsaber item for type: " + saberTypeName + ". Check server logs for material errors.").color(NamedTextColor.RED));
            return;
        }

        saberStack.setAmount(amount);
        targetPlayer.getInventory().addItem(saberStack);

        Component saberDisplayNameComponent = legacySerializer.deserialize(saberType.getDisplayName());
        sender.sendMessage(Component.text("Gave " + amount + "x ")
                .color(NamedTextColor.GREEN)
                .append(saberDisplayNameComponent)
                .append(Component.text(" to " + targetPlayer.getName() + ".").color(NamedTextColor.GREEN)));

        targetPlayer.sendMessage(Component.text("You have received " + amount + "x ")
                .color(NamedTextColor.GREEN)
                .append(saberDisplayNameComponent)
                .append(Component.text("!").color(NamedTextColor.GREEN)));
    }

    private ItemStack createSaberItemStack(SaberType saberType) {
        if (saberType.getItemMaterial() == null) {
            plugin.getLogger().warning("SaberType " + saberType.getInternalName() + " has a null material!");
            return null;
        }

        ItemStack saberItem = new ItemStack(saberType.getItemMaterial());
        ItemMeta meta = saberItem.getItemMeta();

        if (meta == null) {
            plugin.getLogger().severe("Could not get ItemMeta for material: " + saberType.getItemMaterial().name() + "! This is a critical issue.");
            return new ItemStack(Material.STICK);
        }

        meta.displayName(legacySerializer.deserialize(saberType.getDisplayName()));

        List<Component> loreComponents = saberType.getLore().stream()
                .map(legacySerializer::deserialize)
                .collect(Collectors.toList());
        meta.lore(loreComponents);

        meta.setCustomModelData(saberType.getInactiveModelId());

        if (saberType.isEnchanted()) {
            // Fix for line 147: Use a valid Enchantment constant
            meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true); // Using VANISHING_CURSE for glint
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        NamespacedKey typeKey = new NamespacedKey(plugin, "saber_type");
        NamespacedKey stateKey = new NamespacedKey(plugin, "saber_state");

        meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, saberType.getInternalName());
        meta.getPersistentDataContainer().set(stateKey, PersistentDataType.STRING, "inactive");

        saberItem.setItemMeta(meta);
        return saberItem;
    }

    private void handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("apsabers.admin.reload") && !sender.hasPermission("apsabers.admin")) {
            sender.sendMessage(Component.text("You do not have permission to use this command.").color(NamedTextColor.RED));
            return;
        }
        plugin.getConfigManager().loadConfig();
        sender.sendMessage(Component.text(plugin.getDescription().getName() + " configuration reloaded!").color(NamedTextColor.GREEN));
    }

    private void handleListCommand(CommandSender sender) {
        if (!sender.hasPermission("apsabers.admin.list") && !sender.hasPermission("apsabers.admin")) {
            sender.sendMessage(Component.text("You do not have permission to use this command.").color(NamedTextColor.RED));
            return;
        }

        Set<String> saberNames = plugin.getConfigManager().getAllSaberTypeNames();
        if (saberNames.isEmpty()) {
            sender.sendMessage(Component.text("No lightsaber types are defined in the configuration.").color(NamedTextColor.YELLOW));
            return;
        }

        sender.sendMessage(Component.text("--- Available Lightsaber Types ---").color(NamedTextColor.GOLD));
        for (String internalName : saberNames) {
            SaberType type = plugin.getConfigManager().getSaberType(internalName);
            Component line = Component.text("- ", NamedTextColor.AQUA)
                    .append(Component.text(internalName, NamedTextColor.AQUA));
            if (type != null) {
                line = line.append(Component.text(" (Display: ", NamedTextColor.GRAY))
                        .append(legacySerializer.deserialize(type.getDisplayName()))
                        .append(Component.text(")", NamedTextColor.GRAY));
            } else {
                line = line.append(Component.text(" (Error loading details)", NamedTextColor.RED));
            }
            sender.sendMessage(line);
        }
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(Component.text("--- APSabers Help ---").color(NamedTextColor.GOLD));
        if (sender.hasPermission("apsabers.admin") || sender.hasPermission("apsabers.admin.give")) {
            sender.sendMessage(Component.text("/apsabers give <player> <saber_type> [amount]", NamedTextColor.AQUA)
                    .append(Component.text(" - Gives a lightsaber.", NamedTextColor.GRAY)));
        }
        if (sender.hasPermission("apsabers.admin") || sender.hasPermission("apsabers.admin.list")) {
            sender.sendMessage(Component.text("/apsabers list", NamedTextColor.AQUA)
                    .append(Component.text(" - Lists available saber types.", NamedTextColor.GRAY)));
        }
        if (sender.hasPermission("apsabers.admin") || sender.hasPermission("apsabers.admin.reload")) {
            sender.sendMessage(Component.text("/apsabers reload", NamedTextColor.AQUA)
                    .append(Component.text(" - Reloads the plugin configuration.", NamedTextColor.GRAY)));
        }
        boolean hasAnyAdminPerm = sender.hasPermission("apsabers.admin") ||
                sender.hasPermission("apsabers.admin.give") ||
                sender.hasPermission("apsabers.admin.list") ||
                sender.hasPermission("apsabers.admin.reload");
        if (!hasAnyAdminPerm && !(sender instanceof Player && ((Player)sender).isOp())) {
            sender.sendMessage(Component.text("You do not have permission to use any admin commands.").color(NamedTextColor.YELLOW));
        }
    }
}