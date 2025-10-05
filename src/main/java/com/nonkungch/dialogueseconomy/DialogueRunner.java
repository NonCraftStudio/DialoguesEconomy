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

// Imports สำหรับ BungeeChat API
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.ChatMessageType;

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
        String finalLine = lineText.replace("%player_name%", target.getName());
        if (plugin.isPlaceholderApiEnabled()) {
            finalLine = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(target, finalLine);
        }

        // --- 2. การจัดการประเภทคำสั่ง (Type Handling) ---
        switch (type.toLowerCase()) {
            case "text":
                sendLine(target, finalLine, lineConfig.getString("display", "chat"), lineConfig.getString("npc", null));
                state.incrementLine();
                break;

            case "command":
                String commandStr = lineConfig.getString("command", finalLine.replace("%player_name%", target.getName()));
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandStr);
                state.incrementLine();
                break;

            case "choice":
                sendChoice(target, finalLine, lineConfig.getString("action", ""));
                this.cancel(); // หยุด Runner รอการตอบสนองจากผู้เล่น
                return;

            case "end":
                endDialogue(lineConfig.getString("message", "messages.dialogue-ended"));
                return;

            case "check_money":
                handleCheckMoney(lineConfig);
                break;

            case "take_money":
                handleTakeMoney(lineConfig);
                break;

            case "give_item":
                handleGiveItem(lineConfig);
                break;

            case "take_item":
                handleTakeItem(lineConfig);
                break;

            default:
                plugin.getLogger().warning("Unknown dialogue type: " + type);
                state.incrementLine();
                break;
        }

        // Run Next Line
        new DialogueRunner(plugin, target, state).runTaskLater(plugin, delay);
    }
    
    // --- Private Handlers for Clarity ---
    
    private void handleCheckMoney(ConfigurationSection lineConfig) {
        double requiredMoney = lineConfig.getDouble("amount", 0.0);
        if (DialoguesEconomy.getEconomy() != null) {
            double playerBalance = DialoguesEconomy.getEconomy().getBalance(target);
            if (playerBalance < requiredMoney) {
                String failSection = lineConfig.getString("fail_goto");
                if (failSection != null) {
                    state.setCurrentSection(failSection);
                    return; // ไม่ increment line แต่เปลี่ยน Section
                }
            }
        } else {
            plugin.getLogger().warning("Vault Economy is not setup. Skipping money check.");
        }
        state.incrementLine();
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
        state.incrementLine();
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
        state.incrementLine();
    }
    
    private void handleTakeItem(ConfigurationSection lineConfig) {
        String takeItemType = lineConfig.getString("item");
        int takeItemAmount = lineConfig.getInt("amount", 1);

        try {
            Material material = Material.valueOf(takeItemType.toUpperCase());
            int removedCount = removeItemFromInventory(target, material, takeItemAmount);

            String prefix = plugin.getMainConfig().getString("messages.chat-prefix", "&6[&bDialogue&6]");
            if (removedCount < takeItemAmount) {
                target.sendMessage(ChatColor.RED + prefix + " &cไม่สามารถยึด Item ได้ครบ! Dialogue จบลง");
                endDialogue("messages.dialogue-ended");
                return;
            }
            target.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + " &cยึด " + removedCount + "x " + material.name() + " แล้ว"));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().log(Level.WARNING, "Invalid material: " + takeItemType);
        }
        state.incrementLine();
    }

    private int removeItemFromInventory(Player player, Material material, int amount) {
        // (Logic การยึด Item เหมือนเดิม)
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
        this.cancel();
    }

    private void sendLine(Player player, String line, String displayMode, String npcName) {
        String translatedLine = ChatColor.translateAlternateColorCodes('&', line);
        String prefix = plugin.getMainConfig().getString("messages.chat-prefix", "&6[&bDialogue&6]");

        if (npcName != null) {
            // ใช้ NPC Name แทน prefix config
            translatedLine = ChatColor.translateAlternateColorCodes('&', "&6[" + npcName + "]&r " + line);
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

    private void sendChoice(Player player, String text, String action) {
        DialogueState state = plugin.getActiveDialogues().get(player.getUniqueId());
        if (state == null) return;

        TextComponent choiceComponent = new TextComponent(ChatColor.translateAlternateColorCodes('&', text));

        // Choice จะใช้คำสั่งภายใน /dialogue click เพื่อให้ผู้เล่นเลือก
        if (action.startsWith("goto:")) {
            String targetSection = action.substring(5);

            String secretCommand = String.format("/dialogue click %s %s %s",
                    player.getName(),
                    state.getDialogueFileName(), // ใช้ชื่อไฟล์จาก State
                    targetSection
            );

            choiceComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, secretCommand));

            // แก้ HoverEvent ให้ใช้ Text API ใหม่
            choiceComponent.setHoverEvent(new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    new Text(ChatColor.GREEN + "Click to choose: " + targetSection)
            ));

            player.spigot().sendMessage(choiceComponent);
        } else {
            player.sendMessage(ChatColor.RED + "Error in choice action: " + action);
        }
    }
}