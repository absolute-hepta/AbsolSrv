package com.nateplugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class SceneManager {
    private static SceneManager instance;
    private final File sceneFile;
    private final Gson gson;
    private SceneState currentScene;

    public SceneManager() {
        instance = this;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.sceneFile = new File(NatePlugin.getInstance().getDataFolder(), "scene.json");
        ensureSceneFile();
        loadScene();
        if (currentScene == null) {
            resetScene();
        }
    }

    public static SceneManager getInstance() {
        return instance;
    }

    public SceneState getCurrentScene() {
        return currentScene;
    }

    public String getCurrentConversation() {
        return currentScene == null ? "general" : currentScene.currentConversation;
    }

    public void resetScene() {
        currentScene = new SceneState(
                "general",
                new ArrayList<>(List.of("general")),
                "Flujo principal del servidor."
        );
        saveScene();
    }

    public void resetScene(String conversation) {
        String normalized = conversation == null || conversation.trim().isEmpty() ? "general" : conversation.trim();
        currentScene = new SceneState(
                normalized,
                new ArrayList<>(List.of(normalized)),
                "La conversación cambió y el flujo fue reiniciado."
        );
        saveScene();
    }

    public void trackConversation(Player player, String message) {
        String detectedConversation = detectConversation(player, message);
        if (currentScene == null || !Objects.equals(currentScene.currentConversation, detectedConversation)) {
            resetScene(detectedConversation);
            return;
        }

        if (currentScene.flow == null) {
            currentScene.flow = new ArrayList<>();
        }

        String flowStep = message == null ? "chat" : "chat:" + message.trim().replaceAll("\\s+", " ").substring(0, Math.min(20, message.trim().length()));
        if (!currentScene.flow.contains(flowStep)) {
            currentScene.flow.add(flowStep);
        }
        currentScene.lastUpdated = System.currentTimeMillis();
        saveScene();
    }

    public void trackSystemEvent(String conversation, String detail) {
        if (currentScene == null || !Objects.equals(currentScene.currentConversation, conversation)) {
            resetScene(conversation);
        }
        currentScene.detail = detail == null ? "Evento del servidor." : detail;
        currentScene.lastUpdated = System.currentTimeMillis();
        saveScene();
    }

    private String detectConversation(Player player, String message) {
        if (message == null) {
            return player != null && (player.isOp() || player.hasPermission("nate.admin")) ? "admin" : "general";
        }

        String normalized = message.toLowerCase(Locale.ROOT);
        if (normalized.contains("nate")) {
            return "directa";
        }
        if (normalized.contains("op") || normalized.contains("admin") || normalized.contains("permiso") || normalized.contains("ban") || normalized.contains("kick")) {
            return "admin";
        }
        if (player != null && (player.isOp() || player.hasPermission("nate.admin"))) {
            return "admin";
        }
        return "general";
    }

    private void ensureSceneFile() {
        File folder = sceneFile.getParentFile();
        if (folder != null && !folder.exists()) {
            folder.mkdirs();
        }
        if (!sceneFile.exists()) {
            try {
                sceneFile.createNewFile();
                try (FileWriter writer = new FileWriter(sceneFile)) {
                    writer.write(gson.toJson(new SceneState("general", new ArrayList<>(List.of("general")), "Flujo principal del servidor.")));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadScene() {
        if (!sceneFile.exists()) {
            return;
        }
        try (FileReader reader = new FileReader(sceneFile)) {
            SceneState loaded = gson.fromJson(reader, SceneState.class);
            if (loaded != null) {
                currentScene = loaded;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveScene() {
        if (currentScene == null) {
            resetScene();
            return;
        }
        try (FileWriter writer = new FileWriter(sceneFile)) {
            gson.toJson(currentScene, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class SceneState {
        @SerializedName("currentConversation")
        public String currentConversation;
        @SerializedName("flow")
        public List<String> flow;
        @SerializedName("detail")
        public String detail;
        @SerializedName("lastUpdated")
        public long lastUpdated;

        public SceneState(String currentConversation, List<String> flow, String detail) {
            this.currentConversation = currentConversation;
            this.flow = flow == null ? new ArrayList<>() : flow;
            this.detail = detail == null ? "" : detail;
            this.lastUpdated = System.currentTimeMillis();
        }
    }
}
