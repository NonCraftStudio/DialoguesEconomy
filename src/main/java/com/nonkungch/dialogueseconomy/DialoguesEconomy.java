package com.nonkungch.dialogueseconomy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.ChatMessageType;

public class DialoguesEconomy extends JavaPlugin {

    private FileConfiguration config;
    private final File dialoguesFolder = new File(getDataFolder(), "dialogues");
    
    // **NEW:** ตัวควบคุมสถานะบทสนทนาของผู้เล่น
    // Key: UUID ของผู้เล่น, Value: สถานะบทสนทนาปัจจุบัน
    private final Map<UUID, DialogueState> activeDialogues = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        if (!dialoguesFolder.exists()) dialoguesFolder.mkdirs(); 
        getLogger().info("DialoguesEconomy enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("DialoguesEconomy disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("dialogue")) {
            // =======================================================
            // คำสั่งลับสำหรับระบบ: /dialogue click <player> <file> <section>
            // ถูกเรียกเมื่อผู้เล่นคลิกตัวเลือก (Choice)
            // =======================================================
            if (args.length >= 4 && args[0].equalsIgnoreCase("click")) {
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) return true;
                
                String filename = args[2];
                String nextSection = args[3];
                
                // เริ่มรันบทสนทนาใหม่จาก Section ที่ผู้เล่นเลือก
                initiateDialogue(target, target, filename, nextSection);
                return true;
            }
            
            // =======================================================
            // คำสั่งทั่วไป
            // =======================================================
            if (args.length == 0) {
                // ... (แสดง Commands Help)
                sender.sendMessage(ChatColor.GREEN + "=== Dialogue Commands ===");
                sender.sendMessage("/dialogue create <filename>");
                sender.sendMessage("/dialogue run <filename> <player>");
                sender.sendMessage("/dialogue reload");
                return true;
            }

            if (args[0].equalsIgnoreCase("create")) {
                // ... (โค้ด create เหมือนเดิม)
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /dialogue create <filename>");
                    return true;
                }
                String filename = args[1] + ".yml";
                File file = new File(dialoguesFolder, filename);
                
                try {
                    if (file.createNewFile()) {
                        // เขียนโครงสร้าง YAML เริ่มต้นใหม่ให้มี List ภายใน Section
                        YamlConfiguration newConfig = YamlConfiguration.loadConfiguration(file);
                        newConfig.set("start.lines", List.of(
                            Map.of("type", "text", "line", "Hello &b%player_name%&r!"),
                            Map.of("type", "choice", "line", "&a[ Continue ]", "action", "goto:next_part")
                        ));
                        newConfig.set("next_part.lines", List.of(
                            Map.of("type", "text", "line", "This is the next part!"),
                            Map.of("type", "end")
                        ));
                        newConfig.save(file);
                        
                        sender.sendMessage(ChatColor.GREEN + "Created dialogue file: " + filename);
                    } else {
                        sender.sendMessage(ChatColor.YELLOW + "Dialogue file already exists: " + filename);
                    }
                } catch (IOException e) {
                    sender.sendMessage(ChatColor.RED + "Error creating file: " + e.getMessage());
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("run")) {
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /dialogue run <filename> <player> [section]");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found or offline!");
                    return true;
                }
                String filename = args[1] + ".yml";
                String section = (args.length > 3) ? args[3] : "start"; 
                
                initiateDialogue(sender, target, filename, section);
                return true;
            }

            if (args[0].equalsIgnoreCase("reload")) {
                reloadConfig();
                config = getConfig(); 
                sender.sendMessage(ChatColor.GREEN + "DialoguesEconomy config reloaded!");
                return true;
            }
        }

        if (command.getName().equalsIgnoreCase("balance")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                player.sendMessage(ChatColor.GOLD + "Your balance: 0 ⛃"); 
                return true;
            } else {
                sender.sendMessage(ChatColor.RED + "Only players can use the /balance command.");
                return true;
            }
        }

        return false;
    }
    
    // =============================================================
    // Dialogue Flow & Runner
    // =============================================================

    /**
     * เริ่มต้นหรือรันบทสนทนาตาม Section ที่กำหนด
     */
    private void initiateDialogue(CommandSender caller, Player target, String filename, String section) {
        File file = new File(dialoguesFolder, filename);
        if (!file.exists()) {
            caller.sendMessage(ChatColor.RED + "Dialogue file not found: " + filename);
            return;
        }

        FileConfiguration dialogueConfig = YamlConfiguration.loadConfiguration(file);
        
        // สร้างสถานะใหม่ หรือดึงสถานะเก่ามาอัพเดท Section
        DialogueState state = new DialogueState(file, dialogueConfig, section);
        activeDialogues.put(target.getUniqueId(), state);
        
        // เริ่มรัน
        runDialogue(target, state);
    }

    /**
     * เมธอดหลักที่รันบทสนทนาทีละบรรทัด (แบบ Asynchronous Loop)
     */
    private void runDialogue(Player target, DialogueState state) {
        
        new BukkitRunnable() {
            int delay = 0; // หน่วงเวลาสำหรับ Bukkit Scheduler

            @Override
            public void run() {
                ConfigurationSection lineConfig = state.getCurrentLineConfig();
                
                // ตรวจสอบว่าจบบทสนทนาหรือไม่ (ไม่มีบรรทัดถัดไป)
                if (lineConfig == null) {
                    target.sendMessage(ChatColor.GRAY + "[ Dialogue Ended ]");
                    activeDialogues.remove(target.getUniqueId());
                    this.cancel();
                    return;
                }

                String type = lineConfig.getString("type", "text");
                String lineText = lineConfig.getString("line", "");
                lineText = lineText.replace("%player_name%", target.getName());
                
                // ตั้งค่า delay เป็นค่าเริ่มต้น (ถ้ามี)
                delay = lineConfig.getInt("delay", config.getInt("settings.default-delay", 20)); // 20 ticks = 1s

                // --- การจัดการประเภทคำสั่ง (Type Handling) ---
                switch (type.toLowerCase()) {
                    case "text":
                        sendLine(target, lineText, lineConfig.getString("display", "chat"), lineConfig.getString("npc", null));
                        state.incrementLine();
                        break;
                        
                    case "command":
                        String commandStr = lineConfig.getString("command", "");
                        if (!commandStr.isEmpty()) {
                            commandStr = commandStr.replace("%player_name%", target.getName());
                            // รันคำสั่งจาก Console ทันที
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandStr);
                        }
                        state.incrementLine();
                        break;
                        
                    case "choice":
                        // **สำคัญ:** หยุดการวนลูป (cancel) และรอการตอบกลับ
                        sendChoice(target, lineText, lineConfig.getString("action", ""));
                        this.cancel(); 
                        return; 
                        
                    case "end":
                        String endMsg = lineConfig.getString("message", "Dialogue Ended.");
                        sendLine(target, endMsg, "chat", null);
                        activeDialogues.remove(target.getUniqueId());
                        this.cancel();
                        return; 
                        
                    // TODO: เพิ่ม logic สำหรับ check_money, give_item ที่นี่

                    default:
                        getLogger().warning("Unknown dialogue type: " + type);
                        state.incrementLine();
                        break;
                }

                // หน่วงเวลาและรันบรรทัดถัดไป
                Bukkit.getScheduler().runTaskLater(DialoguesEconomy.this, this, delay);
            }
        }.runTask(this); // เริ่มต้นรันทันที
    }

    /**
     * ส่งข้อความธรรมดาพร้อมรองรับ Title/ActionBar และ NPC Name
     */
    private void sendLine(Player player, String line, String displayMode, String npcName) {
        String translatedLine = ChatColor.translateAlternateColorCodes('&', line);
        
        if (npcName != null) {
            String fullLine = ChatColor.translateAlternateColorCodes('&', npcName + ": &r" + line);
            // ใช้ Action Bar สำหรับชื่อ NPC เพื่อแยกจากแชท
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
    
    /**
     * ส่งข้อความที่เป็นตัวเลือก (Choice) ให้ผู้เล่นคลิก
     */
    private void sendChoice(Player player, String text, String action) {
        DialogueState state = activeDialogues.get(player.getUniqueId());
        if (state == null) return;
        
        TextComponent choiceComponent = new TextComponent(ChatColor.translateAlternateColorCodes('&', text));
        
        if (action.startsWith("goto:")) {
            String targetSection = action.substring(5);
            
            // คำสั่งลับ: /dialogue click <player_name> <filename> <section>
            // Note: ใช้ player.getName() และ state.getDialogueFile().getName() เพื่อส่งค่า
            String secretCommand = String.format("/dialogue click %s %s %s", 
                player.getName(), 
                state.getDialogueFile().getName(), 
                targetSection
            );

            choiceComponent.setClickEvent(new ClickEvent(
                ClickEvent.Action.RUN_COMMAND, 
                secretCommand
            )); 
            choiceComponent.setHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT, 
                new ComponentBuilder(ChatColor.GREEN + "Click to choose this option.").create()
            ));
            
            player.spigot().sendMessage(choiceComponent);
        } else {
            player.sendMessage(ChatColor.RED + "Error in choice action: " + action); 
        }
    }
}