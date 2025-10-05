package com.nonkungch.dialogueseconomy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.ChatMessageType;

public class DialogueRunner extends BukkitRunnable {

    private final DialoguesEconomy plugin;
    private final Player target;
    private final DialogueState state;
    private final Map<UUID, DialogueState> activeDialogues;
    private final boolean placeholderApiEnabled;

    public DialogueRunner(DialoguesEconomy plugin, Player target, DialogueState state, Map<UUID, DialogueState> activeDialogues, boolean placeholderApiEnabled) {
        this.plugin = plugin;
        this.target = target;
        this.state = state;
        this.activeDialogues = activeDialogues;
        this.placeholderApiEnabled = placeholderApiEnabled;
    }

    @Override
    public void run() {
        ConfigurationSection lineConfig = state.getCurrentLineConfig();
        
        if (lineConfig == null) {
            // จบบทสนทนาเมื่อไม่มีบรรทัดถัดไป
            endDialogue("messages.dialogue-ended");
            return;
        }
        
        String type = lineConfig.getString("type", "text");
        String lineText = lineConfig.getString("line", "");
        
        // กำหนด delay เริ่มต้น หรือดึงจาก config.yml
        int delay = lineConfig.getInt("delay", plugin.getConfig().getInt("settings.default-delay", 20));

        // --- 1. ประมวลผล Placeholder และ Color Codes ---
        String finalLine = lineText.replace("%player_name%", target.getName());
        if (placeholderApiEnabled) {
            // ใช้ Reflection หรือการ Cast ที่เหมาะสมเพื่อเรียก PlaceholderAPI
            finalLine = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(target, finalLine);
        }
        
        // --- 2. การจัดการประเภทคำสั่ง (Type Handling) ---
        switch (type.toLowerCase()) {
            case "text":
                sendLine(target, finalLine, lineConfig.getString("display", "chat"), lineConfig.getString("npc", null));
                state.incrementLine();
                break;
                
            case "command":
                String commandStr = lineConfig.getString("command", "");
                if (!commandStr.isEmpty()) {
                    commandStr = finalLine.replace("%player_name%", target.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandStr);
                }
                state.incrementLine();
                break;
                
            case "choice":
                // **หยุดการรัน (cancel) และรอการตอบกลับ**
                sendChoice(target, finalLine, lineConfig.getString("action", ""));
                this.cancel(); 
                return; 
                
            case "end":
                String endMsg = lineConfig.getString("message", plugin.getConfig().getString("messages.dialogue-ended"));
                endDialogue(endMsg);
                return; 
                
            // TODO: เพิ่ม logic สำหรับ check_money, give_item, require_item ที่นี่
                
            default:
                plugin.getLogger().warning("Unknown dialogue type: " + type + " in " + state.getDialogueFile().getName());
                state.incrementLine();
                break;
        }

        // รันงานถัดไปตาม delay ที่กำหนด
        new DialogueRunner(plugin, target, state, activeDialogues, placeholderApiEnabled).runTaskLater(plugin, delay);
    }
    
    // --- Helper Methods ---

    private void endDialogue(String endMessageConfigPath) {
        String endMsg = plugin.getConfig().getString(endMessageConfigPath);
        if (endMsg != null) target.sendMessage(ChatColor.translateAlternateColorCodes('&', endMsg));
        activeDialogues.remove(target.getUniqueId());
        this.cancel();
    }

    private void sendLine(Player player, String line, String displayMode, String npcName) {
        String translatedLine = ChatColor.translateAlternateColorCodes('&', line);
        
        if (npcName != null) {
            String fullLine = ChatColor.translateAlternateColorCodes('&', npcName + ": &r" + line);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(fullLine));
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
        
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
    }
    
    private void sendChoice(Player player, String text, String action) {
        TextComponent choiceComponent = new TextComponent(ChatColor.translateAlternateColorCodes('&', text));
        
        if (action.startsWith("goto:")) {
            String targetSection = action.substring(5);
            
            // คำสั่งลับ: /dialogue click <player_name> <filename> <section>
            String secretCommand = String.format("/dialogue click %s %s %s", 
                player.getName(), 
                state.getDialogueFile().getName(), 
                targetSection
            );

            choiceComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, secretCommand)); 
            choiceComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                new ComponentBuilder(ChatColor.GREEN + "Click to choose: " + targetSection).create()));
            
            player.spigot().sendMessage(choiceComponent);
        } else {
            player.sendMessage(ChatColor.RED + "Error in choice action: " + action); 
        }
    }
    }package com.nonkungch.dialogueseconomy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.ChatMessageType;

public class DialogueRunner extends BukkitRunnable {

    private final DialoguesEconomy plugin;
    private final Player target;
    private final DialogueState state;
    private final Map<UUID, DialogueState> activeDialogues;
    private final boolean placeholderApiEnabled;

    public DialogueRunner(DialoguesEconomy plugin, Player target, DialogueState state, Map<UUID, DialogueState> activeDialogues, boolean placeholderApiEnabled) {
        this.plugin = plugin;
        this.target = target;
        this.state = state;
        this.activeDialogues = activeDialogues;
        this.placeholderApiEnabled = placeholderApiEnabled;
    }

    @Override
    public void run() {
        ConfigurationSection lineConfig = state.getCurrentLineConfig();
        
        if (lineConfig == null) {
            // จบบทสนทนาเมื่อไม่มีบรรทัดถัดไป
            endDialogue("messages.dialogue-ended");
            return;
        }
        
        String type = lineConfig.getString("type", "text");
        String lineText = lineConfig.getString("line", "");
        
        // กำหนด delay เริ่มต้น หรือดึงจาก config.yml
        int delay = lineConfig.getInt("delay", plugin.getConfig().getInt("settings.default-delay", 20));

        // --- 1. ประมวลผล Placeholder และ Color Codes ---
        String finalLine = lineText.replace("%player_name%", target.getName());
        if (placeholderApiEnabled) {
            // ใช้ Reflection หรือการ Cast ที่เหมาะสมเพื่อเรียก PlaceholderAPI
            finalLine = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(target, finalLine);
        }
        
        // --- 2. การจัดการประเภทคำสั่ง (Type Handling) ---
        switch (type.toLowerCase()) {
            case "text":
                sendLine(target, finalLine, lineConfig.getString("display", "chat"), lineConfig.getString("npc", null));
                state.incrementLine();
                break;
                
            case "command":
                String commandStr = lineConfig.getString("command", "");
                if (!commandStr.isEmpty()) {
                    commandStr = finalLine.replace("%player_name%", target.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandStr);
                }
                state.incrementLine();
                break;
                
            case "choice":
                // **หยุดการรัน (cancel) และรอการตอบกลับ**
                sendChoice(target, finalLine, lineConfig.getString("action", ""));
                this.cancel(); 
                return; 
                
            case "end":
                String endMsg = lineConfig.getString("message", plugin.getConfig().getString("messages.dialogue-ended"));
                endDialogue(endMsg);
                return; 
                
            // TODO: เพิ่ม logic สำหรับ check_money, give_item, require_item ที่นี่
                
            default:
                plugin.getLogger().warning("Unknown dialogue type: " + type + " in " + state.getDialogueFile().getName());
                state.incrementLine();
                break;
        }

        // รันงานถัดไปตาม delay ที่กำหนด
        new DialogueRunner(plugin, target, state, activeDialogues, placeholderApiEnabled).runTaskLater(plugin, delay);
    }
    
    // --- Helper Methods ---

    private void endDialogue(String endMessageConfigPath) {
        String endMsg = plugin.getConfig().getString(endMessageConfigPath);
        if (endMsg != null) target.sendMessage(ChatColor.translateAlternateColorCodes('&', endMsg));
        activeDialogues.remove(target.getUniqueId());
        this.cancel();
    }

    private void sendLine(Player player, String line, String displayMode, String npcName) {
        String translatedLine = ChatColor.translateAlternateColorCodes('&', line);
        
        if (npcName != null) {
            String fullLine = ChatColor.translateAlternateColorCodes('&', npcName + ": &r" + line);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(fullLine));
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
        
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
    }
    
    private void sendChoice(Player player, String text, String action) {
        TextComponent choiceComponent = new TextComponent(ChatColor.translateAlternateColorCodes('&', text));
        
        if (action.startsWith("goto:")) {
            String targetSection = action.substring(5);
            
            // คำสั่งลับ: /dialogue click <player_name> <filename> <section>
            String secretCommand = String.format("/dialogue click %s %s %s", 
                player.getName(), 
                state.getDialogueFile().getName(), 
                targetSection
            );

            choiceComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, secretCommand)); 
            choiceComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                new ComponentBuilder(ChatColor.GREEN + "Click to choose: " + targetSection).create()));
            
            player.spigot().sendMessage(choiceComponent);
        } else {
            player.sendMessage(ChatColor.RED + "Error in choice action: " + action); 
        }
    }
}
