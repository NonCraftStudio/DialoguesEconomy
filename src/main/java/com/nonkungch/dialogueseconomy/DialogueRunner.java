package com.nonkungch.dialogueseconomy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

// Imports สำหรับ Action Bar และ Chat API
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.logging.Level;
import java.util.List;
import java.util.UUID;

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

        // ดึงค่า delay โดยใช้ค่า default 20 ticks ถ้าไม่ระบุ
        int delay = lineConfig.getInt("delay", plugin.getMainConfig().getInt("settings.default-delay", 20));

        // --- 1. ประมวลผล Placeholder และ Color Codes ---
        String finalLine = plugin.replacePlaceholders(target, lineText); 
        
        // ตัวแปรควบคุมการไหล (ถ้าเป็น false ให้หยุด Runner หรือเปลี่ยน Flow)
        boolean continueToNextLine = true;

        // --- 2. การจัดการประเภทคำสั่ง (Type Handling) ---
        switch (type.toLowerCase()) {
            case "text":
                sendLine(target, finalLine, lineConfig.getString("display", "chat"), lineConfig.getString("npc", null));
                break;

            case "command":
            case "console_command": // รองรับคำสั่งที่รันจาก Console
                String commandStr = lineConfig.getString("command", finalLine); 
                String processedCommand = plugin.replacePlaceholders(target, commandStr);
                
                // สั่งรันคำสั่งผ่าน ConsoleSender บน Main Thread
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
                });
                break;

            case "choice":
                sendChoices(target, lineConfig); // ใช้ระบบพิมพ์ตัวเลข
                this.cancel(); // หยุด Runner รอการตอบสนองจากผู้เล่น
                return;

            case "end":
                endDialogue(lineConfig.getString("message", "messages.dialogue-ended"));
                return;

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
        
        // --- 3. Flow Control: จัดการ goto และตัดสินใจว่าจะไปต่อหรือไม่ ---
        
        // [แก้ไขบั๊ก] ตรวจสอบคำสั่ง 'goto' ก่อนที่จะไปบรรทัดถัดไป
        String gotoSection = lineConfig.getString("goto");
        if (gotoSection != null) {
            // ไม่ต้อง increment line, แค่เปลี่ยน Section และรันทันที
            state.setCurrentSection(gotoSection);
            new DialogueRunner(plugin, target, state).runTask(plugin);
            return; 
        }
        
        // ถ้า Check/Take ล้มเหลว (continueToNextLine = false) จะหยุด
        if (!continueToNextLine) { 
            return; 
        }

        // Run Next Line
        state.incrementLine();
        new DialogueRunner(plugin, target, state).runTaskLater(plugin, delay);
    }
    
    // --- Private Handlers (ไม่มีการเปลี่ยนแปลงจากที่เคยให้ไป) ---
    
    private boolean handleCheckMoney(ConfigurationSection lineConfig) {
        double requiredMoney = lineConfig.getDouble("amount", 0.0);
        if (DialoguesEconomy.getEconomy() != null) {
            double playerBalance = DialoguesEconomy.getEconomy().getBalance(target);
            if (playerBalance < requiredMoney) {
                String failSection = lineConfig.getString("fail_goto");
                if (failSection != null) {
                    state.setCurrentSection(failSection);
                    new DialogueRunner(plugin, target, state).runTask(plugin); // รัน Section ใหม่ทันที
                    return false; // ไม่ต้องไปต่อในบรรทัดถัดไป
                }
                endDialogue("messages.dialogue-ended");
                return false;
            }
        } else {
            plugin.getLogger().warning("Vault Economy is not setup. Skipping money check.");
        }
        return true; // ไปต่อบรรทัดถัดไป
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
                String failSection = lineConfig.getString("fail_goto");
                if (failSection != null) {
                    state.setCurrentSection(failSection);
                    new DialogueRunner(plugin, target, state).runTask(plugin);
                    return false;
                }
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
        // ... (Logic การยึด Item เหมือนเดิม)
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
        plugin.getChatAwait().remove(target.getUniqueId()); // ล้างสถานะรอแชท
        this.cancel();
    }

    private void sendLine(Player player, String line, String displayMode, String npcName) {
        String prefix = plugin.getMainConfig().getString("messages.chat-prefix", "&6[&bDialogue&6]");
        String translatedLine;
        
        if (npcName != null) {
            translatedLine = ChatColor.translateAlternateColorCodes('&', "&c[" + npcName + "]&r " + line);
        } else {
            translatedLine = ChatColor.translateAlternateColorCodes('&', prefix + " " + line);
        }

        switch (displayMode.toLowerCase()) {
            case "title":
                player.sendTitle(translatedLine, "", 10, 70, 20);
                break;
            case "actionbar":
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(translatedLine));
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

    // เมธอดสำหรับแสดงตัวเลือกแบบพิมพ์ตัวเลข (ไม่ใช่ระบบคลิกเก่า)
    private void sendChoices(Player player, ConfigurationSection lineConfig) {
        if (!lineConfig.isList("choices")) {
            player.sendMessage(ChatColor.RED + "Error: 'choices' section not found for choice type.");
            endDialogue("messages.dialogue-ended");
            return;
        }

        String prompt = plugin.replacePlaceholders(player, lineConfig.getString("line", ""));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', prompt));
        player.sendMessage(ChatColor.GOLD + "--- P L E A S E  C H O O S E ---");

        List<?> choicesList = lineConfig.getList("choices");
        int index = 1;

        for (Object choiceObj : choicesList) {
            if (!(choiceObj instanceof String)) continue;
            
            String choiceStr = (String) choiceObj;
            
            // รูปแบบ: "ข้อความตัวเลือก | target_section"
            String[] parts = choiceStr.split("\\|");
            String display = (parts.length > 0 ? parts[0].trim() : "Invalid Choice");
            
            player.sendMessage(ChatColor.YELLOW + "[" + index + "] " + ChatColor.RESET + ChatColor.translateAlternateColorCodes('&', display));
            index++;
        }

        // เก็บสถานะการรอแชทไว้
        plugin.getChatAwait().put(player.getUniqueId(), lineConfig); 
    }
}