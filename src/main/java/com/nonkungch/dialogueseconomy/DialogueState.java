package com.nonkungch.dialogueseconomy;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;
import java.util.Map;

public class DialogueState {

    private final File dialogueFile;
    private final FileConfiguration dialogueConfig;
    private String currentSection;
    private int currentLineIndex;

    public DialogueState(File dialogueFile, FileConfiguration dialogueConfig, String startSection) {
        this.dialogueFile = dialogueFile;
        this.dialogueConfig = dialogueConfig;
        this.currentSection = startSection;
        this.currentLineIndex = 0;
    }

    public File getDialogueFile() {
        return dialogueFile;
    }

    public String getDialogueFileName() {
        return dialogueFile.getName();
    }

    public void setCurrentSection(String section) {
        this.currentSection = section;
        this.currentLineIndex = 0;
    }

    public int getCurrentLineIndex() {
        return currentLineIndex;
    }

    public void incrementLine() {
        this.currentLineIndex++;
    }

    /**
     * ดึง ConfigurationSection ของบรรทัดบทสนทนาปัจจุบัน
     * รองรับทั้งแบบใหม่ (lines:) และแบบเก่า (text:)
     */
    @SuppressWarnings("unchecked")
    public ConfigurationSection getCurrentLineConfig() {
        ConfigurationSection section = dialogueConfig.getConfigurationSection(currentSection);
        if (section == null) return null;

        // ---- 1. ระบบใหม่แบบ lines: ----
        List<?> linesListRaw = section.getList("lines");
        if (linesListRaw != null && !linesListRaw.isEmpty()) {
            if (currentLineIndex >= linesListRaw.size()) return null;

            Object lineObject = linesListRaw.get(currentLineIndex);
            if (!(lineObject instanceof Map)) {
                DialoguesEconomy.getInstance().getLogger().warning("Invalid line data found in section: " + currentSection);
                return null;
            }

            Map<String, Object> lineMap = (Map<String, Object>) lineObject;
            YamlConfiguration tempConfig = new YamlConfiguration();
            lineMap.forEach(tempConfig::set);
            return tempConfig;
        }

        // ---- 2. ระบบเก่าแบบ text: ----
        if (section.isList("text")) {
            List<?> textList = section.getList("text");
            if (textList != null && currentLineIndex < textList.size()) {
                Object textLine = textList.get(currentLineIndex);
                YamlConfiguration tempConfig = new YamlConfiguration();
                tempConfig.set("type", "text");
                tempConfig.set("line", String.valueOf(textLine));
                return tempConfig;
            } else {
                return null;
            }
        }

        return null;
    }
}