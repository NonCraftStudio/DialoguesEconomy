package com.nonkungch.dialogueseconomy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.List;
import java.util.UUID;

public class DialogueChatListener implements Listener {

    private final DialoguesEconomy plugin;

    public DialogueChatListener(DialoguesEconomy plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        
        // 1. ตรวจสอบว่าผู้เล่นกำลังรอการตอบกลับจาก Dialogue อยู่หรือไม่
        if (!plugin.getChatAwait().containsKey(playerUUID)) {
            return; // ไม่ได้รอตอบ ก็ให้แชทตามปกติ
        }

        ConfigurationSection lineConfig = plugin.getChatAwait().get(playerUUID);
        String message = event.getMessage().trim();
        event.setCancelled(true); // [สำคัญ] ยกเลิกข้อความแชทของผู้เล่น

        // 2. ตรวจสอบว่าเป็นตัวเลขหรือไม่
        int choice;
        try {
            choice = Integer.parseInt(message);
        } catch (NumberFormatException e) {
            String errorMsg = plugin.getMainConfig().getString("messages.invalid-choice", 
                                "&cPlease enter a valid choice number, or type /dia stop.");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', errorMsg));
            return;
        }

        // 3. ประมวลผลตัวเลือก
        List<?> choicesList = lineConfig.getList("choices");
        if (choicesList == null || choice < 1 || choice > choicesList.size()) {
            String errorMsg = plugin.getMainConfig().getString("messages.invalid-choice", 
                                "&cPlease enter a valid choice number, or type /dia stop.");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', errorMsg));
            return;
        }

        // ตัวเลือกที่ผู้เล่นเลือก (เป็น String ในรูปแบบ "display | target_section")
        String selectedChoice = (String) choicesList.get(choice - 1);
        String[] parts = selectedChoice.split("\\|");
        
        if (parts.length < 2) {
            player.sendMessage(ChatColor.RED + "Error: Target section not defined for choice.");
            plugin.getChatAwait().remove(playerUUID);
            plugin.getActiveDialogues().remove(playerUUID);
            return;
        }

        String targetSection = parts[1].trim();

        // 4. ล้างสถานะรอแชทและเริ่ม Dialogue ใหม่จาก Section ที่เลือก (ต้องทำบน Main Thread)
        plugin.getChatAwait().remove(playerUUID);
        
        BukkitScheduler scheduler = Bukkit.getScheduler();
        scheduler.runTask(plugin, () -> {
            DialogueState state = plugin.getActiveDialogues().get(playerUUID);
            if (state != null) {
                String filename = state.getDialogueFileName();
                // initiateDialogue จะจัดการล้าง ActiveDialogue เก่าเอง
                plugin.initiateDialogue(player, player, filename, targetSection);
            } else {
                 player.sendMessage(ChatColor.RED + "Dialogue session was lost!");
            }
        });
    }
}