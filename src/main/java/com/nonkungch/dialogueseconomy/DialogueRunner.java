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

// Imports สำหรับ BungeeChat API (สมมติว่ามีการใช้สำหรับตัวเลือกแบบคลิก)
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

        // อ่านค่า delay เริ่มต้น
        int delay = lineConfig.getInt("delay", plugin.getMainConfig().getInt("settings.default-delay", 40));

        // [ใหม่] ตรวจสอบ Action Types: say, cmd, chat
        if (lineConfig.isSet("say")) {
            String content = lineConfig.getString("say", "");
            sendLine(target, content);
            state.incrementLine();
            this.runTaskLater(plugin, delay);
            
        } else if (lineConfig.isSet("cmd")) {
            String content = lineConfig.getString("cmd", "");
            executeCommand(target, content);
            state.incrementLine();
            this.runTaskLater(plugin, delay);
            
        } else if (lineConfig.isConfigurationSection("chat")) {
            // [ใหม่] เข้าสู่โหมดรอการพิมพ์แชท
            ConfigurationSection chatOptions = lineConfig.getConfigurationSection("chat");
            plugin.getChatAwait().put(target.getUniqueId(), chatOptions);
            
            // หยุด Runner, รอการตอบกลับจากผู้เล่นใน DialogueChatListener
            this.cancel(); 
            
        } 
        // โค้ดสำหรับโครงสร้างแบบเก่าและ Action อื่นๆ
        else {
            String type = lineConfig.getString("type", "text");
            String lineText = lineConfig.getString("line", ""); // สำหรับ type: text/line เก่า

            switch (type.toLowerCase()) {
                case "text":
                case "line":
                    sendLine(target, lineText);
                    state.incrementLine();
                    this.runTaskLater(plugin, delay);
                    break;

                case "choice":
                    String action = lineConfig.getString("action", "");
                    sendChoice(target, lineText, action);
                    state.incrementLine();
                    this.runTaskLater(plugin, delay);
                    break;
                
                case "goto":
                    String section = lineConfig.getString("section", "start");
                    state.setCurrentSection(section);
                    this.runTaskLater(plugin, 1L); // รันต่อทันที
                    break;
                    
                case "end":
                    endDialogue("messages.dialogue-ended");
                    break;
                
                // สามารถเพิ่ม case สำหรับ item/money/teleport/etc. ได้ที่นี่

                default:
                    plugin.getLogger().warning("Unknown dialogue line type: " + type + " in file: " + state.getDialogueFileName());
                    state.incrementLine();
                    this.runTaskLater(plugin, delay);
                    break;
            }
        }
    }

    // [ใหม่] Helper method สำหรับ execute command
    private void executeCommand(Player player, String command) {
        // ประมวลผล Placeholder และรันคำสั่งด้วย ConsoleSender
        String processedCommand = plugin.replacePlaceholders(player, command).replace("@p", player.getName());
        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
        });
    }

    private void sendLine(Player player, String rawText) {
        String finalMessage = plugin.replacePlaceholders(player, rawText);
        player.sendMessage(finalMessage);
    }
    
    private void endDialogue(String messageKey) {
        plugin.getActiveDialogues().remove(target.getUniqueId());
        plugin.getChatAwait().remove(target.getUniqueId()); // เผื่อผู้เล่นกำลังรอแชทอยู่
        String endMsg = plugin.getMainConfig().getString(messageKey, "&eDialogue ended.");
        target.sendMessage(ChatColor.translateAlternateColorCodes('&', endMsg));
        this.cancel();
    }
    
    // โค้ด playSound และ sendChoice เดิม
    private void playSound(Player player, String soundName) {
        // ... (โค้ดเดิม)
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
                    state.getDialogueFileName(),
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