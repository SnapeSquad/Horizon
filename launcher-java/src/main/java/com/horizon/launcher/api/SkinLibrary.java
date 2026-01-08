package com.horizon.launcher.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Библиотека скинов пользователя
 * Сохраняет и загружает скины из локального хранилища
 */
public class SkinLibrary {
    private static final Logger logger = LoggerFactory.getLogger(SkinLibrary.class);
    private static SkinLibrary instance;
    private static final String SKINS_DIR = "skins";
    private static final String LIBRARY_FILE = "skin_library.json";
    
    private List<SkinEntry> skins;
    private Gson gson;
    private Path skinsDirectory;
    private Path libraryFile;
    
    private SkinLibrary() {
        gson = new GsonBuilder().setPrettyPrinting().create();
        skins = new ArrayList<>();
        
        try {
            // Создаем директорию для скинов
            skinsDirectory = Paths.get(System.getProperty("user.home"), ".horizon", SKINS_DIR);
            Files.createDirectories(skinsDirectory);
            
            libraryFile = skinsDirectory.resolve(LIBRARY_FILE);
            
            // Загружаем библиотеку
            loadLibrary();
            
            logger.info("Библиотека скинов инициализирована: {} скинов", skins.size());
        } catch (IOException e) {
            logger.error("Ошибка инициализации библиотеки скинов", e);
        }
    }
    
    public static synchronized SkinLibrary getInstance() {
        if (instance == null) {
            instance = new SkinLibrary();
        }
        return instance;
    }
    
    /**
     * Добавляет скин в библиотеку
     */
    public SkinEntry addSkin(String name, byte[] skinData, boolean isSlim) {
        try {
            String id = UUID.randomUUID().toString();
            String filename = id + ".png";
            Path skinFile = skinsDirectory.resolve(filename);
            
            // Сохраняем файл скина
            Files.write(skinFile, skinData);
            
            // Создаем запись
            SkinEntry entry = new SkinEntry(id, name, filename, isSlim, System.currentTimeMillis());
            skins.add(0, entry); // Добавляем в начало списка
            
            // Сохраняем библиотеку
            saveLibrary();
            
            logger.info("Скин добавлен в библиотеку: {}", name);
            return entry;
        } catch (IOException e) {
            logger.error("Ошибка добавления скина в библиотеку", e);
            return null;
        }
    }
    
    /**
     * Добавляет скин из URL
     */
    public SkinEntry addSkin(String name, String skinUrl, boolean isSlim) {
        try {
            // Скачиваем скин
            java.net.URL url = new java.net.URL(skinUrl);
            java.io.InputStream in = url.openStream();
            byte[] skinData = in.readAllBytes();
            in.close();
            
            return addSkin(name, skinData, isSlim);
        } catch (IOException e) {
            logger.error("Ошибка загрузки скина с URL: {}", skinUrl, e);
            return null;
        }
    }
    
    /**
     * Удаляет скин из библиотеки
     */
    public boolean removeSkin(String id) {
        SkinEntry entry = findSkinById(id);
        if (entry == null) {
            logger.warn("Скин не найден: {}", id);
            return false;
        }
        
        try {
            // Удаляем файл
            Path skinFile = skinsDirectory.resolve(entry.getFilename());
            Files.deleteIfExists(skinFile);
            
            // Удаляем из списка
            skins.remove(entry);
            
            // Сохраняем библиотеку
            saveLibrary();
            
            logger.info("Скин удален: {}", entry.getName());
            return true;
        } catch (IOException e) {
            logger.error("Ошибка удаления скина", e);
            return false;
        }
    }
    
    /**
     * Получает данные скина
     */
    public byte[] getSkinData(String id) {
        SkinEntry entry = findSkinById(id);
        if (entry == null) {
            return null;
        }
        
        try {
            Path skinFile = skinsDirectory.resolve(entry.getFilename());
            return Files.readAllBytes(skinFile);
        } catch (IOException e) {
            logger.error("Ошибка чтения данных скина", e);
            return null;
        }
    }
    
    /**
     * Получает InputStream скина
     */
    public InputStream getSkinStream(String id) {
        byte[] data = getSkinData(id);
        if (data == null) {
            return null;
        }
        return new ByteArrayInputStream(data);
    }
    
    /**
     * Переименовывает скин
     */
    public boolean renameSkin(String id, String newName) {
        SkinEntry entry = findSkinById(id);
        if (entry == null) {
            return false;
        }
        
        entry.setName(newName);
        saveLibrary();
        
        logger.info("Скин переименован: {} -> {}", id, newName);
        return true;
    }
    
    /**
     * Находит скин по ID
     */
    private SkinEntry findSkinById(String id) {
        return skins.stream()
                .filter(s -> s.getId().equals(id))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Получает все скины
     */
    public List<SkinEntry> getAllSkins() {
        return new ArrayList<>(skins);
    }
    
    /**
     * Загружает библиотеку из файла
     */
    private void loadLibrary() {
        if (!Files.exists(libraryFile)) {
            logger.info("Файл библиотеки не найден, создаем новую");
            skins = new ArrayList<>();
            return;
        }
        
        try {
            String json = Files.readString(libraryFile);
            Type listType = new TypeToken<ArrayList<SkinEntry>>(){}.getType();
            skins = gson.fromJson(json, listType);
            
            if (skins == null) {
                skins = new ArrayList<>();
            }
            
            logger.info("Библиотека загружена: {} скинов", skins.size());
        } catch (IOException e) {
            logger.error("Ошибка загрузки библиотеки", e);
            skins = new ArrayList<>();
        }
    }
    
    /**
     * Сохраняет библиотеку в файл
     */
    private void saveLibrary() {
        try {
            String json = gson.toJson(skins);
            Files.writeString(libraryFile, json);
            logger.debug("Библиотека сохранена");
        } catch (IOException e) {
            logger.error("Ошибка сохранения библиотеки", e);
        }
    }
    
    /**
     * Класс для хранения информации о скине
     */
    public static class SkinEntry {
        private String id;
        private String name;
        private String filename;
        private boolean isSlim;
        private long addedTimestamp;
        
        public SkinEntry(String id, String name, String filename, boolean isSlim, long addedTimestamp) {
            this.id = id;
            this.name = name;
            this.filename = filename;
            this.isSlim = isSlim;
            this.addedTimestamp = addedTimestamp;
        }
        
        public String getId() {
            return id;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getFilename() {
            return filename;
        }
        
        public boolean isSlim() {
            return isSlim;
        }
        
        public long getAddedTimestamp() {
            return addedTimestamp;
        }
    }
}

