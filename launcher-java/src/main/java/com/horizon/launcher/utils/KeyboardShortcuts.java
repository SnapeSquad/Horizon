package com.horizon.launcher.utils;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

/**
 * Утилита для управления keyboard shortcuts в лаунчере
 */
public class KeyboardShortcuts {
    
    /**
     * Комбинации клавиш для различных действий
     */
    public static final KeyCodeCombination CTRL_N = new KeyCodeCombination(
        KeyCode.N, KeyCombination.CONTROL_DOWN
    );
    
    public static final KeyCodeCombination CTRL_O = new KeyCodeCombination(
        KeyCode.O, KeyCombination.CONTROL_DOWN
    );
    
    public static final KeyCodeCombination CTRL_S = new KeyCodeCombination(
        KeyCode.S, KeyCombination.CONTROL_DOWN
    );
    
    public static final KeyCodeCombination CTRL_F = new KeyCodeCombination(
        KeyCode.F, KeyCombination.CONTROL_DOWN
    );
    
    public static final KeyCodeCombination CTRL_Q = new KeyCodeCombination(
        KeyCode.Q, KeyCombination.CONTROL_DOWN
    );
    
    public static final KeyCodeCombination CTRL_COMMA = new KeyCodeCombination(
        KeyCode.COMMA, KeyCombination.CONTROL_DOWN
    );
    
    public static final KeyCodeCombination F5 = new KeyCodeCombination(KeyCode.F5);
    
    public static final KeyCodeCombination ESCAPE = new KeyCodeCombination(KeyCode.ESCAPE);
    
    public static final KeyCodeCombination ENTER = new KeyCodeCombination(KeyCode.ENTER);
    
    /**
     * Навигация по секциям
     */
    public static final KeyCodeCombination CTRL_1 = new KeyCodeCombination(
        KeyCode.DIGIT1, KeyCombination.CONTROL_DOWN
    );
    
    public static final KeyCodeCombination CTRL_2 = new KeyCodeCombination(
        KeyCode.DIGIT2, KeyCombination.CONTROL_DOWN
    );
    
    public static final KeyCodeCombination CTRL_3 = new KeyCodeCombination(
        KeyCode.DIGIT3, KeyCombination.CONTROL_DOWN
    );
    
    public static final KeyCodeCombination CTRL_4 = new KeyCodeCombination(
        KeyCode.DIGIT4, KeyCombination.CONTROL_DOWN
    );
    
    public static final KeyCodeCombination CTRL_5 = new KeyCodeCombination(
        KeyCode.DIGIT5, KeyCombination.CONTROL_DOWN
    );
    
    /**
     * Получить описание комбинации клавиш для отображения в UI
     */
    public static String getDescription(KeyCodeCombination combination) {
        if (combination.equals(CTRL_N)) return "Ctrl+N";
        if (combination.equals(CTRL_O)) return "Ctrl+O";
        if (combination.equals(CTRL_S)) return "Ctrl+S";
        if (combination.equals(CTRL_F)) return "Ctrl+F";
        if (combination.equals(CTRL_Q)) return "Ctrl+Q";
        if (combination.equals(CTRL_COMMA)) return "Ctrl+,";
        if (combination.equals(F5)) return "F5";
        if (combination.equals(ESCAPE)) return "Esc";
        if (combination.equals(ENTER)) return "Enter";
        if (combination.equals(CTRL_1)) return "Ctrl+1";
        if (combination.equals(CTRL_2)) return "Ctrl+2";
        if (combination.equals(CTRL_3)) return "Ctrl+3";
        if (combination.equals(CTRL_4)) return "Ctrl+4";
        if (combination.equals(CTRL_5)) return "Ctrl+5";
        
        return combination.getDisplayText();
    }
}
