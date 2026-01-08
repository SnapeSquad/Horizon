package com.horizon.launcher.ui.mvc;

import com.horizon.launcher.minecraft.GameLauncher;
import com.horizon.launcher.ui.components.SmartRamSlider;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Контроллер для главной страницы
 */
public class MainPageController extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(MainPageController.class);
    
    private String username;
    private GameLauncher gameLauncher;
    private SmartRamSlider ramSlider;
    private ToggleGroup serverGroup;
    private Button launchButton;
    private Label welcomeLabel;
    private Label ramLabel;
    
    public MainPageController(String username) {
        super();
        this.username = username;
        this.gameLauncher = new GameLauncher();
    }
    
    public void setRamSlider(SmartRamSlider ramSlider) {
        this.ramSlider = ramSlider;
    }
    
    public void setServerGroup(ToggleGroup serverGroup) {
        this.serverGroup = serverGroup;
    }
    
    public void setLaunchButton(Button launchButton) {
        this.launchButton = launchButton;
        if (launchButton != null) {
            launchButton.setOnAction(e -> handleLaunch());
        }
    }
    
    public void setWelcomeLabel(Label welcomeLabel) {
        this.welcomeLabel = welcomeLabel;
    }
    
    public void setRamLabel(Label ramLabel) {
        this.ramLabel = ramLabel;
    }
    
    /**
     * Обработка запуска игры
     */
    private void handleLaunch() {
        if (serverGroup == null || serverGroup.getSelectedToggle() == null) {
            logger.warn("Сервер не выбран");
            return;
        }
        
        RadioButton selected = (RadioButton) serverGroup.getSelectedToggle();
        String server = selected.getText().toLowerCase();
        
        int ramGB = ramSlider != null ? ramSlider.getRamValue() : 4;
        
        logger.info("Запуск игры: сервер={}, RAM={}GB", server, ramGB);
        
        if (launchButton != null) {
            launchButton.setDisable(true);
            launchButton.setText(t("main.launching"));
        }
        
        // Запускаем игру в отдельном потоке
        new Thread(() -> {
            try {
                gameLauncher.launch(server, username, ramGB);
            } catch (IOException e) {
                logger.error("Ошибка при запуске игры", e);
                javafx.application.Platform.runLater(() -> {
                    if (launchButton != null) {
                        launchButton.setDisable(false);
                        launchButton.setText(t("main.launchButton"));
                    }
                });
            }
        }).start();
    }
    
    @Override
    protected void updateLocalizedStrings() {
        if (welcomeLabel != null) {
            welcomeLabel.setText(t("main.welcome", username));
        }
        
        if (launchButton != null) {
            launchButton.setText(t("main.launchButton"));
        }
        
        if (ramLabel != null) {
            ramLabel.setText(t("main.ramSettings"));
        }
    }
}

