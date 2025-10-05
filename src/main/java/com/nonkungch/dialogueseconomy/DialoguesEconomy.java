package com.yourname.dialogueplugin;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.List;
import java.util.Collections; // สำหรับ DialogueFileManager จำลอง

// Dependencies
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;

// Bungee Chat API for Java Edition's Clickable Text
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.ClickEvent;

// PlaceholderAPI (ต้องมี Library ใน Build Path)
import me.clip.placeholderapi.PlaceholderAPI;

// Floodgate API for Bedrock Edition's Forms (ต้องมี Library ใน Build Path)
// **NOTE: ต้องมี Floodgate JAR ใน Build Path เพื่อให้โค้ดส่วนนี้ Compile ได้**
// import org.geysermc.floodgate.api.FloodgateApi;
// import org.geysermc.floodgate.api.player.PublicPlayer;
// import org.geysermc.floodgate.api.SimpleForm; 

public class DialoguesEconomy extends JavaPlugin {

    private static DialoguesEconomy instance;
    private Economy economy = null;
    
    // 1. เก็บ Map ของผู้เล่นที่กำลังคุย
    private final Map<UUID, DialogueState> activeDialogues = new HashMap<>();

    // Map จำลองสำหรับเก็บข้อมูลบทสนทนา (DialogueFileManager)
    private final Map<String, Dialogue> loadedDialogues = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        
        // 1. โหลดไฟล์
        this.saveDefaultConfig();
        loadAllDialogues(); // โหลดบทสนทนาจำลอง
        
        // 2. ตรวจสอบ Vault
        if (!setupEconomy()) {
            getLogger().warning("Vault dependency not found. Economy features disabled.");
        }
        
        // 3. ลงทะเบียนคำสั่ง: /dialogue และ /dia
        this.getCommand("dialogue").setExecutor(new DialogueCommand());
        this.getCommand("dialogue_choice").setExecutor(new DialogueChoiceCommand());
        
        getLogger().info("DialoguesEconomy is enabled.");
    }

    // Vault Setup (ย่อ)
    private boolean setupEconomy() {
        // ... Logic การเชื่อมต่อ Vault ...
        return true; // สมมติว่าสำเร็จ
    }

    // DialogueFileManager จำลอง (ในโค้ดจริงต้องอ่านจาก dialogue.yml)
    private void loadAllDialogues() {
        // ... โค้ดจริงจะอ่านจากไฟล์ ...
        // ตัวอย่างการโหลด
        List<Map<String, Object>> lines = List.of(
            Map.of("type", "text", "line", "สวัสดี %player_name%! คุณจะรับเควสนี้ไหม?", "delay", 40),
            Map.of("type", "check_money", "amount", 100.0, "success_id", "quest_accept", "fail_id", "quest_deny"),
            Map.of("type", "end")
        );
        loadedDialogues.put("intro_quest", new Dialogue("intro_quest", lines));
        
        List<Map<String, Object>> acceptLines = List.of(
            Map.of("type", "text", "line", "&aยินดีต้อนรับสู่เควส! คุณต้องการหักเงิน 100 เหรียญหรือไม่?", "delay", 20),
            Map.of("type", "choice", "line", "เลือกเลย:", "options", List.of(
                Map.of("text", "&7[&2หักเงิน&7]", "next_id", "take_money"),
                Map.of("text", "&7[&4ไม่หัก&7]", "next_id", "quest_deny")
            ))
        );
        loadedDialogues.put("quest_accept", new Dialogue("quest_accept", acceptLines));
        
        List<Map<String, Object>> denyLines = List.of(
            Map.of("type", "text", "line", "&cคุณปฏิเสธ หรือมีเงินไม่พอ", "delay", 20),
            Map.of("type", "end")
        );
        loadedDialogues.put("quest_deny", new Dialogue("quest_deny", denyLines));
        
        List<Map<String, Object>> takeMoneyLines = List.of(
            Map.of("type", "take_money", "amount", 100.0),
            Map.of("type", "text", "line", "&6หักเงินเรียบร้อยแล้ว! ขอบคุณ!", "delay", 20),
            Map.of("type", "end")
        );
        loadedDialogues.put("take_money", new Dialogue("take_money", takeMoneyLines));
    }
    
    // Getters
    public static DialoguesEconomy getInstance() { return instance; }
    public Economy getEconomy() { return economy; }
    public Map<UUID, DialogueState> getActiveDialogues() { return activeDialogues; }
    public Dialogue getDialogue(String id) { return loadedDialogues.get(id); }

    // =========================================================================
    // 1. Dialogue State Class
    // =========================================================================
    public static class DialogueState {
        private final String dialogueId;
        private final List<Map<String, Object>> lines; 
        private int currentLineIndex = 0;

        public DialogueState(String dialogueId, List<Map<String, Object>> lines) {
            this.dialogueId = dialogueId;
            this.lines = lines;
        }

        public void incrementLine() { currentLineIndex++; }
        public boolean isFinished() { return currentLineIndex >= lines.size(); }
        public Map<String, Object> getCurrentLine() {
            return isFinished() ? null : lines.get(currentLineIndex);
        }
        
        public void setCurrentLineIndex(int index) { this.currentLineIndex = index; }
        public String getDialogueId() { return dialogueId; }
        public List<Map<String, Object>> getLines() { return lines; }
    }

    // =========================================================================
    // 2. Dialogue Runner Class
    // =========================================================================
    public static class DialogueRunner {

        public static void startDialogue(Player player, DialogueState state) {
            instance.getActiveDialogues().put(player.getUniqueId(), state);
            runNextLine(player, state);
        }

        public static void endDialogue(Player player) {
            instance.getActiveDialogues().remove(player.getUniqueId());
            player.sendMessage(ChatColor.GRAY + "--- " + 
                               instance.getConfig().getString("end-message", "บทสนทนาจบลงแล้ว") + 
                               " ---");
        }
        
        public static void runNextLine(Player player, DialogueState state) {
            if (state.isFinished()) {
                endDialogue(player);
                return;
            }

            Map<String, Object> lineData = state.getCurrentLine();
            String type = (String) lineData.getOrDefault("type", "text");
            int delay = instance.getConfig().getInt("default-delay", 20); // 20 ticks = 1 second

            switch (type) {
                case "text":
                    handleTextLine(player, lineData);
                    delay = (int) lineData.getOrDefault("delay", delay);
                    state.incrementLine();
                    break;
                case "check_money":
                    handleCheckMoney(player, state, lineData);
                    return; // check_money/take_money จัดการ runNextLine เอง
                case "take_money":
                    handleTakeMoney(player, state, lineData);
                    return;
                case "choice":
                    handleChoice(player, lineData);
                    return; // choice รอผู้เล่นคลิก
                case "end":
                    endDialogue(player);
                    return;
            }

            // รันบรรทัดถัดไปตาม delay
            Bukkit.getScheduler().runTaskLater(instance, () -> runNextLine(player, state), delay);
        }
        
        // Handlers:
        private static void handleTextLine(Player player, Map<String, Object> lineData) {
            String rawText = (String) lineData.get("line");
            String processedText = PlaceholderAPI.setPlaceholders(player, rawText);
            processedText = ChatColor.translateAlternateColorCodes('&', processedText);
            
            player.sendMessage(instance.getConfig().getString("prefix", "&b[NPC] &r") + processedText);
        }
        
        private static void handleCheckMoney(Player player, DialogueState state, Map<String, Object> lineData) {
            double required = ((Number) lineData.get("amount")).doubleValue();
            String successId = (String) lineData.get("success_id");
            String failId = (String) lineData.get("fail_id");
            
            String nextId = (instance.getEconomy() != null && instance.getEconomy().has(player, required)) 
                             ? successId : failId;
            
            Dialogue nextDialogue = instance.getDialogue(nextId);
            if (nextDialogue != null) {
                startDialogue(player, new DialogueState(nextId, nextDialogue.getLines()));
            } else {
                 player.sendMessage(ChatColor.RED + "Error: Next Dialogue ID not found: " + nextId);
                 endDialogue(player);
            }
        }
        
        private static void handleTakeMoney(Player player, DialogueState state, Map<String, Object> lineData) {
            double amount = ((Number) lineData.get("amount")).doubleValue();
            
            if (instance.getEconomy() != null) {
                instance.getEconomy().withdrawPlayer(player, amount);
                player.sendMessage(ChatColor.GREEN + "หักเงิน " + String.format("%.2f", amount) + " เรียบร้อย");
            } 
            state.incrementLine();
            runNextLine(player, state);
        }

        private static void handleChoice(Player player, Map<String, Object> lineData) {
            boolean isBedrock = false;
            // ตรวจสอบ Floodgate (จำลอง)
            // if (instance.getServer().getPluginManager().isPluginEnabled("Floodgate")) {
            //     try { isBedrock = FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId()); } 
            //     catch (Exception ignored) {}
            // }

            String dialogueId = instance.getActiveDialogues().get(player.getUniqueId()).getDialogueId();
            List<Map<String, Object>> options = (List<Map<String, Object>>) lineData.get("options");

            if (isBedrock) {
                // ** BEDROCK: ใช้ Form UI **
                // PublicPlayer publicPlayer = FloodgateApi.getInstance().getPlayer(player.getUniqueId());
                // ... สร้าง SimpleForm และส่งให้ publicPlayer.sendForm() ...
                player.sendMessage("Bedrock UI: Showing Form with choices..."); // ข้อความจำลอง
            } else {
                // ** JAVA: ใช้ Clickable Chat **
                handleTextLine(player, lineData); // แสดงข้อความก่อนเลือก
                for (int i = 0; i < options.size(); i++) {
                    Map<String, Object> option = options.get(i);
                    String text = (String) option.get("text");
                    TextComponent component = new TextComponent(ChatColor.translateAlternateColorCodes('&', text));
                    
                    // คำสั่งที่จะรันเมื่อคลิก: /dialogue_choice <id> <index>
                    component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, 
                            "/dialogue_choice " + dialogueId + " " + i));
                    player.spigot().sendMessage(component);
                }
            }
        }
    }

    // =========================================================================
    // 3. Command Handlers
    // =========================================================================
    public class DialogueCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player)) return true;
            Player player = (Player) sender;
            if (args.length < 1) {
                player.sendMessage(ChatColor.RED + "Usage: /" + label + " <id>");
                return true;
            }
            
            String dialogueId = args[0];
            Dialogue dialogue = getDialogue(dialogueId); 
            
            if (dialogue == null) {
                player.sendMessage(ChatColor.RED + "Dialogue ID '" + dialogueId + "' not found.");
                return true;
            }
            
            if (activeDialogues.containsKey(player.getUniqueId())) {
                 player.sendMessage(ChatColor.RED + "คุณกำลังคุยกับคนอื่นอยู่!");
                 return true;
            }
            
            DialogueState state = new DialogueState(dialogueId, dialogue.getLines());
            DialogueRunner.startDialogue(player, state);
            return true;
        }
    }
    
    public class DialogueChoiceCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player)) return true;
            Player player = (Player) sender;
            if (args.length < 2) return true;
            
            String dialogueId = args[0];
            int choiceIndex;
            try { choiceIndex = Integer.parseInt(args[1]); } catch (NumberFormatException e) { return true; }

            DialogueState state = activeDialogues.get(player.getUniqueId());

            if (state == null || !state.getDialogueId().equals(dialogueId)) return true;

            Map<String, Object> currentLine = state.getCurrentLine();
            if (currentLine == null || !"choice".equals(currentLine.get("type"))) return true;

            List<Map<String, Object>> options = (List<Map<String, Object>>) currentLine.get("options");
            if (options == null || choiceIndex < 0 || choiceIndex >= options.size()) return true;
            
            Map<String, Object> chosenOption = options.get(choiceIndex);
            
            // ประมวลผลทางเลือก
            if (chosenOption.containsKey("next_id")) {
                String nextId = (String) chosenOption.get("next_id");
                Dialogue nextDialogue = getDialogue(nextId);
                if (nextDialogue != null) {
                    DialogueRunner.startDialogue(player, new DialogueState(nextId, nextDialogue.getLines()));
                } else { DialogueRunner.endDialogue(player); }
            } else if (chosenOption.containsKey("next_line")) {
                int nextLine = ((Number) chosenOption.get("next_line")).intValue();
                state.setCurrentLineIndex(nextLine);
                DialogueRunner.runNextLine(player, state);
            } else {
                state.incrementLine();
                DialogueRunner.runNextLine(player, state);
            }
            return true;
        }
    }

    // =========================================================================
    // 4. Supporting Data Classes
    // =========================================================================
    // คลาสสำหรับเก็บโครงสร้าง Dialogue (ใช้แทนการอ่านจากไฟล์โดยตรง)
    public static class Dialogue {
        private final String id;
        private final List<Map<String, Object>> lines;
        public Dialogue(String id, List<Map<String, Object>> lines) {
            this.id = id;
            this.lines = lines;
        }
        public List<Map<String, Object>> getLines() { return lines; }
    }
}
