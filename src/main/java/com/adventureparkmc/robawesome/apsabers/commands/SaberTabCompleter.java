package com.adventureparkmc.robawesome.apsabers.commands;

import com.adventureparkmc.robawesome.apsabers.APSabers;
import com.adventureparkmc.robawesome.apsabers.SaberType;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SaberTabCompleter implements TabCompleter {

    private final APSabers plugin;
    private final List<String> SUBCOMMANDS_ADMIN = Arrays.asList("give", "reload", "list");
    // If you had user-specific subcommands, you'd define them too.

    public SaberTabCompleter(APSabers plugin) {
        this.plugin = plugin;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        String currentArg = args[args.length - 1].toLowerCase();

        // Tab completing the first argument (subcommands)
        if (args.length == 1) {
            // Suggest subcommands based on permission, though for simplicity,
            // often all are suggested and execution handles permissions.
            // Here, we'll filter by what they can conceptually use or see.
            if (sender.hasPermission("apsabers.admin")) { // Or more granular checks
                StringUtil.copyPartialMatches(currentArg, SUBCOMMANDS_ADMIN, completions);
            } else {
                // If you add non-admin commands later, suggest them here.
                // For now, an empty list or a "no permission" hint (though that's not typical for tab-complete)
                // StringUtil.copyPartialMatches(currentArg, Arrays.asList("help"), completions); // example
            }
        }
        // Tab completing arguments for subcommands
        else if (args.length > 1) {
            String subCommand = args[0].toLowerCase();

            if ("give".equalsIgnoreCase(subCommand)) {
                if (sender.hasPermission("apsabers.admin.give") || sender.hasPermission("apsabers.admin")) {
                    if (args.length == 2) { // Suggest player names
                        List<String> playerNames = Bukkit.getOnlinePlayers().stream()
                                .map(Player::getName)
                                .collect(Collectors.toList());
                        StringUtil.copyPartialMatches(currentArg, playerNames, completions);
                    } else if (args.length == 3) { // Suggest saber types
                        Set<String> saberTypeNames = plugin.getConfigManager().getAllSaberTypeNames();
                        StringUtil.copyPartialMatches(currentArg, saberTypeNames, completions);
                    } else if (args.length == 4) { // Suggest amounts
                        StringUtil.copyPartialMatches(currentArg, Arrays.asList("1", "16", "64", "[amount]"), completions);
                    }
                }
            }
            // No specific tab completions for 'reload' or 'list' beyond the subcommand itself
        }

        Collections.sort(completions); // Sort alphabetically
        return completions;
    }
}