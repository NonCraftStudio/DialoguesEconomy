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
     */
    public ConfigurationSection getCurrentLineConfig() {
        ConfigurationSection section = dialogueConfig.getConfigurationSection(currentSection);
        if (section == null) return null;
        
        List<Map<?, ?>> linesList = section.getMapList("lines");
        if (linesList == null || linesList.isEmpty() || currentLineIndex >= linesList.size()) {
            return null; // จบบรรทัดใน Section นี้
        }

        // แปลง Map เป็น ConfigurationSection ชั่วคราวเพื่ออ่านค่า
        Map<?, ?> lineMap = linesList.get(currentLineIndex);
        YamlConfiguration tempConfig = new YamlConfiguration();
        lineMap.forEach((key, value) -> tempConfig.set(key.toString(), value));
        return tempConfig;
    }
}
