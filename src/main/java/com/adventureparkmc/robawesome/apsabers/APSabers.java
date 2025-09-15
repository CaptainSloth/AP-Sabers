package com.adventureparkmc.robawesome.apsabers;

import com.adventureparkmc.robawesome.apsabers.abilities.AbilityManager;
import com.adventureparkmc.robawesome.apsabers.commands.SaberCommand;
import com.adventureparkmc.robawesome.apsabers.commands.SaberTabCompleter;
import com.adventureparkmc.robawesome.apsabers.gui.SaberMenuListener;
import com.adventureparkmc.robawesome.apsabers.listeners.*;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class APSabers extends JavaPlugin {

    private static APSabers instance;
    private ConfigManager configManager;
    private AbilityManager abilityManager;

    @Override
    public void onEnable() {
        instance = this;

        // Initialize ConfigManager
        configManager = new ConfigManager(this);
        configManager.loadConfig(); // This will also save default if not present

        // Initialize AbilityManager
        abilityManager = new AbilityManager(this);

        // Register Commands
        try {
            Objects.requireNonNull(getCommand("apsabers")).setExecutor(new SaberCommand(this));
            // Add this line to register the TabCompleter
            Objects.requireNonNull(getCommand("apsabers")).setTabCompleter(new SaberTabCompleter(this));
        } catch (NullPointerException e) {
            getLogger().severe("Could not register 'apsabers' command or tab completer! Is it defined in plugin.yml correctly?");
        }


        // Register Event Listeners
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerDropItemListener(this), this);
        pm.registerEvents(new PlayerInteractListener(this), this);
        pm.registerEvents(new EntityDamageListener(this), this); // Renamed for clarity to cover more entity damage scenarios
        pm.registerEvents(new PlayerQuitListener(this), this); // For cleaning up player-specific data like active throws
        pm.registerEvents(new PlayerToggleSneakListener(this), this);
        pm.registerEvents(new BlockBreakListener(this), this);
        pm.registerEvents(new SaberMenuListener(this), this);

        getLogger().info("--------------------------------------");
        getLogger().info("AP-Sabers version " + this.getDescription().getVersion() + " has been enabled!");
        getLogger().info("Developed by: " + this.getDescription().getAuthors());
        getLogger().info("May the Force be with you!");
        getLogger().info("--------------------------------------");
    }

    @Override
    public void onDisable() {
        // Clean up any active tasks or data
        if (abilityManager != null) {
            abilityManager.cleanupSaberThrows(); // Important for any saber throw tasks
        }

        getLogger().info("AP-Sabers has been disabled.");
    }

    // Getters
    public static APSabers getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public AbilityManager getAbilityManager() {
        return abilityManager;
    }
}