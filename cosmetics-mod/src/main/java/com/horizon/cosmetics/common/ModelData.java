package com.horizon.cosmetics.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Общий класс для хранения данных 3D модели
 * Используется как в лаунчере, так и в игровом моде
 * 
 * ВАЖНО: Этот класс должен быть идентичен версии в лаунчере
 * для обеспечения совместимости между компонентами системы
 */
public class ModelData {
    private String formatVersion;
    private Map<String, Object> metadata;
    private List<Bone> bones;
    private Map<String, Object> geometry;
    
    public ModelData() {
        this.bones = new ArrayList<>();
        this.metadata = new HashMap<>();
        this.geometry = new HashMap<>();
    }
    
    /**
     * Кость модели (bone)
     */
    public static class Bone {
        private String name;
        private String parent;
        private double[] pivot;
        private double[] rotation;
        private double[] cubes;
        private Map<String, Object> properties;
        
        public Bone() {
            this.properties = new HashMap<>();
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getParent() {
            return parent;
        }
        
        public void setParent(String parent) {
            this.parent = parent;
        }
        
        public double[] getPivot() {
            return pivot;
        }
        
        public void setPivot(double[] pivot) {
            this.pivot = pivot;
        }
        
        public double[] getRotation() {
            return rotation;
        }
        
        public void setRotation(double[] rotation) {
            this.rotation = rotation;
        }
        
        public double[] getCubes() {
            return cubes;
        }
        
        public void setCubes(double[] cubes) {
            this.cubes = cubes;
        }
        
        public Map<String, Object> getProperties() {
            return properties;
        }
        
        public void setProperties(Map<String, Object> properties) {
            this.properties = properties;
        }
    }
    
    /**
     * Куб (box) модели
     */
    public static class Cube {
        private double[] origin;
        private double[] size;
        private double[] uv;
        private double[] rotation;
        private boolean mirror;
        
        public Cube() {
        }
        
        public Cube(double[] origin, double[] size, double[] uv) {
            this.origin = origin;
            this.size = size;
            this.uv = uv;
            this.mirror = false;
        }
        
        public double[] getOrigin() {
            return origin;
        }
        
        public void setOrigin(double[] origin) {
            this.origin = origin;
        }
        
        public double[] getSize() {
            return size;
        }
        
        public void setSize(double[] size) {
            this.size = size;
        }
        
        public double[] getUv() {
            return uv;
        }
        
        public void setUv(double[] uv) {
            this.uv = uv;
        }
        
        public double[] getRotation() {
            return rotation;
        }
        
        public void setRotation(double[] rotation) {
            this.rotation = rotation;
        }
        
        public boolean isMirror() {
            return mirror;
        }
        
        public void setMirror(boolean mirror) {
            this.mirror = mirror;
        }
    }
    
    public String getFormatVersion() {
        return formatVersion;
    }
    
    public void setFormatVersion(String formatVersion) {
        this.formatVersion = formatVersion;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    public List<Bone> getBones() {
        return bones;
    }
    
    public void setBones(List<Bone> bones) {
        this.bones = bones;
    }
    
    public Map<String, Object> getGeometry() {
        return geometry;
    }
    
    public void setGeometry(Map<String, Object> geometry) {
        this.geometry = geometry;
    }
    
    /**
     * Найти кость по имени
     */
    public Bone findBone(String name) {
        if (name == null || bones == null) {
            return null;
        }
        
        for (Bone bone : bones) {
            if (name.equals(bone.getName())) {
                return bone;
            }
        }
        
        return null;
    }
    
    /**
     * Получить все дочерние кости для указанной родительской кости
     */
    public List<Bone> getChildBones(String parentName) {
        List<Bone> children = new ArrayList<>();
        if (parentName == null || bones == null) {
            return children;
        }
        
        for (Bone bone : bones) {
            if (parentName.equals(bone.getParent())) {
                children.add(bone);
            }
        }
        
        return children;
    }
}
