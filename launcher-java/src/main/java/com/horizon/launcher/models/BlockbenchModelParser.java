package com.horizon.launcher.models;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Парсер для моделей Blockbench в формате Bedrock Geometry
 * Поддерживает формат .json, используемый BlockBench для Java-модов Minecraft
 */
public class BlockbenchModelParser {
    private static final Logger logger = LoggerFactory.getLogger(BlockbenchModelParser.class);
    private static final Gson gson = new Gson();
    
    /**
     * Загрузить модель из файла
     * @param file Файл модели (.json)
     * @return Данные модели или null при ошибке
     */
    public static ModelData parseFromFile(File file) {
        if (file == null || !file.exists()) {
            logger.error("Файл модели не существует: {}", file);
            return null;
        }
        
        try (FileReader reader = new FileReader(file)) {
            return parse(reader);
        } catch (IOException e) {
            logger.error("Ошибка при чтении файла модели: {}", file, e);
            return null;
        }
    }
    
    /**
     * Загрузить модель из пути
     * @param path Путь к файлу модели
     * @return Данные модели или null при ошибке
     */
    public static ModelData parseFromPath(Path path) {
        if (path == null || !Files.exists(path)) {
            logger.error("Путь к модели не существует: {}", path);
            return null;
        }
        
        try (InputStream is = Files.newInputStream(path);
             InputStreamReader reader = new InputStreamReader(is)) {
            return parse(reader);
        } catch (IOException e) {
            logger.error("Ошибка при чтении модели из пути: {}", path, e);
            return null;
        }
    }
    
    /**
     * Загрузить модель из InputStream
     * @param inputStream Поток данных модели
     * @return Данные модели или null при ошибке
     */
    public static ModelData parseFromStream(InputStream inputStream) {
        if (inputStream == null) {
            logger.error("InputStream равен null");
            return null;
        }
        
        try (InputStreamReader reader = new InputStreamReader(inputStream)) {
            return parse(reader);
        } catch (IOException e) {
            logger.error("Ошибка при чтении модели из потока", e);
            return null;
        }
    }
    
    /**
     * Загрузить модель из JSON строки
     * @param jsonString JSON строка модели
     * @return Данные модели или null при ошибке
     */
    public static ModelData parseFromString(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            logger.error("JSON строка пуста");
            return null;
        }
        
        try {
            JsonObject json = gson.fromJson(jsonString, JsonObject.class);
            return parseFromJson(json);
        } catch (Exception e) {
            logger.error("Ошибка при парсинге JSON строки модели", e);
            return null;
        }
    }
    
    /**
     * Парсинг модели из InputStreamReader
     */
    private static ModelData parse(InputStreamReader reader) throws IOException {
        JsonObject json = gson.fromJson(reader, JsonObject.class);
        return parseFromJson(json);
    }
    
    /**
     * Парсинг модели из JsonObject
     */
    private static ModelData parseFromJson(JsonObject json) {
        if (json == null) {
            logger.error("JSON объект равен null");
            return null;
        }
        
        ModelData modelData = new ModelData();
        
        try {
            // Формат версии
            if (json.has("format_version")) {
                modelData.setFormatVersion(json.get("format_version").getAsString());
            }
            
            // Метаданные
            if (json.has("minecraft:geometry")) {
                JsonArray geometryArray = json.getAsJsonArray("minecraft:geometry");
                if (geometryArray.size() > 0) {
                    JsonObject geometry = geometryArray.get(0).getAsJsonObject();
                    parseGeometry(geometry, modelData);
                }
            } else if (json.has("geometry")) {
                // Альтернативный формат (для Blockbench экспорта)
                Object geometry = json.get("geometry");
                if (geometry instanceof JsonArray) {
                    JsonArray geometryArray = (JsonArray) geometry;
                    if (geometryArray.size() > 0) {
                        parseGeometry(geometryArray.get(0).getAsJsonObject(), modelData);
                    }
                } else if (geometry instanceof JsonObject) {
                    parseGeometry((JsonObject) geometry, modelData);
                }
            }
            
            logger.debug("Модель успешно распарсена: {} костей", modelData.getBones().size());
            return modelData;
            
        } catch (Exception e) {
            logger.error("Ошибка при парсинге модели", e);
            return null;
        }
    }
    
    /**
     * Парсинг геометрии из JSON объекта
     */
    private static void parseGeometry(JsonObject geometry, ModelData modelData) {
        if (geometry == null) {
            return;
        }
        
        // Имя геометрии
        if (geometry.has("description")) {
            JsonObject description = geometry.getAsJsonObject("description");
            if (description.has("identifier")) {
                modelData.getMetadata().put("identifier", description.get("identifier").getAsString());
            }
        }
        
        // Кости (bones)
        if (geometry.has("bones")) {
            JsonArray bonesArray = geometry.getAsJsonArray("bones");
            List<ModelData.Bone> bones = new ArrayList<>();
            
            for (JsonElement boneElement : bonesArray) {
                if (boneElement.isJsonObject()) {
                    ModelData.Bone bone = parseBone(boneElement.getAsJsonObject());
                    if (bone != null) {
                        bones.add(bone);
                    }
                }
            }
            
            modelData.setBones(bones);
        }
    }
    
    /**
     * Парсинг кости из JSON объекта
     */
    private static ModelData.Bone parseBone(JsonObject boneJson) {
        if (boneJson == null) {
            return null;
        }
        
        ModelData.Bone bone = new ModelData.Bone();
        
        // Имя кости
        if (boneJson.has("name")) {
            bone.setName(boneJson.get("name").getAsString());
        }
        
        // Родительская кость
        if (boneJson.has("parent")) {
            bone.setParent(boneJson.get("parent").getAsString());
        }
        
        // Точка поворота (pivot)
        if (boneJson.has("pivot")) {
            JsonArray pivotArray = boneJson.getAsJsonArray("pivot");
            if (pivotArray.size() >= 3) {
                double[] pivot = new double[3];
                pivot[0] = pivotArray.get(0).getAsDouble();
                pivot[1] = pivotArray.get(1).getAsDouble();
                pivot[2] = pivotArray.get(2).getAsDouble();
                bone.setPivot(pivot);
            }
        }
        
        // Поворот (rotation)
        if (boneJson.has("rotation")) {
            JsonArray rotationArray = boneJson.getAsJsonArray("rotation");
            if (rotationArray.size() >= 3) {
                double[] rotation = new double[3];
                rotation[0] = rotationArray.get(0).getAsDouble();
                rotation[1] = rotationArray.get(1).getAsDouble();
                rotation[2] = rotationArray.get(2).getAsDouble();
                bone.setRotation(rotation);
            }
        }
        
        // Кубы (cubes/boxes)
        if (boneJson.has("cubes") || boneJson.has("boxes")) {
            JsonArray cubesArray = boneJson.has("cubes") 
                ? boneJson.getAsJsonArray("cubes")
                : boneJson.getAsJsonArray("boxes");
            
            List<ModelData.Cube> cubes = new ArrayList<>();
            
            for (JsonElement cubeElement : cubesArray) {
                if (cubeElement.isJsonObject()) {
                    ModelData.Cube cube = parseCube(cubeElement.getAsJsonObject());
                    if (cube != null) {
                        cubes.add(cube);
                    }
                }
            }
            
            // Сохраняем кубы в свойствах кости
            if (!cubes.isEmpty()) {
                bone.getProperties().put("cubes", cubes);
            }
        }
        
        // Дополнительные свойства
        for (String key : boneJson.keySet()) {
            if (!key.equals("name") && !key.equals("parent") && 
                !key.equals("pivot") && !key.equals("rotation") && 
                !key.equals("cubes") && !key.equals("boxes")) {
                bone.getProperties().put(key, boneJson.get(key));
            }
        }
        
        return bone;
    }
    
    /**
     * Парсинг куба из JSON объекта
     */
    private static ModelData.Cube parseCube(JsonObject cubeJson) {
        if (cubeJson == null) {
            return null;
        }
        
        ModelData.Cube cube = new ModelData.Cube();
        
        // Начало (origin)
        if (cubeJson.has("origin")) {
            JsonArray originArray = cubeJson.getAsJsonArray("origin");
            if (originArray.size() >= 3) {
                double[] origin = new double[3];
                origin[0] = originArray.get(0).getAsDouble();
                origin[1] = originArray.get(1).getAsDouble();
                origin[2] = originArray.get(2).getAsDouble();
                cube.setOrigin(origin);
            }
        } else if (cubeJson.has("pos")) {
            // Альтернативное название
            JsonArray posArray = cubeJson.getAsJsonArray("pos");
            if (posArray.size() >= 3) {
                double[] origin = new double[3];
                origin[0] = posArray.get(0).getAsDouble();
                origin[1] = posArray.get(1).getAsDouble();
                origin[2] = posArray.get(2).getAsDouble();
                cube.setOrigin(origin);
            }
        }
        
        // Размер (size)
        if (cubeJson.has("size")) {
            JsonArray sizeArray = cubeJson.getAsJsonArray("size");
            if (sizeArray.size() >= 3) {
                double[] size = new double[3];
                size[0] = sizeArray.get(0).getAsDouble();
                size[1] = sizeArray.get(1).getAsDouble();
                size[2] = sizeArray.get(2).getAsDouble();
                cube.setSize(size);
            }
        }
        
        // UV координаты
        if (cubeJson.has("uv")) {
            JsonElement uvElement = cubeJson.get("uv");
            if (uvElement.isJsonArray()) {
                // UV как массив [u, v] или [u1, v1, u2, v2]
                JsonArray uvArray = uvElement.getAsJsonArray();
                if (uvArray.size() >= 2) {
                    double[] uv = new double[2];
                    uv[0] = uvArray.get(0).getAsDouble();
                    uv[1] = uvArray.get(1).getAsDouble();
                    cube.setUv(uv);
                }
            } else if (uvElement.isJsonObject()) {
                // UV как объект с координатами для каждой стороны
                // В Blockbench это может быть: {north: [u, v], south: [u, v], ...}
                JsonObject uvObj = uvElement.getAsJsonObject();
                
                // Пытаемся найти UV для передней грани (обычно используется для базовых UV)
                String[] frontKeys = {"north", "front", "up", "top"};
                double[] uv = null;
                
                for (String key : frontKeys) {
                    if (uvObj.has(key)) {
                        JsonElement frontElement = uvObj.get(key);
                        if (frontElement.isJsonArray()) {
                            JsonArray frontUv = frontElement.getAsJsonArray();
                            if (frontUv.size() >= 2) {
                                uv = new double[2];
                                uv[0] = frontUv.get(0).getAsDouble();
                                uv[1] = frontUv.get(1).getAsDouble();
                                cube.setUv(uv);
                                break;
                            }
                        }
                    }
                }
                
                // Если не нашли конкретную грань, используем первую доступную
                if (uv == null && !uvObj.keySet().isEmpty()) {
                    String firstKey = uvObj.keySet().iterator().next();
                    JsonElement firstElement = uvObj.get(firstKey);
                    if (firstElement.isJsonArray()) {
                        JsonArray firstUv = firstElement.getAsJsonArray();
                        if (firstUv.size() >= 2) {
                            uv = new double[2];
                            uv[0] = firstUv.get(0).getAsDouble();
                            uv[1] = firstUv.get(1).getAsDouble();
                            cube.setUv(uv);
                        }
                    }
                }
            }
        } else {
            // Если UV не указаны, устанавливаем значения по умолчанию (0, 0)
            cube.setUv(new double[]{0.0, 0.0});
        }
        
        // Поворот куба
        if (cubeJson.has("rotation")) {
            JsonArray rotationArray = cubeJson.getAsJsonArray("rotation");
            if (rotationArray.size() >= 3) {
                double[] rotation = new double[3];
                rotation[0] = rotationArray.get(0).getAsDouble();
                rotation[1] = rotationArray.get(1).getAsDouble();
                rotation[2] = rotationArray.get(2).getAsDouble();
                cube.setRotation(rotation);
            }
        }
        
        // Зеркалирование
        if (cubeJson.has("mirror")) {
            cube.setMirror(cubeJson.get("mirror").getAsBoolean());
        }
        
        return cube;
    }
    
    /**
     * Валидация модели
     */
    public static boolean validate(ModelData modelData) {
        if (modelData == null) {
            return false;
        }
        
        if (modelData.getBones() == null || modelData.getBones().isEmpty()) {
            logger.warn("Модель не содержит костей");
            return false;
        }
        
        // Проверяем наличие основных костей для модели игрока
        String[] requiredBones = {"head", "body", "left_arm", "right_arm", "left_leg", "right_leg"};
        boolean hasRequired = false;
        for (String boneName : requiredBones) {
            if (modelData.findBone(boneName) != null) {
                hasRequired = true;
                break;
            }
        }
        
        return hasRequired;
    }
}
