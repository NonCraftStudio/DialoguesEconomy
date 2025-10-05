package com.nonkungch.dialogueseconomy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class DialogueCommandExecutor implements CommandExecutor {

    private final DialoguesEconomy plugin;

    public DialogueCommandExecutor(DialoguesEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        
        // ตรวจสอบว่าคำสั่งคือ /dialogue หรือ /dia
        if (!command.getName().equalsIgnoreCase("dialogue") && !command.getName().equalsIgnoreCase("dia")) {
            // นี่คือคำสั่ง /balance ซึ่งเราไม่ต้องการให้รันผ่าน alias
            if (command.getName().equalsIgnoreCase("balance") && sender instanceof Player) {
                handleBalance((Player) sender);
                return true;
            }
            return false;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "start":
                handleStart(sender, args);
                break;
            case "click":
                handleInternalClick(sender, args);
                break;
            case "stop":
                handleStop(sender, args);
                break;
            case "reload":
                handleReload(sender);
                break;
            case "create":
                handleCreate(sender, args);
                break;
            default:
                sendHelp(sender);
                break;
        }

        return true;
    }
    
    // --- Handlers ---
    
    private void handleStart(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /dialogue start <player> <file>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        String filename = args[2];
        if (!filename.endsWith(".yml")) filename += ".yml";

        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player '" + args[1] + "' not found.");
            return;
        }

        // Player Busy Check
        if (plugin.getActiveDialogues().containsKey(target.getUniqueId())) {
            String busyMessage = plugin.getMainConfig().getString("messages.player-busy", "&cPlayer is already in a dialogue!");
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', busyMessage));
            return;
        }

        // เริ่มบทสนทนา (เริ่มที่ Section 'start' เสมอ)
        plugin.initiateDialogue(sender, target, filename, "start");
    }

    private void handleStop(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /dialogue stop <player>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player '" + args[1] + "' not found.");
            return;
        }

        if (plugin.getActiveDialogues().remove(target.getUniqueId()) != null) {
            String endMsg = plugin.getMainConfig().getString("messages.dialogue-ended-admin", "&aDialogue with %player_name% forcefully stopped.");
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', endMsg.replace("%player_name%", target.getName())));
            target.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMainConfig().getString("messages.dialogue-ended", "&aDialogue ended.")));
        } else {
            sender.sendMessage(ChatColor.YELLOW + target.getName() + " is not currently in a dialogue.");
        }
    }
    
    private void handleReload(CommandSender sender) {
        plugin.reloadConfig();
        // ไม่ต้องอัปเดต Command Executor เพราะเราไม่ได้เก็บตัวแปร config ไว้ในนี้
        sender.sendMessage(ChatColor.GREEN + "DialoguesEconomy config reloaded!");
    }
    
    private void handleCreate(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /dialogue create <filename>");
            return;
        }
        
        String filename = args[1];
        if (!filename.endsWith(".yml")) filename += ".yml";

        File targetFile = new File(plugin.getDialoguesFolder(), filename);
        
        if (targetFile.exists()) {
            sender.sendMessage(ChatColor.RED + "Dialogue file '" + filename + "' already exists!");
            return;
        }

        // ใช้วิธี Copy ไฟล์ Template จาก Resources (เราต้องสร้างไฟล์นี้เอง)
        try (InputStream is = plugin.getResource("dialogue_template.yml")) {
            if (is == null) {
                sender.sendMessage(ChatColor.RED + "Error: Missing dialogue_template.yml in plugin JAR.");
                // สร้างไฟล์เปล่าเป็นทางเลือกสำรอง
                targetFile.createNewFile();
                sender.sendMessage(ChatColor.YELLOW + "Created empty dialogue file: " + filename);
                return;
            }
            
            Files.copy(is, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            sender.sendMessage(ChatColor.GREEN + "Successfully created new dialogue file: " + filename);
        } catch (IOException e) {
            sender.sendMessage(ChatColor.RED + "Error creating dialogue file: " + e.getMessage());
            plugin.getLogger().severe("Could not create dialogue file: " + filename + " - " + e.getMessage());
        }
    }
    
    private void handleInternalClick(CommandSender sender, String[] args) {
        // [Logic เดิมสำหรับการคลิก] ... (ใช้ args[1] player, args[2] file, args[3] section)
        if (!(sender instanceof Player)) return; 
        
        if (args.length < 4) return; 

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null || !sender.getName().equals(args[1])) return; // ต้องเป็นผู้เล่นที่รันเอง
        
        String filename = args[2];
        String nextSection = args[3];
        
        DialogueState currentState = plugin.getActiveDialogues().get(target.getUniqueId());
        if (currentState == null || !currentState.getDialogueFileName().equalsIgnoreCase(filename)) {
            target.sendMessage(ChatColor.RED + "Dialogue session expired or invalid.");
            return;
        }
        
        // เริ่ม Dialogue ใหม่ที่ Section ที่ถูกเลือก
        plugin.initiateDialogue(target, target, filename, nextSection);
    }
    
    private void handleBalance(Player player) {
        if (DialoguesEconomy.getEconomy() != null) {
            String balanceStr = String.format("%.2f", DialoguesEconomy.getEconomy().getBalance(player));
            String currency = DialoguesEconomy.getEconomy().currencyNamePlural();
            player.sendMessage(ChatColor.GOLD + "Your balance: " + balanceStr + " " + currency);
        } else {
            player.sendMessage(ChatColor.RED + "Economy system is not available. Install Vault and an economy plugin.");
        }
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "--- DialoguesEconomy Commands ---");
        sender.sendMessage(ChatColor.YELLOW + "/dialogue start <player> <file> [section]");
        sender.sendMessage(ChatColor.YELLOW + "/dialogue stop <player>");
        sender.sendMessage(ChatColor.YELLOW + "/dialogue create <file>");
        sender.sendMessage(ChatColor.YELLOW + "/dialogue reload");
        sender.sendMessage(ChatColor.YELLOW + "/balance (or /dia balance)");
        sender.sendMessage(ChatColor.YELLOW + "/dialogue click ... (Internal)");
    }
}