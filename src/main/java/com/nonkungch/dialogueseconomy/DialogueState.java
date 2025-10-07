package com.nonkungch.dialogueseconomy;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

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
     * รองรับทุกรูปแบบ: (1) lines: [...] (2) section: [...] (3) text: [...]
     */
    @SuppressWarnings("unchecked")
    public ConfigurationSection getCurrentLineConfig() {
        // ดึง Section หลัก (เช่น "start" หรือ "dialogues.start")
        ConfigurationSection section = dialogueConfig.getConfigurationSection(currentSection);
        
        // ถ้าเป็น Section ที่เป็น Map (มี Key/Value) แต่ไม่มี List เลย จะถือว่าไม่มีบรรทัด
        if (section == null && !(dialogueConfig.get(currentSection) instanceof List)) {
            return null;
        }

        List<?> linesListRaw = null;
        
        // 1. ตรวจสอบรูปแบบใหม่: lines: [ List of Map ]
        if (section != null && section.isList("lines")) {
            linesListRaw = section.getList("lines");
            DialoguesEconomy.getInstance().getLogger().log(Level.FINEST, "Dialogue Loader: Found lines list under 'lines' key.");
        } 
        // 2. ตรวจสอบรูปแบบ List ที่ระดับ Section โดยตรง: start: [ List of Map ] 
        // (นี่คือรูปแบบที่คุณใช้)
        else if (dialogueConfig.get(currentSection) instanceof List) {
            linesListRaw = (List<?>) dialogueConfig.get(currentSection);
            DialoguesEconomy.getInstance().getLogger().log(Level.FINEST, "Dialogue Loader: Found lines list directly under section key.");
        }
        // 3. ตรวจสอบรูปแบบเก่า: text: [ List of String ]
        else if (section != null && section.isList("text")) {
            linesListRaw = section.getList("text");
            DialoguesEconomy.getInstance().getLogger().log(Level.FINEST, "Dialogue Loader: Found lines list under 'text' key (legacy).");
        }
        
        // ถ้าไม่พบ List เลย
        if (linesListRaw == null || linesListRaw.isEmpty()) {
            return null;
        }

        // ตรวจสอบว่า Line Index เกินขนาด List หรือไม่
        if (currentLineIndex >= linesListRaw.size()) {
            return null;
        }

        // ดึง Object ของบรรทัดปัจจุบัน
        Object lineObject = linesListRaw.get(currentLineIndex);
        
        YamlConfiguration tempConfig = new YamlConfiguration();

        if (lineObject instanceof Map) {
            // Case 1: เป็น Map (ใช้สำหรับ say/cmd/chat/choice)
            Map<String, Object> lineMap = (Map<String, Object>) lineObject;
            lineMap.forEach(tempConfig::set);
            return tempConfig;
        } 
        else if (lineObject instanceof String) {
            // Case 2: เป็น String (ใช้สำหรับรูปแบบเก่า text: [...] หรือรายการ String ธรรมดา)
            
            // ใช้ Key "say" แทน "line" เพื่อให้ DialogueRunner ประมวลผลได้ทันที
            // โดยยังคงรองรับการแทนที่ Placeholder
            tempConfig.set("say", String.valueOf(lineObject)); 
            
            // ตั้ง type เป็น "text" สำหรับ backward compatibility (เผื่อมี logic อื่น)
            tempConfig.set("type", "text"); 
            
            return tempConfig;
        }
        else {
            DialoguesEconomy.getInstance().getLogger().warning("Invalid line data type found in section: " + currentSection + " at index: " + currentLineIndex + ". Found type: " + lineObject.getClass().getSimpleName());
            return null;
        }
    }
}