package com.nonkungch.dialogueseconomy;

import org.bukkit.plugin.java.JavaPlugin;
import net.milkbowl.vault.economy.Economy;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DialoguesEconomy extends JavaPlugin {

    private static Economy economy;
    public final Map<UUID, DialogueState> activeDialogues = new HashMap<>();

    private File configFile;
    private File dialogueFile;

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) getDataFolder().mkdirs();

        configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) saveResource("config.yml", false);

        dialogueFile = new File(getDataFolder(), "dialogue.yml");
        if (!dialogueFile.exists()) saveResource("dialogue.yml", false);

        saveDefaultConfig();

        // Vault economy
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            if (getServer().getServicesManager().getRegistration(Economy.class) != null) {
                economy = getServer().getServicesManager().getRegistration(Economy.class).getProvider();
                getLogger().info("Vault Economy hooked successfully!");
            }
        }

        // Register command
        this.getCommand("dialogue").setExecutor(new DialogueCommand(this));

        getLogger().info("DialoguesEconomy enabled!");
    }

    public static Economy getEconomy() {
        return economy;
    }

    public File getDialogueFile() {
        return dialogueFile;
    }
}
