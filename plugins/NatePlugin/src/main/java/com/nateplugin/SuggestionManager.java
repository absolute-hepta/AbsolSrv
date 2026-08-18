package com.nateplugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SuggestionManager {
    private static SuggestionManager instance;
    private List<Suggestion> suggestions;
    private Gson gson;
    private File suggestionsFile;
    
    public SuggestionManager() {
        instance = this;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.suggestions = new ArrayList<>();
        this.suggestionsFile = new File(NatePlugin.getInstance().getDataFolder(), "suggestions.json");
        
        loadSuggestions();
    }
    
    public static SuggestionManager getInstance() {
        return instance;
    }
    
    public void addSuggestion(String playerName, String suggestion) {
        Suggestion newSuggestion = new Suggestion(playerName, suggestion, System.currentTimeMillis());
        suggestions.add(newSuggestion);
        saveSuggestions();
    }
    
    public List<Suggestion> getSuggestions() {
        return new ArrayList<>(suggestions);
    }
    
    public void saveSuggestions() {
        try {
            if (!suggestionsFile.exists()) {
                suggestionsFile.createNewFile();
            }
            
            try (Writer writer = new FileWriter(suggestionsFile)) {
                gson.toJson(suggestions, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @SuppressWarnings("unchecked")
    public void loadSuggestions() {
        if (!suggestionsFile.exists()) {
            return;
        }
        
        try (Reader reader = new FileReader(suggestionsFile)) {
            Type type = new TypeToken<List<Suggestion>>(){}.getType();
            List<Suggestion> loaded = gson.fromJson(reader, type);
            
            if (loaded != null) {
                suggestions = loaded;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static class Suggestion {
        private String playerName;
        private String suggestion;
        private long timestamp;
        
        public Suggestion(String playerName, String suggestion, long timestamp) {
            this.playerName = playerName;
            this.suggestion = suggestion;
            this.timestamp = timestamp;
        }
        
        public String getPlayerName() {
            return playerName;
        }
        
        public String getSuggestion() {
            return suggestion;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
    }
}
