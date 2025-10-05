package com.nonkungch.dialogueseconomy;

public class DialogueState {

    private final String id;
    private final String[] lines;
    private int currentLine = 0;

    public DialogueState(String id, String[] lines) {
        this.id = id;
        this.lines = lines;
    }

    public String getId() { return id; }
    public String[] getLines() { return lines; }
    public int getCurrentLineIndex() { return currentLine; }
    public void incrementLine() { currentLine++; }
    public boolean isFinished() { return currentLine >= lines.length; }
    public String getCurrentLine() { return isFinished() ? null : lines[currentLine]; }
}
