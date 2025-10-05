package com.nonkungch.dialogueseconomy;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DialogueCommand implements CommandExecutor {

    private final DialoguesEconomy plugin;

    public DialogueCommand(DialoguesEconomy plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage("Usage: /dialogue <id>");
            return true;
        }

        String dialogueId = args[0];

        // ตัวอย่างเรียกบทสนทนา
        String[] lines = { "สวัสดี %player_name%", "ยินดีต้อนรับสู่เซิร์ฟเวอร์" };

        DialogueState state = new DialogueState(dialogueId, lines);
        plugin.activeDialogues.put(player.getUniqueId(), state);

        new DialogueRunner(plugin, player, state, plugin.activeDialogues, true).runTask(plugin);

        return true;
    }
          }
