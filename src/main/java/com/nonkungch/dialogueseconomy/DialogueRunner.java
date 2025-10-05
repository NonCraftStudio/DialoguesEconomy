package com.nonkungch.dialogueseconomy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import me.clip.placeholderapi.PlaceholderAPI;

import java.util.Map;

public class DialogueRunner extends BukkitRunnable {

    private final DialoguesEconomy plugin;
    private final Player target;
    private final DialogueState state;
    private final Map<java.util.UUID, DialogueState> activeDialogues;
    private final boolean placeholderApiEnabled;

    public DialogueRunner(DialoguesEconomy plugin, Player target, DialogueState state,
                          Map<java.util.UUID, DialogueState> activeDialogues, boolean placeholderApiEnabled) {
        this.plugin = plugin;
        this.target = target;
        this.state = state;
        this.activeDialogues = activeDialogues;
        this.placeholderApiEnabled = placeholderApiEnabled;
    }

    @Override
    public void run() {
        String line = state.getCurrentLine();
        if (line == null) { endDialogue(); return; }

        if (placeholderApiEnabled) line = PlaceholderAPI.setPlaceholders(target, line);

        // ส่งข้อความ
        target.sendMessage(ChatColor.translateAlternateColorCodes('&', line));

        // TODO: parse type เช่น text, command, choice, check_money, take_money, give_item, take_item, end

        state.incrementLine();

        if (!state.isFinished()) {
            new DialogueRunner(plugin, target, state, activeDialogues, placeholderApiEnabled)
                    .runTaskLater(plugin, plugin.getConfig().getInt("settings.default-delay", 20));
        } else endDialogue();
    }

    private void endDialogue() {
        activeDialogues.remove(target.getUniqueId());
        target.sendMessage(ChatColor.RED + plugin.getConfig().getString("messages.dialogue-ended"));
        this.cancel();
    }
}
