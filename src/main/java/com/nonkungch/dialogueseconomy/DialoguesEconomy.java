package com.nonkungch.dialogueseconomy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import net.milkbowl.vault.economy.Economy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class DialoguesEconomy extends JavaPlugin {

    private static DialoguesEconomy instance;
    private FileConfiguration config;
    
    // โฟลเดอร์เก็บบทสนทนา
    private final File dialoguesFolder = new File(getDataFolder(), "dialogues");

    // แผนที่เก็บสถานะบทสนทนา (ผู้เล่นที่กำลังอยู่ใน dialogue)
    private final Map<UUID, DialogueState> activeDialogues = new HashMap<>();
    
    // [ใหม่] แผนที่สำหรับผู้เล่นที่กำลังรอการตอบกลับทางแชท (UUID ของผู้เล่น -> ConfigurationSection ของตัวเลือก)
    private final Map<UUID, ConfigurationSection> chatAwait = new HashMap<>();

    private boolean placeholderApiEnabled = false;
    private static Economy econ = null;

    @Override
    public void onEnable() {
        instance = this;

        // === สร้างไฟล์ config และโฟลเดอร์ที่จำเป็น ===
        saveDefaultConfig();
        config = getConfig();
        if (!dialoguesFolder.exists()) dialoguesFolder.mkdirs();

        // [ใหม่] สร้างไฟล์ตัวอย่าง Dialogue
        saveDefaultDialogue("example.yml");
        saveDefaultDialogue("1.yml");

        // === ตรวจสอบการเชื่อมต่อกับ Vault Economy ===
        setupEconomy();

        // === ตรวจสอบ PlaceholderAPI ===
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderApiEnabled = true;
            getLogger().info("PlaceholderAPI integration enabled.");
        }

        // [ใหม่] ลงทะเบียน Event Listener สำหรับการแชท
        Bukkit.getPluginManager().registerEvents(new DialogueChatListener(this), this);

        // === ลงทะเบียน Command Executor ===
        getCommand("dialogue").setExecutor(new DialogueCommandExecutor(this));
        getCommand("dia").setExecutor(new DialogueCommandExecutor(this));
        if (getCommand("balance") != null) getCommand("balance").setExecutor(new DialogueCommandExecutor(this));
    }

    @Override
    public void onDisable() {
        // ...
    }
    
    // [ใหม่] เมธอดสำหรับบันทึกไฟล์ Dialogue ตัวอย่างจาก resources
    private void saveDefaultDialogue(String filename) {
        File file = new File(dialoguesFolder, filename);
        if (!file.exists()) {
            try (InputStream is = getResource(filename)) {
                if (is != null) {
                    Files.copy(is, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    getLogger().info("Successfully created default dialogue file: " + filename);
                } else {
                    getLogger().warning("Could not find default dialogue file in resources: " + filename);
                }
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "Could not save default dialogue file: " + filename, e);
            }
        }
    }

    public static DialoguesEconomy getInstance() {
        return instance;
    }

    public FileConfiguration getMainConfig() {
        return config;
    }

    // เมธอด getActiveDialogues เดิม
    public Map<UUID, DialogueState> getActiveDialogues() {
        return activeDialogues;
    }

    // [ใหม่] Getter สำหรับ chatAwait
    public Map<UUID, ConfigurationSection> getChatAwait() {
        return chatAwait;
    }
    
    // [ใหม่] เมธอดสำหรับประมวลผลรายการ Action (เช่น say/cmd) ที่ผู้เล่นเลือกผ่านแชท
    @SuppressWarnings("unchecked")
    public void processChatActions(Player player, DialogueState mainState, List<?> actionsList) {
        if (actionsList == null || actionsList.isEmpty()) {
            new DialogueRunner(this, player, mainState).runTaskLater(this, 1L);
            return;
        }

        for (Object actionObj : actionsList) {
            if (!(actionObj instanceof Map)) continue;

            Map<String, Object> actionMap = (Map<String, Object>) actionObj;
            
            YamlConfiguration tempActionConfig = new YamlConfiguration();
            actionMap.forEach(tempActionConfig::set);

            // 1. Action: say
            if (tempActionConfig.isSet("say")) {
                String content = replacePlaceholders(player, tempActionConfig.getString("say", ""));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', content));
            } 
            // 2. Action: cmd
            else if (tempActionConfig.isSet("cmd")) {
                String content = replacePlaceholders(player, tempActionConfig.getString("cmd", ""));
                
                String processedCommand = content.replace("@p", player.getName());
                Bukkit.getScheduler().runTask(this, () -> {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
                });
            }
        }

        // รัน DialogueRunner ต่อจากบรรทัดที่ 'chat' หยุดไว้
        new DialogueRunner(this, player, mainState).runTaskLater(this, 1L);
    }
    
    // เมธอดสำหรับแทนที่ Placeholder และ Color Code
    public String replacePlaceholders(Player player, String text) {
        String processedText = text;
        
        if (placeholderApiEnabled && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                 processedText = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, processedText);
            } catch (NoClassDefFoundError | NoSuchMethodError ignored) {
                 // ignore
            }
        }
        
        if (player != null) {
            processedText = processedText.replace("%player_name%", player.getName());
        }

        return ChatColor.translateAlternateColorCodes('&', processedText);
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }
    
    public static Economy getEconomy() {
        return econ;
    }

    public File getDialoguesFolder() {
        return dialoguesFolder;
    }
    
    // เมธอด initiateDialogue เดิม + เพิ่ม Logic ล้าง chatAwait และปรับปรุงการค้นหา Section
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
            // ใช้ dialogueConfig.get(path) != null เพื่อรองรับ List of Map ที่ระดับบนสุด
            if (dialogueConfig.get(path) != null) { 
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
        
        // [สำคัญ] ล้างสถานะรอแชท หากผู้เล่นเริ่ม dialogue ใหม่ทับของเดิม
        chatAwait.remove(target.getUniqueId());

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