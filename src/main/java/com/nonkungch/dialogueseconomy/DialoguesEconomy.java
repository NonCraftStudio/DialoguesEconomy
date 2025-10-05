package com.nonkungch.dialogueseconomy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

// Imports สำหรับ BungeeChat API
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.ChatMessageType;

// Imports สำหรับ PlaceholderAPI และ Vault
import me.clip.placeholderapi.PlaceholderAPI;
import net.milkbowl.vault.economy.Economy;

public class DialoguesEconomy extends JavaPlugin {

    private static Economy economy;
    private final Map<UUID, DialogueState> activeDialogues = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Vault setup
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            economy = getServer().getServicesManager().getRegistration(Economy.class).getProvider();
        }
    }

    public static Economy getEconomy() { return economy; }

    public Map<UUID, DialogueState> getActiveDialogues() { return activeDialogues; }

    // ================= DialogueState Inner Class ==================
    public static class DialogueState {
        private final File dialogueFile;
        private int currentLine = 0;
        private String currentSection = "start";

        public DialogueState(File file) { this.dialogueFile = file; }

        public File getDialogueFile() { return dialogueFile; }

        // TODO: ใส่โค้ดโหลด ConfigurationSection จากไฟล์ dialogue.yml
        public ConfigurationSection getCurrentLineConfig() { return null; }

        public void incrementLine() { currentLine++; }
        public void setCurrentSection(String section) { currentSection = section; }
        public List<String> getSections() { return List.of("start","nextSection"); }
    }

    // ================= DialogueRunner Inner Class ==================
    public static class DialogueRunner extends BukkitRunnable {
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
            if (lineConfig == null) { endDialogue("messages.dialogue-ended"); return; }

            String type = lineConfig.getString("type", "text");
            String lineText = lineConfig.getString("line", "");
            int delay = lineConfig.getInt("delay", plugin.getConfig().getInt("settings.default-delay", 20));

            String finalLine = lineText.replace("%player_name%", target.getName());
            if (placeholderApiEnabled) {
                finalLine = PlaceholderAPI.setPlaceholders(target, finalLine);
            }

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
                    this.cancel();
                    return;

                case "end":
                    endDialogue(lineConfig.getString("message", "messages.dialogue-ended"));
                    return;

                case "check_money":
                    double requiredMoney = lineConfig.getDouble("amount", 0.0);
                    double playerBalance = 0.0;
                    if (DialoguesEconomy.getEconomy() != null) {
                        playerBalance = DialoguesEconomy.getEconomy().getBalance(target);
                    } else {
                        plugin.getLogger().warning("Vault Economy is not setup. Skipping money check.");
                        state.incrementLine();
                        break;
                    }
                    if (playerBalance < requiredMoney) {
                        String failSection = lineConfig.getString("fail_goto");
                        if (failSection != null) state.setCurrentSection(failSection);
                        else { endDialogue("messages.dialogue-ended"); return; }
                    } else state.incrementLine();
                    break;

                case "take_money":
                    double takeAmount = lineConfig.getDouble("amount", 0.0);
                    if (DialoguesEconomy.getEconomy() != null) {
                        DialoguesEconomy.getEconomy().withdrawPlayer(target, takeAmount);
                        String currency = DialoguesEconomy.getEconomy().currencyNamePlural();
                        target.sendMessage(ChatColor.YELLOW + plugin.getConfig().getString("messages.chat-prefix") + " &cหักเงิน " + takeAmount + " " + currency);
                    } else target.sendMessage(ChatColor.RED + plugin.getConfig().getString("messages.chat-prefix") + " &cไม่สามารถหักเงินได้: Vault Economy ไม่พร้อมใช้งาน");
                    state.incrementLine();
                    break;

                case "give_item":
                    String itemType = lineConfig.getString("item");
                    int itemAmount = lineConfig.getInt("amount", 1);
                    try {
                        Material material = Material.valueOf(itemType.toUpperCase());
                        ItemStack item = new ItemStack(material, itemAmount);
                        target.getInventory().addItem(item);
                        target.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.chat-prefix") + " &aได้รับ " + itemAmount + "x " + material.name()));
                    } catch (IllegalArgumentException e) { plugin.getLogger().log(Level.WARNING, "Invalid material: " + itemType); }
                    state.incrementLine();
                    break;

                case "take_item":
                    String takeItemType = lineConfig.getString("item");
                    int takeItemAmount = lineConfig.getInt("amount", 1);
                    try {
                        Material material = Material.valueOf(takeItemType.toUpperCase());
                        int removedCount = removeItemFromInventory(target, material, takeItemAmount);
                        if (removedCount < takeItemAmount) { target.sendMessage(ChatColor.RED + plugin.getConfig().getString("messages.chat-prefix") + " &cไม่สามารถยึด Item ได้ครบ!"); endDialogue("messages.dialogue-ended"); return; }
                        target.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.chat-prefix") + " &cยึด " + removedCount + "x " + material.name() + " แล้ว"));
                    } catch (IllegalArgumentException e) { plugin.getLogger().log(Level.WARNING, "Invalid material: " + takeItemType); }
                    state.incrementLine();
                    break;

                default:
                    plugin.getLogger().warning("Unknown dialogue type: " + type);
                    state.incrementLine();
                    break;
            }

            new DialogueRunner(plugin, target, state, activeDialogues, placeholderApiEnabled).runTaskLater(plugin, delay);
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
                    if (removedCount >= amount) { player.getInventory().setContents(contents); return removedCount; }
                }
            }
            player.getInventory().setContents(contents);
            return removedCount;
        }

        private void endDialogue(String endMessageConfigPath) {
            String endMsg = plugin.getConfig().getString(endMessageConfigPath);
            String prefix = plugin.getConfig().getString("messages.chat-prefix", "&6[&bDialogue&6]");
            if (endMsg != null) { endMsg = endMsg.replace("%chat-prefix%", prefix); target.sendMessage(ChatColor.translateAlternateColorCodes('&', endMsg)); }
            activeDialogues.remove(target.getUniqueId());
            this.cancel();
        }

        private void sendLine(Player player, String line, String displayMode, String npcName) {
            String translatedLine = ChatColor.translateAlternateColorCodes('&', line);
            if (npcName != null) translatedLine = ChatColor.translateAlternateColorCodes('&', npcName + ": &r" + line);

            switch (displayMode.toLowerCase()) {
                case "title": player.sendTitle(translatedLine, "", 10, 70, 20); break;
                case "actionbar": player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(translatedLine)); break;
                case "chat":
                default: player.sendMessage(translatedLine); break;
            }

            String soundName = plugin.getConfig().getString("settings.default-sound", "ENTITY_EXPERIENCE_ORB_PICKUP");
            try { player.playSound(player.getLocation(), Sound.valueOf(soundName.toUpperCase()), 1f, 1f); } catch (IllegalArgumentException ignored) {}
        }

        private void sendChoice(Player player, String text, String action) {
            DialogueState state = activeDialogues.get(player.getUniqueId());
            if (state == null) return;
            TextComponent choiceComponent = new TextComponent(ChatColor.translateAlternateColorCodes('&', text));
            if (action.startsWith("goto:")) {
                String targetSection = action.substring(5);
                String secretCommand = String.format("/dialogue click %s %s %s", player.getName(), state.getDialogueFile().getName(), targetSection);
                choiceComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, secretCommand));
                choiceComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GREEN + "Click to choose: " + targetSection)));
                player.spigot().sendMessage(choiceComponent);
            } else player.sendMessage(ChatColor.RED + "Error in choice action: " + action);
        }
    }
}
