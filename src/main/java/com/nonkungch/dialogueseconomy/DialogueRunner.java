package com.nonkungch.dialogueseconomy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.logging.Level;
import java.util.List;

// [ถูกลบ]: Imports สำหรับ BungeeChat API

public class DialogueRunner extends BukkitRunnable {

    private final DialoguesEconomy plugin;
    private final Player target;
    private final DialogueState state;

    public DialogueRunner(DialoguesEconomy plugin, Player target, DialogueState state) {
        this.plugin = plugin;
        this.target = target;
        this.state = state;
    }

    @Override
    public void run() {
        ConfigurationSection lineConfig = state.getCurrentLineConfig();

        if (lineConfig == null) {
            endDialogue("messages.dialogue-ended");
            return;
        }

        String type = lineConfig.getString("type", "text");
        String lineText = lineConfig.getString("line", "");

        int delay = lineConfig.getInt("delay", plugin.getMainConfig().getInt("settings.default-delay", 20));

        // --- 1. ประมวลผล Placeholder และ Color Codes ---
        String finalLine = plugin.replacePlaceholders(target, lineText); 
        
        // ตัวแปรควบคุมการไหล (ถ้าเป็น false ให้หยุด Runner)
        boolean continueToNextLine = true;

        // --- 2. การจัดการประเภทคำสั่ง (Type Handling) ---
        switch (type.toLowerCase()) {
            case "text":
                sendLine(target, finalLine, lineConfig.getString("display", "chat"), lineConfig.getString("npc", null));
                break;

            case "command":
                String commandStr = lineConfig.getString("command", finalLine);
                String processedCommand = plugin.replacePlaceholders(target, commandStr);
                
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
                });
                break;

            case "choice":
                sendChoices(target, lineConfig); // เรียกเมธอดใหม่สำหรับพิมพ์ตัวเลข
                this.cancel(); // หยุด Runner รอการตอบสนองจากผู้เล่น
                return; // หยุดการทำงานของ run()

            case "end":
                endDialogue(lineConfig.getString("message", "messages.dialogue-ended"));
                return; // หยุดการทำงานของ run()

            case "check_money":
                continueToNextLine = handleCheckMoney(lineConfig); 
                break;

            case "take_money":
                handleTakeMoney(lineConfig);
                break;

            case "give_item":
                handleGiveItem(lineConfig);
                break;

            case "take_item":
                continueToNextLine = handleTakeItem(lineConfig);
                break;

            default:
                plugin.getLogger().warning("Unknown dialogue type: " + type);
                break;
        }
        
        // --- 3. Flow Control: ตัดสินใจว่าจะไปต่อหรือไม่ ---
        if (!continueToNextLine) {
            return; 
        }

        // Run Next Line (สำหรับคำสั่งที่ไม่ได้หยุดการทำงาน)
        state.incrementLine();
        new DialogueRunner(plugin, target, state).runTaskLater(plugin, delay);
    }
    
    // --- Private Handlers for Clarity ---
    
    private boolean handleCheckMoney(ConfigurationSection lineConfig) {
        double requiredMoney = lineConfig.getDouble("amount", 0.0);
        if (DialoguesEconomy.getEconomy() != null) {
            double playerBalance = DialoguesEconomy.getEconomy().getBalance(target);
            if (playerBalance < requiredMoney) {
                String failSection = lineConfig.getString("fail_goto");
                if (failSection != null) {
                    state.setCurrentSection(failSection);
                    new DialogueRunner(plugin, target, state).runTask(plugin);
                    return false;
                }
                endDialogue("messages.dialogue-ended");
                return false;
            }
        } else {
            plugin.getLogger().warning("Vault Economy is not setup. Skipping money check.");
        }
        return true;
    }
    
    private void handleTakeMoney(ConfigurationSection lineConfig) {
        double takeAmount = lineConfig.getDouble("amount", 0.0);
        if (DialoguesEconomy.getEconomy() != null) {
            DialoguesEconomy.getEconomy().withdrawPlayer(target, takeAmount);
            String currency = DialoguesEconomy.getEconomy().currencyNamePlural();
            String prefix = plugin.getMainConfig().getString("messages.chat-prefix", "&6[&bDialogue&6]");
            target.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + " &cหักเงิน " + takeAmount + " " + currency));
        } else {
            target.sendMessage(ChatColor.RED + "ไม่สามารถหักเงินได้: Vault Economy ไม่พร้อมใช้งาน");
        }
    }
    
    private void handleGiveItem(ConfigurationSection lineConfig) {
        String itemType = lineConfig.getString("item");
        int itemAmount = lineConfig.getInt("amount", 1);

        try {
            Material material = Material.valueOf(itemType.toUpperCase());
            ItemStack item = new ItemStack(material, itemAmount);
            target.getInventory().addItem(item);
            String prefix = plugin.getMainConfig().getString("messages.chat-prefix", "&6[&bDialogue&6]");
            target.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + " &aได้รับ " + itemAmount + "x " + material.name()));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().log(Level.WARNING, "Invalid material: " + itemType);
        }
    }
    
    private boolean handleTakeItem(ConfigurationSection lineConfig) {
        String takeItemType = lineConfig.getString("item");
        int takeItemAmount = lineConfig.getInt("amount", 1);

        try {
            Material material = Material.valueOf(takeItemType.toUpperCase());
            int removedCount = removeItemFromInventory(target, material, takeItemAmount);

            String prefix = plugin.getMainConfig().getString("messages.chat-prefix", "&6[&bDialogue&6]");
            if (removedCount < takeItemAmount) {
                target.sendMessage(ChatColor.RED + prefix + " &cไม่สามารถยึด Item ได้ครบ! Dialogue จบลง");
                endDialogue("messages.dialogue-ended");
                return false;
            }
            target.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + " &cยึด " + removedCount + "x " + material.name() + " แล้ว"));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().log(Level.WARNING, "Invalid material: " + takeItemType);
        }
        return true;
    }

    private int removeItemFromInventory(Player player, Material material, int amount) {
        int removedCount = 0;
        ItemStack[] contents = player.getInventory().getContents();

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];

            if (item != null && item.getType() == material) {
                int stackSize = item.getAmount();
                int toRemove = Math.min(amount - removedCount, stackSize);

                item.setAmount(stackSize - toRemove);
                contents[i] = item.getAmount() == 0 ? null : item;
                removedCount += toRemove;

                if (removedCount >= amount) {
                    player.getInventory().setContents(contents);
                    return removedCount;
                }
            }
        }
        player.getInventory().setContents(contents);
        return removedCount;
    }

    private void endDialogue(String endMessageConfigPath) {
        String endMsg = plugin.getMainConfig().getString(endMessageConfigPath);
        String prefix = plugin.getMainConfig().getString("messages.chat-prefix", "&6[&bDialogue&6]");

        if (endMsg != null) {
            endMsg = endMsg.replace("%chat-prefix%", prefix);
            target.sendMessage(ChatColor.translateAlternateColorCodes('&', endMsg));
        }
        plugin.getActiveDialogues().remove(target.getUniqueId());
        plugin.getChatAwait().remove(target.getUniqueId()); // ล้างสถานะรอแชทเมื่อจบ
        this.cancel();
    }

    private void sendLine(Player player, String line, String displayMode, String npcName) {
        // ... (โค้ดเดิม) ...
        String prefix = plugin.getMainConfig().getString("messages.chat-prefix", "&6[&bDialogue&6]");
        String translatedLine;
        
        if (npcName != null) {
            translatedLine = ChatColor.translateAlternateColorCodes('&', "&6[" + npcName + "]&r " + line);
        } else {
            translatedLine = ChatColor.translateAlternateColorCodes('&', prefix + " " + line);
        }

        switch (displayMode.toLowerCase()) {
            case "title":
                player.sendTitle(translatedLine, "", 10, 70, 20);
                break;
            case "actionbar":
                // ต้องใช้ BungeeChat API สำหรับ Action Bar
                player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, new net.md_5.bungee.api.chat.TextComponent(translatedLine));
                break;
            case "chat":
            default:
                player.sendMessage(translatedLine);
                break;
        }

        String soundName = plugin.getMainConfig().getString("settings.default-sound", "ENTITY_EXPERIENCE_ORB_PICKUP");
        try {
            player.playSound(player.getLocation(), Sound.valueOf(soundName.toUpperCase()), 1f, 1f);
        } catch (IllegalArgumentException e) {
            // Ignore if sound name is invalid
        }
    }

    // เมธอดใหม่: สำหรับแสดงตัวเลือกแบบพิมพ์ตัวเลข
    private void sendChoices(Player player, ConfigurationSection lineConfig) {
        if (!lineConfig.isList("choices")) {
            player.sendMessage(ChatColor.RED + "Error: 'choices' section not found for choice type.");
            endDialogue("messages.dialogue-ended");
            return;
        }

        String prompt = plugin.replacePlaceholders(player, lineConfig.getString("line", ""));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', prompt));
        player.sendMessage(ChatColor.GOLD + "--- P L E A S E  C H O O S E ---");

        int index = 1;
        List<?> choicesList = lineConfig.getList("choices");

        for (Object choiceObj : choicesList) {
            if (!(choiceObj instanceof String)) continue;
            
            String choiceStr = (String) choiceObj;
            
            // รูปแบบ: "ข้อความตัวเลือก | target_section"
            String[] parts = choiceStr.split("\\|");
            String display = (parts.length > 0 ? parts[0].trim() : "Invalid Choice");
            
            // แสดงตัวเลือกให้ผู้เล่นเห็น
            player.sendMessage(ChatColor.YELLOW + "[" + index + "] " + ChatColor.RESET + ChatColor.translateAlternateColorCodes('&', display));
            index++;
        }

        // [สำคัญ] เก็บสถานะการรอแชทไว้ที่นี่
        plugin.getChatAwait().put(player.getUniqueId(), lineConfig); 
    }
}