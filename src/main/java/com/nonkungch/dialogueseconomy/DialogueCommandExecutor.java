package com.nonkungch.dialogueseconomy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

public class DialogueCommandExecutor implements CommandExecutor {

    private final DialoguesEconomy plugin;

    public DialogueCommandExecutor(DialoguesEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // จัดการกับคำสั่ง /balance หรือ /dia balance
        if (!command.getName().equalsIgnoreCase("dialogue") && !command.getName().equalsIgnoreCase("dia")) {
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
            case "create":
                handleCreate(sender, args);
                break;
            case "reload":
                handleReload(sender, args);
                break;
            case "list":
                handleList(sender);
                break;
            case "balance":
                // อนุญาตให้ใช้ /dialogue balance ได้ด้วย
                if (sender instanceof Player) {
                    handleBalance((Player) sender);
                } else {
                    sender.sendMessage(ChatColor.RED + "This command can only be used by a player.");
                }
                break;
            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    /**
     * [ใหม่] เมธอดสำหรับแสดงรายการไฟล์ dialogue
     */
    private void handleList(CommandSender sender) {
        if (!sender.hasPermission("dialogue.list")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return;
        }

        File dialoguesDir = plugin.getDialoguesFolder();
        // กรองเฉพาะไฟล์ .yml
        File[] files = dialoguesDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));

        sender.sendMessage(ChatColor.GOLD + "--- Dialogues Files (" + dialoguesDir.getName() + ") ---");

        if (files == null || files.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "No dialogue files found (.yml) in " + dialoguesDir.getName() + " folder.");
            return;
        }

        // เรียงลำดับชื่อไฟล์และรวมเป็น String เดียวกัน
        String fileList = Arrays.stream(files)
                                .map(File::getName)
                                .sorted()
                                .collect(Collectors.joining(ChatColor.GRAY + ", " + ChatColor.GREEN));

        sender.sendMessage(ChatColor.GREEN + fileList);
    }
    
    private void handleStart(CommandSender sender, String[] args) {
        if (!sender.hasPermission("dialogue.start")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return;
        }
        
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /dialogue start <player> <file> [section]");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player '" + args[1] + "' not found.");
            return;
        }

        String filename = args[2];
        // กำหนดให้ section เป็น "start" หากไม่ได้ระบุ
        String section = args.length > 3 ? args[3] : "start";

        plugin.initiateDialogue(sender, target, filename, section);
    }

    private void handleStop(CommandSender sender, String[] args) {
        if (!sender.hasPermission("dialogue.stop")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /dialogue stop <player>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player '" + args[1] + "' not found.");
            return;
        }

        if (plugin.getActiveDialogues().containsKey(target.getUniqueId())) {
            // ลบทั้งสถานะ dialogue และสถานะรอแชท
            plugin.getActiveDialogues().remove(target.getUniqueId());
            plugin.getChatAwait().remove(target.getUniqueId()); 
            target.sendMessage(ChatColor.YELLOW + "Dialogue has been forcefully stopped.");
            sender.sendMessage(ChatColor.GREEN + "Dialogue for " + target.getName() + " stopped.");
        } else {
            sender.sendMessage(ChatColor.YELLOW + target.getName() + " is not currently in a dialogue.");
        }
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("dialogue.create")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /dialogue create <file>");
            return;
        }

        String filename = args[1].toLowerCase().endsWith(".yml") ? args[1] : args[1] + ".yml";
        File file = new File(plugin.getDialoguesFolder(), filename);

        if (file.exists()) {
            sender.sendMessage(ChatColor.YELLOW + "Dialogue file '" + filename + "' already exists.");
            return;
        }

        try {
            if (file.createNewFile()) {
                // เขียนเนื้อหาเริ่มต้นลงในไฟล์ (ใช้รูปแบบ List of Map ที่เราแก้ไขใน DialogueState ให้รองรับแล้ว)
                YamlConfiguration config = new YamlConfiguration();
                config.set("start.0.say", "§eNPC: §fสวัสดี! นี่คือไฟล์ใหม่.");
                config.set("start.1.end", "");
                config.save(file);

                sender.sendMessage(ChatColor.GREEN + "Dialogue file '" + filename + "' created successfully with default 'start' section.");
            } else {
                sender.sendMessage(ChatColor.RED + "Failed to create dialogue file '" + filename + "'.");
            }
        } catch (IOException e) {
            sender.sendMessage(ChatColor.RED + "An error occurred while creating the file.");
            e.printStackTrace();
        }
    }

    private void handleReload(CommandSender sender, String[] args) {
        if (!sender.hasPermission("dialogue.reload")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return;
        }
        
        plugin.reloadConfig();
        // ไม่จำเป็นต้อง Reload dialogues files เพราะถูกโหลดใหม่เมื่อมีการเรียกใช้คำสั่ง start

        sender.sendMessage(ChatColor.GREEN + plugin.getName() + " configuration reloaded.");
    }

    private void handleInternalClick(CommandSender sender, String[] args) {
        // [คำสั่งภายใน] ใช้เมื่อผู้เล่นคลิกตัวเลือกในแชท (Choice)
        if (args.length < 4) return;
        if (!(sender instanceof Player)) return; 
        
        Player target = (Player) sender;
        
        String playerName = args[1];
        if (!target.getName().equals(playerName)) return;

        String filename = args[2];
        String nextSection = args[3];

        DialogueState currentState = plugin.getActiveDialogues().get(target.getUniqueId());
        
        // ตรวจสอบว่า Dialogue session ยังไม่หมดอายุ และตรงกับไฟล์ที่กำลังรันอยู่หรือไม่
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
        sender.sendMessage(ChatColor.GOLD + "--- DialoguesEconomy Commands ---\n");
        sender.sendMessage(ChatColor.YELLOW + "/dialogue start <player> <file> [section] §f- Start a dialogue.");
        sender.sendMessage(ChatColor.YELLOW + "/dialogue stop <player> §f- Force stop a dialogue.");
        sender.sendMessage(ChatColor.YELLOW + "/dialogue create <file> §f- Create a new dialogue file.");
        sender.sendMessage(ChatColor.YELLOW + "/dialogue list §f- List all dialogue files.");
        sender.sendMessage(ChatColor.YELLOW + "/dialogue reload §f- Reload plugin config.");
        sender.sendMessage(ChatColor.YELLOW + "/balance (or /dia balance) §f- Check player balance (Requires Vault).");
        sender.sendMessage(ChatColor.GOLD + "--- DialoguesEconomy Commands ---\n");
        sender.sendMessage(ChatColor.YELLOW + "/dia start <player> <file> [section] §f- Start a dialogue.");
        sender.sendMessage(ChatColor.YELLOW + "/dia stop <player> §f- Force stop a dialogue.");
        sender.sendMessage(ChatColor.YELLOW + "/dia create <file> §f- Create a new dialogue file.");
        sender.sendMessage(ChatColor.YELLOW + "/dia list §f- List all dialogue files.");
        sender.sendMessage(ChatColor.YELLOW + "/dia reload §f- Reload plugin config.");
        sender.sendMessage(ChatColor.GOLD + "--- DialoguesEconomy Commands ---\n");
    }
}