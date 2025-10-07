package com.nonkungch.dialogueseconomy;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DialogueChatListener implements Listener {

    private final DialoguesEconomy plugin;

    public DialogueChatListener(DialoguesEconomy plugin) {
        this.plugin = plugin;
    }

    // EventPriority.LOWEST เพื่อให้มั่นใจว่าเราดักจับ Chat ก่อน Listener อื่นๆ และยกเลิกได้
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Map<UUID, ConfigurationSection> chatAwait = plugin.getChatAwait();

        // 1. ตรวจสอบว่าผู้เล่นกำลังรอการตอบกลับทางแชทหรือไม่
        if (chatAwait.containsKey(player.getUniqueId())) {

            // ยกเลิกข้อความแชทเพื่อไม่ให้แสดงในช่องแชทปกติ
            event.setCancelled(true);

            String playerMessage = event.getMessage().trim();
            ConfigurationSection chatOptions = chatAwait.get(player.getUniqueId());
            
            // ย้ายไปทำงานใน Main Thread เพราะเราจะแก้ไขสถานะของ Dialogue และรันคำสั่ง Bukkit
            new BukkitRunnable() {
                @Override
                public void run() {
                    
                    String selectedKey = null;

                    // 2. ตรวจสอบว่าข้อความตรงกับตัวเลือกที่กำหนดหรือไม่
                    if (chatOptions.contains(playerMessage)) {
                        selectedKey = playerMessage;
                    } 
                    // ตรวจสอบตัวเลือกโดยตัด Color Code ออก
                    else if (chatOptions.contains(ChatColor.stripColor(playerMessage))) {
                        selectedKey = ChatColor.stripColor(playerMessage);
                    }

                    if (selectedKey != null) {
                        
                        List<?> actionsList = chatOptions.getList(selectedKey);
                        DialogueState mainState = plugin.getActiveDialogues().get(player.getUniqueId());

                        if (mainState == null) {
                            player.sendMessage(ChatColor.RED + "Error: Dialogue session expired.");
                            chatAwait.remove(player.getUniqueId());
                            return;
                        }

                        // ลบผู้เล่นออกจากสถานะรอแชท
                        chatAwait.remove(player.getUniqueId());
                        
                        // [สำคัญ] เลื่อน Line index เพื่อให้ Runner ข้ามบรรทัด 'chat' ไป
                        mainState.incrementLine();
                        
                        // ประมวลผล Action ที่เลือก และรัน DialogueRunner ต่อ
                        plugin.processChatActions(player, mainState, actionsList);
                        
                    } else {
                        // 3.b ไม่มีตัวเลือกที่ตรงกัน
                        String invalidMsg = plugin.replacePlaceholders(player, 
                            plugin.getMainConfig().getString("messages.invalid-chat-response", "&cกรุณาพิมพ์ตัวเลือกให้ถูกต้อง.")
                        );
                        player.sendMessage(invalidMsg);
                        // ยังคงรอการตอบกลับ
                    }
                }
            }.runTask(plugin);
        }
    }
}