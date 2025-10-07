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

        // [รองรับคำสั่งย่อ 'dia' และคำสั่ง 'dialogue']
        if (!command.getName().equalsIgnoreCase("dialogue") && !command.getName().equalsIgnoreCase("dia")) {
            // กรณีเป็นคำสั่ง /balance ที่แยกออกมา
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
            // [สำคัญ] ลบ case "click" ออกไป เพราะใช้ DialogueChatListener จัดการแทน
            case "stop":
                handleStop(sender, args);
                break;
            case "reload":
                handleReload(sender);
                break;
            case "create":
                handleCreate(sender, args);
                break;
            case "list":
                handleList(sender);
                break;
            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    // --- START DIALOGUE ---
    private void handleStart(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /dia start <player> <file>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        String filename = args[2];
        
        if (target == null || !target.isOnline()) {
            sender.sendMessage(ChatColor.RED + "Player '" + args[1] + "' not found or offline.");
            return;
        }

        // initiateDialogue จะล้างสถานะเก่าเองก่อนเริ่ม
        plugin.initiateDialogue(sender, target, filename, "start");
    }

    // --- STOP DIALOGUE ---
    private void handleStop(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /dia stop <player>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null || !target.isOnline()) {
            sender.sendMessage(ChatColor.RED + "Player '" + args[1] + "' not found or offline.");
            return;
        }

        if (plugin.getActiveDialogues().remove(target.getUniqueId()) != null) {
            // [สำคัญ] ล้างสถานะรอแชท เมื่อสั่งหยุดด้วยคำสั่ง
            plugin.getChatAwait().remove(target.getUniqueId());
            String endMsg = plugin.getMainConfig().getString("messages.dialogue-ended-admin", "&aDialogue with %player_name% forcefully stopped.");
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', endMsg.replace("%player_name%", target.getName())));
            target.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMainConfig().getString("messages.dialogue-ended", "&aDialogue ended.")));
        } else {
            sender.sendMessage(ChatColor.YELLOW + target.getName() + " is not currently in a dialogue.");
        }
    }

    // --- RELOAD CONFIG ---
    private void handleReload(CommandSender sender) {
        plugin.reloadConfig();
        sender.sendMessage(ChatColor.GREEN + "DialoguesEconomy config reloaded!");
    }

    // --- CREATE DIALOGUE FILE ---
    private void handleCreate(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /dia create <filename>");
            return;
        }

        String filename = args[1];
        if (!filename.endsWith(".yml")) filename += ".yml";

        File targetFile = new File(plugin.getDialoguesFolder(), filename);

        if (targetFile.exists()) {
            sender.sendMessage(ChatColor.RED + "Dialogue file '" + filename + "' already exists!");
            return;
        }

        // พยายามคัดลอกไฟล์ template จาก resources 
        try (InputStream is = plugin.getResource("dialogue_template.yml")) {
            if (is == null) {
                // ถ้าไม่มี template ให้สร้างไฟล์เปล่า
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
    
    // --- LIST DIALOGUE FILES ---
    private void handleList(CommandSender sender) {
        File folder = plugin.getDialoguesFolder();
        // กรองเฉพาะไฟล์ที่ลงท้ายด้วย .yml
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));

        if (files == null || files.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "No dialogue files found in the dialogues/ folder.");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "--- Available Dialogue Files (.yml) ---");
        for (File file : files) {
            sender.sendMessage(ChatColor.YELLOW + "- " + file.getName());
        }
    }


    // --- CHECK BALANCE (/balance) ---
    private void handleBalance(Player player) {
        if (DialoguesEconomy.getEconomy() != null) {
            String balanceStr = String.format("%.2f", DialoguesEconomy.getEconomy().getBalance(player));
            String currency = DialoguesEconomy.getEconomy().currencyNamePlural();
            player.sendMessage(ChatColor.GOLD + "Your balance: " + balanceStr + " " + currency);
        } else {
            player.sendMessage(ChatColor.RED + "Economy system is not available. Install Vault and an economy plugin.");
        }
    }

    // --- HELP MESSAGE ---
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "--- DialoguesEconomy Commands ALL /dia ---");
        sender.sendMessage(ChatColor.YELLOW + "/dia start <player> <file>");
        sender.sendMessage(ChatColor.YELLOW + "/dia stop <player>");
        sender.sendMessage(ChatColor.YELLOW + "/dia list");
        sender.sendMessage(ChatColor.YELLOW + "/dia create <file>");
        sender.sendMessage(ChatColor.YELLOW + "/dia reload");
        sender.sendMessage(ChatColor.YELLOW + "/balance (or /dia balance)");
        sender.sendMessage(ChatColor.GOLD + "--- DialoguesEconomy Commands ALL /dialogue ---");
        sender.sendMessage(ChatColor.YELLOW + "/dialogue start <player> <file>");
        sender.sendMessage(ChatColor.YELLOW + "/dialogue stop <player>");
        sender.sendMessage(ChatColor.YELLOW + "/dialogue list");
        sender.sendMessage(ChatColor.YELLOW + "/dialogue create <file>");
        sender.sendMessage(ChatColor.YELLOW + "/dialogue reload");
        sender.sendMessage(ChatColor.GOLD + "------ END ------");
    }
}