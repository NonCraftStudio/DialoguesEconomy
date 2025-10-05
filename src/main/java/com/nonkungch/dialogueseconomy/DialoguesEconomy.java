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
import java.util.logging.Level;

public class DialoguesEconomy extends JavaPlugin {

    private static DialoguesEconomy instance;
    private FileConfiguration config;
    // โฟลเดอร์สำหรับเก็บไฟล์ Dialogue
    private final File dialoguesFolder = new File(getDataFolder(), "dialogues");
    
    // แผนที่เก็บสถานะบทสนทนาที่กำลังใช้งานอยู่ (Key: Player UUID)
    private final Map<UUID, DialogueState> activeDialogues = new HashMap<>();
    private boolean placeholderApiEnabled = false;
    private static Economy econ = null;

    @Override
    public void onEnable() {
        instance = this;
        
        // 1. Setup Files and Folders
        this.saveDefaultConfig();
        config = getConfig();
        if (!dialoguesFolder.exists()) dialoguesFolder.mkdirs(); 
        
        // 2. Setup Dependencies (Vault and PlaceholderAPI)
        if (!setupEconomy() ) {
            getLogger().warning(String.format("[%s] - Vault Economy not found. Economy features will be disabled.", getDescription().getName()));
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderApiEnabled = true;
            getLogger().info("PlaceholderAPI found and enabled.");
        } else {
            getLogger().warning("PlaceholderAPI not found. Dialogue placeholders will not work.");
        }
        
        // 3. Register Commands (ใช้ Executor ตัวเดียวกันสำหรับทุกคำสั่งที่เกี่ยวข้องกับ /dialogue)
        DialogueCommandExecutor commandExecutor = new DialogueCommandExecutor(this);
        this.getCommand("dialogue").setExecutor(commandExecutor);
        this.getCommand("dia").setExecutor(commandExecutor); // Alias
        this.getCommand("balance").setExecutor(commandExecutor); 
        
        getLogger().info("DialoguesEconomy enabled!");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("DialoguesEconomy disabled!");
    }

    // --- Economy Setup ---
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        econ = rsp.getProvider();
        return econ != null;
    }

    // =========================================================================
    // API Getters (เพื่อให้ Class อื่นเข้าถึงข้อมูลหลักได้)
    // =========================================================================
    
    public static Economy getEconomy() { return econ; }
    public static DialoguesEconomy getInstance() { return instance; }
    public FileConfiguration getMainConfig() { return config; }
    public boolean isPlaceholderApiEnabled() { return placeholderApiEnabled; }
    public Map<UUID, DialogueState> getActiveDialogues() { return activeDialogues; }
    public File getDialoguesFolder() { return dialoguesFolder; }

    // =========================================================================
    // Core API: Initiate Dialogue
    // =========================================================================
    
    public void initiateDialogue(CommandSender caller, Player target, String filename, String section) {
        File file = new File(dialoguesFolder, filename);
        if (!file.exists()) {
            caller.sendMessage(ChatColor.RED + "Dialogue file not found: " + filename + " in dialogues/ folder.");
            return;
        }

        FileConfiguration dialogueConfig = YamlConfiguration.loadConfiguration(file);
        
        // ตรวจสอบว่า Section เริ่มต้นมีอยู่จริงหรือไม่
        if (!dialogueConfig.isConfigurationSection(section)) {
            caller.sendMessage(ChatColor.RED + "Dialogue section '" + section + "' not found in " + filename);
            return;
        }
        
        DialogueState state = new DialogueState(file, dialogueConfig, section);
        activeDialogues.put(target.getUniqueId(), state);
        
        // เริ่ม Runner (ส่งตัว plugin instance ไปด้วย)
        new DialogueRunner(this, target, state).runTask(this);
        
        // ส่งข้อความแจ้งผู้สั่ง (ถ้ามี)
        if (caller instanceof Player && !caller.equals(target)) {
            String startMsg = config.getString("messages.dialogue-started-target", "&aDialogue started for %player_name%.");
            caller.sendMessage(ChatColor.translateAlternateColorCodes('&', startMsg.replace("%player_name%", target.getName())));
        }
    }
}