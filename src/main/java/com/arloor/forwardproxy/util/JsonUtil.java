package com.arloor.forwardproxy.util;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

public class JsonUtil {
    private static ObjectMapper MAPPER;

    private JsonUtil() {
    }

    public static <T> T fromJson(String json, Class<T> clazz) throws IOException {
        return MAPPER.readValue(json, clazz);
    }

    public static <T> T fromJson(String json, TypeReference<T> valueTypeRef) throws IOException {
        return MAPPER.readValue(json, valueTypeRef);
    }

    public static <T> List<T> fromJson(String json, Class collection, Class<T> clazz) throws IOException {
        return (List) MAPPER.readValue(json, getCollectionType(MAPPER, collection, clazz));
    }

    public static <T> String toJson(T src) throws IOException {
        return src instanceof String ? (String) src : MAPPER.writeValueAsString(src);
    }

    public static <T> String toJson(T src, Include inclusion) throws IOException {
        if (src instanceof String) {
            return (String) src;
        } else {
            ObjectMapper customMapper = generateMapper(inclusion);
            return customMapper.writeValueAsString(src);
        }
    }

    public static <T> String toJson(T src, ObjectMapper mapper) throws IOException {
        if (null != mapper) {
            return src instanceof String ? (String) src : mapper.writeValueAsString(src);
        } else {
            return null;
        }
    }

    private static ObjectMapper generateMapper(Include include) {
        ObjectMapper customMapper = new ObjectMapper();
        customMapper.setSerializationInclusion(include);
        customMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        customMapper.configure(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS, true);
        customMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        return customMapper;
    }

    private static JavaType getCollectionType(ObjectMapper mapper, Class<?> collectionClass, Class<?>... elementClasses) {
        return mapper.getTypeFactory().constructParametricType(collectionClass, elementClasses);
    }

    static {
        MAPPER = generateMapper(Include.ALWAYS);
    }

    public enum Some {
        A("a", 1), B("b", 2);

        private String name;
        private int id;

        Some(String name, int id) {
            this.name = name;
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public int getId() {
            return id;
        }
    }
}