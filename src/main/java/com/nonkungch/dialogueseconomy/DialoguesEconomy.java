package com.nonkungch.dialogueseconomy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import net.milkbowl.vault.economy.Economy;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DialoguesEconomy extends JavaPlugin {

    private static DialoguesEconomy instance;
    private FileConfiguration config;

    // โฟลเดอร์เก็บบทสนทนา
    private final File dialoguesFolder = new File(getDataFolder(), "dialogues");

    // แผนที่เก็บสถานะบทสนทนา (ผู้เล่นที่กำลังอยู่ใน dialogue)
    private final Map<UUID, DialogueState> activeDialogues = new HashMap<>();

    private boolean placeholderApiEnabled = false;
    private static Economy econ = null;

    @Override
    public void onEnable() {
        instance = this;

        // === สร้างไฟล์ config และโฟลเดอร์ที่จำเป็น ===
        saveDefaultConfig();
        config = getConfig();
        if (!dialoguesFolder.exists()) dialoguesFolder.mkdirs();

        // === ตรวจสอบการเชื่อมต่อกับ Vault Economy ===
        setupEconomy();

        // === ตรวจสอบ PlaceholderAPI ===
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderApiEnabled = true;
            getLogger().info("PlaceholderAPI found and enabled.");
        } else {
            getLogger().warning("PlaceholderAPI not found. Dialogue placeholders will not work.");
        }

        // === ลงทะเบียนคำสั่งทั้งหมด ===
        DialogueCommandExecutor commandExecutor = new DialogueCommandExecutor(this);

        if (getCommand("dialogue") != null)
            getCommand("dialogue").setExecutor(commandExecutor);

        if (getCommand("dia") != null)
            getCommand("dia").setExecutor(commandExecutor);

        if (getCommand("balance") != null)
            getCommand("balance").setExecutor(commandExecutor);

        getLogger().info("DialoguesEconomy enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("DialoguesEconomy disabled!");
    }

    // ==========================================================
    // Vault Economy Setup
    // ==========================================================
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;

        econ = rsp.getProvider();
        if (econ != null)
            getLogger().info("Vault Economy hooked successfully.");
        else
            getLogger().warning("Vault Economy not found.");

        return econ != null;
    }

    // ==========================================================
    // Getter methods
    // ==========================================================
    public static DialoguesEconomy getInstance() { return instance; }
    public static Economy getEconomy() { return econ; }
    public FileConfiguration getMainConfig() { return config; }
    public boolean isPlaceholderApiEnabled() { return placeholderApiEnabled; }
    public Map<UUID, DialogueState> getActiveDialogues() { return activeDialogues; }
    public File getDialoguesFolder() { return dialoguesFolder; }

    // ==========================================================
    // เริ่มบทสนทนา (Initiate Dialogue)
    // ==========================================================
    public void initiateDialogue(CommandSender caller, Player target, String filename, String section) {
        File file = new File(dialoguesFolder, filename);
        if (!file.exists()) {
            caller.sendMessage(ChatColor.RED + "Dialogue file not found: " + filename + " in dialogues/ folder.");
            return;
        }

        FileConfiguration dialogueConfig = YamlConfiguration.loadConfiguration(file);

        // รองรับทั้งแบบ "start" และ "dialogues.start"
        String[] possiblePaths = { section, "dialogues." + section };
        boolean found = false;
        String validPath = section;

        for (String path : possiblePaths) {
            if (dialogueConfig.isConfigurationSection(path)) {
                found = true;
                validPath = path;
                break;
            }
        }

        if (!found) {
            caller.sendMessage(ChatColor.RED + "Dialogue section '" + section + "' not found in " + filename);
            return;
        }

        // สร้างสถานะบทสนทนาใหม่
        DialogueState state = new DialogueState(file, dialogueConfig, validPath);
        activeDialogues.put(target.getUniqueId(), state);

        // เริ่ม Dialogue Runner
        new DialogueRunner(this, target, state).runTask(this);

        // แจ้งผู้เรียกคำสั่ง (เฉพาะกรณีที่ไม่ใช่ผู้เล่นคนเดียวกับเป้าหมาย)
        if (caller instanceof Player && !caller.equals(target)) {
            String startMsg = config.getString("messages.dialogue-started-target",
                    "&aDialogue started for %player_name%.");
            caller.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    startMsg.replace("%player_name%", target.getName())));
        }
    }
}