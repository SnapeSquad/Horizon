package com.horizon.launcher.ui.mvc;

import javafx.scene.layout.Pane;

/**
 * Базовый класс для View в MVC архитектуре
 */
public abstract class BaseView {
    protected Pane root;
    protected BaseController controller;
    
    public BaseView(BaseController controller) {
        this.controller = controller;
        this.root = createRoot();
    }
    
    /**
     * Создает корневой элемент View
     * Должен быть переопределен в дочерних классах
     */
    protected abstract Pane createRoot();
    
    /**
     * Получает корневой элемент
     */
    public Pane getRoot() {
        return root;
    }
    
    /**
     * Получает контроллер
     */
    public BaseController getController() {
        return controller;
    }
    
    /**
     * Обновляет View (например, при изменении локализации)
     */
    public void update() {
        if (controller != null) {
            controller.updateLocalizedStrings();
        }
    }
}

