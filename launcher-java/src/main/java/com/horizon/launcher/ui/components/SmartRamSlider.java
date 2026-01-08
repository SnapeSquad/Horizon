package com.horizon.launcher.ui.components;

import com.horizon.launcher.ui.i18n.LocalizationManager;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.VBox;
import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Умный слайдер ОЗУ
 * 
 * Логика для нейросети:
 * - Получить TotalPhysicalMemory через OSHI
 * - Если Total < 4GB — лимит слайдера 2GB
 * - Если Total > 16GB — лимит слайдера 8GB (максимум для стабильной работы Java в MC)
 * - Шаг слайдера — 512MB
 */
public class SmartRamSlider extends VBox {
    private static final Logger logger = LoggerFactory.getLogger(SmartRamSlider.class);
    
    private final Slider slider;
    private final Label valueLabel;
    private final IntegerProperty ramValue = new SimpleIntegerProperty(4);
    
    private final int minRamGB;
    private final int maxRamGB;
    private final int totalRamGB;
    
    public SmartRamSlider() {
        // Получаем информацию о памяти через OSHI
        SystemInfo systemInfo = new SystemInfo();
        GlobalMemory memory = systemInfo.getHardware().getMemory();
        long totalPhysicalMemory = memory.getTotal();
        totalRamGB = (int) (totalPhysicalMemory / (1024L * 1024L * 1024L));
        
        logger.info("Общая память системы: {} GB", totalRamGB);
        
        // Определяем лимиты слайдера
        if (totalRamGB < 4) {
            // Если меньше 4GB — лимит 2GB
            minRamGB = 1;
            maxRamGB = 2;
            logger.info("Мало памяти (<4GB), лимит слайдера: 2GB");
        } else if (totalRamGB > 16) {
            // Если больше 16GB — лимит 8GB (максимум для стабильной работы)
            minRamGB = 2;
            maxRamGB = 8;
            logger.info("Много памяти (>16GB), лимит слайдера: 8GB");
        } else {
            // Для 4-16GB используем половину доступной памяти, но не более 8GB
            minRamGB = 2;
            maxRamGB = Math.min(8, totalRamGB / 2);
            logger.info("Средний объем памяти (4-16GB), лимит слайдера: {}GB", maxRamGB);
        }
        
        // Создаем слайдер
        slider = new Slider(minRamGB, maxRamGB, 4);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(1);
        slider.setMinorTickCount(0);
        slider.setSnapToTicks(true);
        
        // Устанавливаем шаг 512MB (0.5GB)
        slider.setBlockIncrement(0.5);
        
        // Привязываем значение к свойству
        ramValue.bindBidirectional(slider.valueProperty());
        
        // Обновляем значение при изменении слайдера
        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            // Округляем до ближайшего значения с шагом 0.5GB
            double rounded = Math.round(newVal.doubleValue() * 2) / 2.0;
            slider.setValue(rounded);
            updateLabel();
        });
        
        // Создаем метку с текущим значением
        valueLabel = new Label();
        valueLabel.getStyleClass().add("ram-value-label");
        updateLabel();
        
        // Добавляем элементы
        this.setSpacing(10);
        this.getChildren().addAll(slider, valueLabel);
        
        // Инициализируем значение по умолчанию
        setRamValue(4);
    }
    
    /**
     * Обновляет метку с текущим значением ОЗУ
     */
    private void updateLabel() {
        LocalizationManager i18n = LocalizationManager.getInstance();
        double value = slider.getValue();
        valueLabel.setText(i18n.get("main.ramAllocation", String.format("%.1f", value)));
    }
    
    /**
     * Получает текущее значение ОЗУ в GB
     */
    public int getRamValue() {
        return (int) Math.round(slider.getValue());
    }
    
    /**
     * Устанавливает значение ОЗУ в GB
     */
    public void setRamValue(int ramGB) {
        // Ограничиваем значение в пределах допустимого диапазона
        int clampedValue = Math.max(minRamGB, Math.min(maxRamGB, ramGB));
        slider.setValue(clampedValue);
        updateLabel();
    }
    
    /**
     * Получает IntegerProperty для привязки
     */
    public IntegerProperty ramValueProperty() {
        return ramValue;
    }
    
    /**
     * Получает минимальное значение ОЗУ
     */
    public int getMinRamGB() {
        return minRamGB;
    }
    
    /**
     * Получает максимальное значение ОЗУ
     */
    public int getMaxRamGB() {
        return maxRamGB;
    }
    
    /**
     * Получает общий объем ОЗУ системы
     */
    public int getTotalRamGB() {
        return totalRamGB;
    }
}

