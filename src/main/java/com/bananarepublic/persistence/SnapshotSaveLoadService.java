package com.bananarepublic.persistence;

import com.bananarepublic.engine.GameState;
import com.bananarepublic.exception.SaveLoadException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SnapshotSaveLoadService implements SaveLoadService {
    private static final Pattern PAYLOAD_PATTERN = Pattern.compile("\"payload\"\\s*:\\s*\"([^\"]+)\"");

    @Override
    public void save(GameState state, Path path) {
        Objects.requireNonNull(state, "Game state cannot be null");
        Path targetPath = normalize(path);
        GameSaveData saveData = GameSaveData.fromState(state);

        try {
            if (isSerializedFile(targetPath)) {
                writeSerialized(saveData, targetPath);
            } else {
                writeJson(saveData, targetPath);
            }
        } catch (IOException e) {
            throw new SaveLoadException("Failed to save game to " + targetPath, e);
        }
    }

    @Override
    public GameState load(Path path) {
        Path sourcePath = normalize(path);

        try {
            GameSaveData saveData = isSerializedFile(sourcePath)
                    ? readSerialized(sourcePath)
                    : readJson(sourcePath);
            return saveData.toGameState();
        } catch (IOException e) {
            throw new SaveLoadException("Failed to load game from " + sourcePath, e);
        }
    }

    private void writeSerialized(GameSaveData saveData, Path path) throws IOException {
        ensureParentDirectory(path);
        try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(path))) {
            output.writeObject(saveData);
        }
    }

    private GameSaveData readSerialized(Path path) throws IOException {
        try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(path))) {
            Object restored = input.readObject();
            if (!(restored instanceof GameSaveData saveData)) {
                throw new SaveLoadException("Save file does not contain a valid game snapshot");
            }
            return saveData;
        } catch (ClassNotFoundException e) {
            throw new SaveLoadException("Failed to deserialize save data", e);
        }
    }

    private void writeJson(GameSaveData saveData, Path path) throws IOException {
        ensureParentDirectory(path);

        byte[] serializedPayload = serialize(saveData);
        Map<String, Object> summary = new LinkedHashMap<>(saveData.toSummaryMap());
        summary.put("payload", Base64.getEncoder().encodeToString(serializedPayload));

        Files.writeString(path, writeJsonValue(summary, 0) + System.lineSeparator(), StandardCharsets.UTF_8);
    }

    private GameSaveData readJson(Path path) throws IOException {
        String json = Files.readString(path, StandardCharsets.UTF_8);
        Matcher matcher = PAYLOAD_PATTERN.matcher(json);

        if (!matcher.find()) {
            throw new SaveLoadException("JSON save file is missing the payload field");
        }

        byte[] payload = Base64.getDecoder().decode(matcher.group(1));
        return deserialize(payload);
    }

    private byte[] serialize(GameSaveData saveData) {
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream();
             ObjectOutputStream output = new ObjectOutputStream(buffer)) {
            output.writeObject(saveData);
            output.flush();
            return buffer.toByteArray();
        } catch (IOException e) {
            throw new SaveLoadException("Failed to serialize save data", e);
        }
    }

    private GameSaveData deserialize(byte[] bytes) {
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            Object restored = input.readObject();
            if (!(restored instanceof GameSaveData saveData)) {
                throw new SaveLoadException("Save payload does not contain a valid game snapshot");
            }
            return saveData;
        } catch (IOException | ClassNotFoundException e) {
            throw new SaveLoadException("Failed to deserialize save payload", e);
        }
    }

    private String writeJsonValue(Object value, int indentLevel) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String stringValue) {
            return "\"" + escapeJson(stringValue) + "\"";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof Enum<?> enumValue) {
            return writeJsonValue(enumValue.name(), indentLevel);
        }
        if (value instanceof Map<?, ?> mapValue) {
            return writeJsonObject(mapValue, indentLevel);
        }
        if (value instanceof Collection<?> collectionValue) {
            return writeJsonArray(collectionValue, indentLevel);
        }

        throw new SaveLoadException("Unsupported JSON value: " + value.getClass().getName());
    }

    private String writeJsonObject(Map<?, ?> mapValue, int indentLevel) {
        if (mapValue.isEmpty()) {
            return "{}";
        }

        StringBuilder json = new StringBuilder("{\n");
        int index = 0;

        for (Map.Entry<?, ?> entry : mapValue.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw new SaveLoadException("JSON object keys must be strings");
            }

            json.append(indent(indentLevel + 1))
                    .append("\"")
                    .append(escapeJson(key))
                    .append("\": ")
                    .append(writeJsonValue(entry.getValue(), indentLevel + 1));

            if (index < mapValue.size() - 1) {
                json.append(",");
            }
            json.append("\n");
            index++;
        }

        json.append(indent(indentLevel)).append("}");
        return json.toString();
    }

    private String writeJsonArray(Collection<?> collectionValue, int indentLevel) {
        if (collectionValue.isEmpty()) {
            return "[]";
        }

        StringBuilder json = new StringBuilder("[\n");
        int index = 0;

        for (Object element : collectionValue) {
            json.append(indent(indentLevel + 1))
                    .append(writeJsonValue(element, indentLevel + 1));

            if (index < collectionValue.size() - 1) {
                json.append(",");
            }
            json.append("\n");
            index++;
        }

        json.append(indent(indentLevel)).append("]");
        return json.toString();
    }

    private String indent(int indentLevel) {
        return "  ".repeat(Math.max(indentLevel, 0));
    }

    private String escapeJson(String value) {
        StringBuilder escaped = new StringBuilder();

        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }

        return escaped.toString();
    }

    private void ensureParentDirectory(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private boolean isSerializedFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        return fileName.endsWith(".ser");
    }

    private Path normalize(Path path) {
        return Objects.requireNonNull(path, "Path cannot be null").toAbsolutePath().normalize();
    }
}
