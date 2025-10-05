package com.nonkungch.dialogueseconomy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.milkbowl.vault.economy.Economy; // <-- Import สำหรับ Vault

public class DialoguesEconomy extends JavaPlugin {

    private FileConfiguration config;
    private final File dialoguesFolder = new File(getDataFolder(), "dialogues");
    
    private final Map<UUID, DialogueState> activeDialogues = new HashMap<>();
    private boolean placeholderApiEnabled = false;
    
    private static Economy econ = null; // <-- ตัวแปร Vault Economy

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        if (!dialoguesFolder.exists()) dialoguesFolder.mkdirs(); 
        
        // Setup Vault Economy
        if (!setupEconomy() ) {
            getLogger().warning(String.format("[%s] - Vault Economy not found. Economy features will be disabled.", getDescription().getName()));
            // ไม่ต้อง disable plugin แต่ฟีเจอร์เงินจะถูกเตือน
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderApiEnabled = true;
            getLogger().info("PlaceholderAPI found and enabled.");
        } else {
            getLogger().warning("PlaceholderAPI not found. Dialogue placeholders will not work.");
        }
        
        getLogger().info("DialoguesEconomy enabled!");
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

    @Override
    public void onDisable() {
        getLogger().info("DialoguesEconomy disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("dialogue")) {
            
            // /dialogue click
            if (args.length >= 4 && args[0].equalsIgnoreCase("click")) {
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) return true;
                
                String filename = args[2];
                String nextSection = args[3];
                
                DialogueState currentState = activeDialogues.get(target.getUniqueId());
                if (currentState == null || !currentState.getDialogueFile().getName().equalsIgnoreCase(filename)) {
                    target.sendMessage(ChatColor.RED + "Dialogue session expired or invalid.");
                    return true;
                }
                
                initiateDialogue(target, target, filename, nextSection);
                return true;
            }
            
            // /dialogue run
            if (args.length >= 2 && args[0].equalsIgnoreCase("run")) {
                String filename = args[1] + ".yml";
                Player target = (args.length >= 3) ? Bukkit.getPlayer(args[2]) : null;
                String section = (args.length > 3) ? args[3] : "start"; 
                
                if (target == null && sender instanceof Player) {
                    target = (Player) sender;
                } else if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Usage: /dialogue run <filename> <player> [section]");
                    return true;
                }
                
                // Player Busy Check
                if (activeDialogues.containsKey(target.getUniqueId())) {
                    String busyMessage = config.getString("messages.player-busy", "&cYou are already in a dialogue!");
                    target.sendMessage(ChatColor.translateAlternateColorCodes('&', busyMessage));
                    return true;
                } 

                initiateDialogue(sender, target, filename, section);
                return true;
            } 
            
            // /dialogue reload
            if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
                reloadConfig();
                config = getConfig(); 
                sender.sendMessage(ChatColor.GREEN + "DialoguesEconomy config reloaded!");
                return true;
            }
        }

        // คำสั่ง /balance
        if (command.getName().equalsIgnoreCase("balance")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                // ใช้ Vault API จริง
                if (econ != null) {
                    String balanceStr = String.format("%.2f", econ.getBalance(player));
                    String currency = econ.currencyNamePlural();
                    player.sendMessage(ChatColor.GOLD + "Your balance: " + balanceStr + " " + currency);
                } else {
                    player.sendMessage(ChatColor.RED + "Economy system is not available. Install Vault and an economy plugin.");
                }
                return true;
            }
        }
        return false;
    }
    
    private void initiateDialogue(CommandSender caller, Player target, String filename, String section) {
        File file = new File(dialoguesFolder, filename);
        if (!file.exists()) {
            caller.sendMessage(ChatColor.RED + "Dialogue file not found: " + filename);
            return;
        }

        FileConfiguration dialogueConfig = YamlConfiguration.loadConfiguration(file);
        
        DialogueState state = new DialogueState(file, dialogueConfig, section);
        activeDialogues.put(target.getUniqueId(), state);
        
        new DialogueRunner(this, target, state, activeDialogues, placeholderApiEnabled).runTask(this);
        
        if (caller instanceof Player) {
            String startMsg = config.getString("messages.dialogue-started");
            if (startMsg != null) target.sendMessage(ChatColor.translateAlternateColorCodes('&', startMsg));
        }
    }
}
