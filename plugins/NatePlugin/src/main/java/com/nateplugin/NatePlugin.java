package com.nateplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Locale;

public class NatePlugin extends JavaPlugin {
    
    private static NatePlugin instance;
    private boolean nateEnabled = false;
    private String apiKey = "";
    private String model = "meta-llama/llama-3.3-70b-instruct:free";
    private final Map<UUID, BukkitTask> thinkingTasks = new HashMap<>();
    private final Map<UUID, Integer> thinkingProgress = new HashMap<>();
    private final Map<UUID, Boolean> personalNateEnabled = new HashMap<>();
    private boolean thinkingAlertShown = false;
    
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
        new SceneManager();
        
        getCommand("nate").setExecutor(new NateCommand());
        getCommand("nate").setTabCompleter(new NateTabCompleter());
        getCommand("chat").setExecutor(new ChatCommand());
        getCommand("chat").setTabCompleter(new NateTabCompleter());
        
        getServer().getPluginManager().registerEvents(new NateListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(), this);
        getServer().getPluginManager().registerEvents(new ModerationListener(), this);

        String startupSummary = buildPowerStatusSummary();
        getLogger().info(startupSummary);
        getLogger().info("NatePlugin activado correctamente. Programado por Xautral.");
    }
    
    @Override
    public void onDisable() {
        for (BukkitTask task : thinkingTasks.values()) {
            if (task != null) {
                task.cancel();
            }
        }
        thinkingTasks.clear();
        thinkingProgress.clear();
        getLogger().info("NatePlugin desactivado.");
    }
    
    public static NatePlugin getInstance() {
        return instance;
    }
    
    public boolean isAdmin(CommandSender sender) {
        if (sender == null) {
            return true;
        }
        return sender instanceof ConsoleCommandSender || sender.isOp() || sender.hasPermission("nate.admin");
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
    
    public void showThinking(Player player) {
        if (player == null) {
            return;
        }
        stopThinking(player);
        UUID uuid = player.getUniqueId();
        thinkingProgress.put(uuid, 0);
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(this, () -> {
            int current = thinkingProgress.getOrDefault(uuid, 0) + 1;
            if (current > 8) {
                current = 1;
            }
            thinkingProgress.put(uuid, current);
            sendActionBarCompat(player, ChatColor.translateAlternateColorCodes('&', "&7Nate está pensando... &f" + buildProgressBar(current)));
        }, 0L, 3L);
        thinkingTasks.put(uuid, task);
    }
    
    public void stopThinking(Player player) {
        if (player == null) {
            return;
        }
        UUID uuid = player.getUniqueId();
        BukkitTask task = thinkingTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
        thinkingProgress.remove(uuid);
        sendActionBarCompat(player, " ");
    }
    
    private void sendActionBarCompat(Player player, String message) {
        try {
            Player.class.getMethod("sendActionBar", String.class).invoke(player, message);
        } catch (Exception ignored) {
            player.sendMessage(message);
        }
    }
    
    public void broadcastThinkingAlert() {
        if (thinkingAlertShown) {
            return;
        }
        thinkingAlertShown = true;
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&8[!] &7Nate está pensando..."));
        Bukkit.getScheduler().runTaskLater(this, () -> thinkingAlertShown = false, 40L);
    }

    public String buildPowerStatusSummary() {
        StringBuilder summary = new StringBuilder();
        int opCount = 0;
        int normalCount = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isOp() || player.hasPermission("nate.admin")) {
                opCount++;
            } else {
                normalCount++;
            }
        }
        summary.append("Nate status: ")
                .append(opCount).append(" OP / ")
                .append(normalCount).append(" jugadores normales.");
        return summary.toString();
    }

    public String classifyPlayerPower(Player player) {
        if (player == null) {
            return "desconocido";
        }
        if (player.isOp() || player.hasPermission("nate.admin")) {
            return "poderoso";
        }
        return "débil";
    }

    public String getPowerLabel(Player player) {
        return classifyPlayerPower(player).equals("poderoso") ? "OP/administrador" : "jugador normal";
    }

    public boolean isNateEnabledForPlayer(Player player) {
        if (player == null) {
            return true;
        }
        return personalNateEnabled.getOrDefault(player.getUniqueId(), true);
    }

    public void setNateEnabledForPlayer(Player player, boolean enabled) {
        if (player == null) {
            return;
        }
        personalNateEnabled.put(player.getUniqueId(), enabled);
    }

    public void toggleNateForPlayer(Player player) {
        if (player == null) {
            return;
        }
        setNateEnabledForPlayer(player, !isNateEnabledForPlayer(player));
    }

    public boolean isNateMentioned(String message) {
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        return lower.contains("nate") || lower.contains("nateo");
    }

    public String colorNateMentions(String message) {
        if (message == null) {
            return "";
        }
        return message.replaceAll("(?i)\\bNateo?\\b", ChatColor.GOLD + "$0" + ChatColor.WHITE);
    }

    public boolean processAdminBanOrder(Player sender, String message) {
        if (sender == null || !isAdmin(sender) || message == null) {
            return false;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        if (!lower.contains("ban") && !lower.contains("banea") && !lower.contains("banear")) {
            return false;
        }

        String[] words = message.split("\\s+");
        for (String word : words) {
            if (word == null || word.isBlank() || word.equalsIgnoreCase("nate") || word.equalsIgnoreCase("banea") || word.equalsIgnoreCase("ban") || word.equalsIgnoreCase("banear") || word.equalsIgnoreCase("a") || word.equalsIgnoreCase("al") || word.equalsIgnoreCase("alguien") || word.equalsIgnoreCase("a:")) {
                continue;
            }

            Player target = Bukkit.getPlayerExact(word.replaceAll("[.,!?]", ""));
            if (target == null) {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (online.getName().equalsIgnoreCase(word.replaceAll("[.,!?]", ""))) {
                        target = online;
                        break;
                    }
                }
            }

            if (target != null && !isAdmin(target)) {
                String reason = "Orden de baneo del administrador " + sender.getName();
                Bukkit.getBanList(org.bukkit.BanList.Type.NAME).addBan(target.getName(), reason, null, sender.getName());
                target.kickPlayer(ChatColor.translateAlternateColorCodes('&', "&cHas sido baneado por una orden de Nate.\n&7Motivo: &f" + reason));
                Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&c" + target.getName() + " ha sido baneado por orden de " + sender.getName() + "."));
                return true;
            }
        }
        return false;
    }

    public void notifyPlayerJoin(Player player) {
        String label = getPowerLabel(player);
        String message = ChatColor.translateAlternateColorCodes('&', "&7[Nate] &f" + player.getName() + " entró al servidor. Rango: " + label + ".");
        Bukkit.broadcastMessage(message);
        SceneManager.getInstance().trackSystemEvent("join", player.getName() + " se unió al servidor como " + label + ".");
    }

    public void notifyPlayerQuit(Player player) {
        String label = getPowerLabel(player);
        String message = ChatColor.translateAlternateColorCodes('&', "&7[Nate] &f" + player.getName() + " salió del servidor. Rango: " + label + ".");
        Bukkit.broadcastMessage(message);
        SceneManager.getInstance().trackSystemEvent("quit", player.getName() + " abandonó el servidor como " + label + ".");
    }

    private String buildProgressBar(int progress) {
        StringBuilder bar = new StringBuilder();
        for (int i = 1; i <= 8; i++) {
            bar.append(i <= progress ? "█" : "░");
        }
        return bar.toString();
    }
}
