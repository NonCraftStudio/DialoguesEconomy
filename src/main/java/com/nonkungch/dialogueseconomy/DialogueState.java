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
     * *NOTE: มีการแปลง MapList ไปเป็น ConfigurationSection ชั่วคราว
     */
    @SuppressWarnings("unchecked")
    public ConfigurationSection getCurrentLineConfig() {
        ConfigurationSection section = dialogueConfig.getConfigurationSection(currentSection);
        if (section == null) return null;
        
        // ใช้ getList เพื่อดึง List<Map> 
        List<?> linesListRaw = section.getList("lines"); 
        if (linesListRaw == null || linesListRaw.isEmpty() || currentLineIndex >= linesListRaw.size()) {
            return null; 
        }

        // ต้องตรวจสอบให้แน่ใจว่าแต่ละ Element เป็น Map
        Object lineObject = linesListRaw.get(currentLineIndex);
        if (!(lineObject instanceof Map)) {
            DialoguesEconomy.getInstance().getLogger().warning("Invalid line data found in section: " + currentSection);
            return null;
        }

        // แปลง Map เป็น ConfigurationSection ชั่วคราวเพื่ออ่านค่าอย่างง่ายดาย
        Map<String, Object> lineMap = (Map<String, Object>) lineObject;
        YamlConfiguration tempConfig = new YamlConfiguration();
        lineMap.forEach((key, value) -> tempConfig.set(key, value));
        return tempConfig;
    }
}