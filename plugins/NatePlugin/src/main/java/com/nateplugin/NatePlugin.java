package com.nateplugin;

import org.bukkit.plugin.java.JavaPlugin;

public class NatePlugin extends JavaPlugin {
    
    private static NatePlugin instance;
    private boolean nateEnabled = false;
    private String apiKey = "";
    private String model = "meta-llama/llama-3.3-70b-instruct:free";
    
    @Override
    public void onEnable() {
        instance = this;
        
        saveDefaultConfig();
        
        nateEnabled = getConfig().getBoolean("nate.enabled", false);
        apiKey = getConfig().getString("nate.apikey", "");
        model = getConfig().getString("nate.model", "meta-llama/llama-3.3-70b-instruct:free");
        
        new MemoryManager();
        new OpenAIManager();
        new SuggestionManager();
        
        getCommand("nate").setExecutor(new NateCommand());
        getCommand("nate").setTabCompleter(new NateTabCompleter());
        getCommand("chat").setExecutor(new ChatCommand());
        getCommand("chat").setTabCompleter(new NateTabCompleter());
        
        getServer().getPluginManager().registerEvents(new NateListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(), this);
        
        getLogger().info("NatePlugin ha sido activado");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("NatePlugin ha sido desactivado");
    }
    
    public static NatePlugin getInstance() {
        return instance;
    }
    
    public boolean isNateEnabled() {
        return nateEnabled;
    }
    
    public void setNateEnabled(boolean enabled) {
        this.nateEnabled = enabled;
        getConfig().set("nate.enabled", enabled);
        saveConfig();
    }
    
    public String getApiKey() {
        return apiKey;
    }
    
    public void setApiKey(String key) {
        this.apiKey = key;
        getConfig().set("nate.apikey", key);
        saveConfig();
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
        getConfig().set("nate.model", model);
        saveConfig();
    }
}
