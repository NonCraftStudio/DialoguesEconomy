// ในไฟล์ใหม่: com.nonkungch.dialogueseconomy.DialogueState.java

package com.nonkungch.dialogueseconomy;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

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

    public FileConfiguration getDialogueConfig() {
        return dialogueConfig;
    }

    public void setCurrentSection(String section) {
        this.currentSection = section;
        this.currentLineIndex = 0; // รีเซ็ตบรรทัดเมื่อเปลี่ยน Section
    }

    public int getCurrentLineIndex() {
        return currentLineIndex;
    }

    public void incrementLine() {
        this.currentLineIndex++;
    }

    public ConfigurationSection getCurrentLineConfig() {
        ConfigurationSection section = dialogueConfig.getConfigurationSection(currentSection);
        if (section == null) return null;
        
        // YamlConfiguration.getMapList() is safer than just getting a list directly
        List<Map<?, ?>> linesList = section.getMapList("lines");
        if (linesList == null || linesList.isEmpty() || currentLineIndex >= linesList.size()) {
            return null;
        }

        // ต้องสร้าง ConfigurationSection ชั่วคราวจาก Map เพื่อให้ใช้งานง่ายขึ้น
        Map<?, ?> lineMap = linesList.get(currentLineIndex);
        if (lineMap instanceof ConfigurationSection) {
            return (ConfigurationSection) lineMap;
        }
        
        // สร้าง ConfigSection ชั่วคราวจาก Map (ถ้าจำเป็น)
        YamlConfiguration tempConfig = new YamlConfiguration();
        lineMap.forEach((key, value) -> tempConfig.set(key.toString(), value));
        return tempConfig.getRoot();
    }
    
    // เมธอดสำหรับดึง ConfigurationSection ปัจจุบัน
    public ConfigurationSection getCurrentDialogueSection() {
        return dialogueConfig.getConfigurationSection(currentSection);
    }
}