package com.nateplugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MemoryManager {
    private static MemoryManager instance;
    private Map<UUID, PlayerMemory> playerMemories;
    private Gson gson;
    private File memoryFile;
    
    public MemoryManager() {
        instance = this;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.playerMemories = new HashMap<>();
        this.memoryFile = new File(NatePlugin.getInstance().getDataFolder(), "player_memories.json");
        
        loadMemories();
    }
    
    public static MemoryManager getInstance() {
        return instance;
    }
    
    public PlayerMemory getPlayerMemory(UUID playerUUID) {
        return playerMemories.computeIfAbsent(playerUUID, uuid -> {
            // Cargar desde archivo si existe
            return new PlayerMemory("Unknown");
        });
    }
    
    public PlayerMemory getPlayerMemory(UUID playerUUID, String playerName) {
        return playerMemories.computeIfAbsent(playerUUID, uuid -> new PlayerMemory(playerName));
    }
    
    public void recordInteraction(UUID playerUUID, String playerName, String interactionType) {
        PlayerMemory memory = getPlayerMemory(playerUUID, playerName);
        memory.incrementInteraction(interactionType);
        saveMemories();
    }
    
    public void saveMemories() {
        try {
            if (!memoryFile.exists()) {
                memoryFile.createNewFile();
            }
            
            try (Writer writer = new FileWriter(memoryFile)) {
                gson.toJson(playerMemories, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @SuppressWarnings("unchecked")
    public void loadMemories() {
        if (!memoryFile.exists()) {
            return;
        }
        
        try (Reader reader = new FileReader(memoryFile)) {
            Type type = new TypeToken<Map<UUID, PlayerMemory>>(){}.getType();
            Map<UUID, PlayerMemory> loaded = gson.fromJson(reader, type);
            
            if (loaded != null) {
                playerMemories = loaded;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public Map<UUID, PlayerMemory> getAllMemories() {
        return new HashMap<>(playerMemories);
    }
}
