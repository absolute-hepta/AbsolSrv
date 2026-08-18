package com.permadeathmode;

import org.bukkit.*;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.*;

public class PermadeathMode extends JavaPlugin {

    private static PermadeathMode instance;
    private boolean permadeathEnabled = false;
    private boolean autoWeatherEnabled = false;
    private final Map<UUID, Location> pendingRevives = new HashMap<>();
    private final Map<Integer, Long> weatherTimers = new HashMap<>();  // taskId -> start time
    private final Map<UUID, Integer> reviveCounts = new HashMap<>();  // playerId -> revive count
    private final Map<UUID, Integer> headCounts = new HashMap<>();  // playerId -> head count
    private String language = "es";  // Default language

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        permadeathEnabled = getConfig().getBoolean("permadeath.enabled", false);
        autoWeatherEnabled = getConfig().getBoolean("permadeath.auto-weather", false);
        language = getConfig().getString("language", "es");
        
        // Load statistics
        loadStatistics();

        getCommand("permadeath").setExecutor(new PermadeathCommand());
        getCommand("permadeath").setTabCompleter(new PermadeathTabCompleter());
        getServer().getPluginManager().registerEvents(new PermadeathListener(), this);
        getLogger().info("PermadeathMode activado.");
    }

    public static PermadeathMode getInstance() {
        return instance;
    }

    public boolean isPermadeathEnabled() {
        return permadeathEnabled;
    }

    public void setPermadeathEnabled(boolean enabled) {
        this.permadeathEnabled = enabled;
        getConfig().set("permadeath.enabled", enabled);
        saveConfig();
    }

    public void togglePermadeath() {
        setPermadeathEnabled(!permadeathEnabled);
    }

    public void triggerToggleSound(Player player) {
        if (player == null) {
            return;
        }
        player.playSound(player.getLocation(), Sound.ENTITY_SKELETON_HORSE_DEATH, 1.0F, 0.0F);
        player.playSound(player.getLocation(), Sound.ENTITY_SKELETON_HORSE_DEATH, 1.0F, 1.0F);
    }
    
    public void triggerToggleSoundAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            triggerToggleSound(player);
        }
    }

    public void triggerDeathSound(Player player) {
        if (player == null) {
            return;
        }
        player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 1.0F, 2.0F);
        player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 1.0F, 0.0F);
    }

    public void showToggleTitle(Player player, boolean on) {
        if (player == null) {
            return;
        }
        String title = "§l" + getMessage(on ? "permadeath.toggle_on" : "permadeath.toggle_off");
        String subtitle = "§7" + getMessage(on ? "permadeath.death_permanent" : "permadeath.back_normal");
        player.sendTitle(title, subtitle, 10, 40, 15);
    }

    public void handleToggle(CommandSender sender, boolean on) {
        setPermadeathEnabled(on);
        if (sender instanceof Player player) {
            triggerToggleSound(player);
            showToggleTitle(player, on);
        }
        Bukkit.broadcastMessage(ChatColor.GOLD + "[Permadeath] §7" + getMessage(on ? "permadeath.activated" : "permadeath.deactivated"));
    }

    public void handleDeath(Player player, String cause, Player killer) {
        if (!permadeathEnabled || player == null) {
            return;
        }

        Location deathLocation = player.getLocation().clone();
        String killerName = killer != null ? killer.getName() : "Mundo";
        String cleanCause = cause == null || cause.trim().isEmpty() ? "desconocida" : cause.trim();

        pendingRevives.put(player.getUniqueId(), deathLocation.clone());

        player.setGameMode(GameMode.SPECTATOR);
        player.sendTitle("§l" + getMessage("permadeath.you_died"), "§7" + getMessage("permadeath.active"), 10, 60, 20);
        triggerDeathSound(player);
        spawnDeathChest(player, deathLocation, cleanCause, killerName);

        String coordText = "X: " + Math.round(deathLocation.getX()) + " Y: " + Math.round(deathLocation.getY()) + " Z: " + Math.round(deathLocation.getZ());
        String kickMessage = ChatColor.GRAY + "" + ChatColor.BOLD + getMessage("permadeath.you_died") + ".\n"
                + ChatColor.GRAY + "Causa: " + ChatColor.YELLOW + cleanCause + "\n"
                + ChatColor.GRAY + "Victimario: " + ChatColor.YELLOW + killerName + "\n"
                + ChatColor.GRAY + "Coordenadas: " + ChatColor.YELLOW + coordText;

        Bukkit.broadcastMessage(ChatColor.GOLD + "[Permadeath] " + ChatColor.YELLOW + player.getName() + ChatColor.GRAY + " " + getMessage("permadeath.died") + " " + ChatColor.YELLOW + coordText);
        Bukkit.getScheduler().scheduleSyncDelayedTask(getInstance(), () -> player.kickPlayer(kickMessage), 5L);
    }

    private void spawnDeathChest(Player player, Location deathLocation, String cause, String killerName) {
        World world = deathLocation.getWorld();
        if (world == null) {
            return;
        }

        // Contar items totales para decidir si crear un doble cofre
        int totalItems = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                totalItems++;
            }
        }
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor != null && armor.getType() != Material.AIR) {
                totalItems++;
            }
        }
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand != null && offhand.getType() != Material.AIR) {
            totalItems++;
        }
        totalItems += 11; // Cabeza + libro + XP bottles
        
        boolean needsDoubleChest = totalItems > 27;
        
        Location chestLocation1 = deathLocation.clone().add(0.5, 1.0, 0.5);
        Location chestLocation2 = needsDoubleChest ? chestLocation1.clone().add(1.0, 0, 0) : null;
        
        Block block1 = world.getBlockAt(chestLocation1);
        block1.setType(Material.CHEST);
        
        Block block2 = null;
        if (needsDoubleChest && chestLocation2 != null) {
            block2 = world.getBlockAt(chestLocation2);
            block2.setType(Material.CHEST);
        }

        if (block1.getState() instanceof Chest) {
            Chest chest1 = (Chest) block1.getState();
            Inventory inventory1 = chest1.getInventory();
            
            // Cabeza del jugador muerto en el primer cofre
            ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta headMeta = playerHead.getItemMeta();
            if (headMeta != null) {
                headMeta.setDisplayName("§c§l" + player.getName() + "'s Head");
                List<String> headLore = new ArrayList<>();
                headLore.add("§7" + getMessage("head.lore"));
                headMeta.setLore(headLore);
                playerHead.setItemMeta(headMeta);
            }
            inventory1.setItem(0, playerHead);
            
            // Libro de reviva en el segundo slot
            ItemStack reviveBook = createReviveBook(deathLocation, cause, killerName);
            inventory1.setItem(1, reviveBook);

            // Agregar inventario del jugador al primer cofre
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.getType() != Material.AIR) {
                    Map<Integer, ItemStack> overflow = inventory1.addItem(item);
                    // Si no cabe en el primer cofre y hay segundo, intentar ahí
                    if (!overflow.isEmpty() && needsDoubleChest && block2 != null && block2.getState() instanceof Chest) {
                        Chest chest2 = (Chest) block2.getState();
                        for (ItemStack overflowItem : overflow.values()) {
                            chest2.getInventory().addItem(overflowItem);
                        }
                    }
                }
            }

            for (ItemStack armor : player.getInventory().getArmorContents()) {
                if (armor != null && armor.getType() != Material.AIR) {
                    Map<Integer, ItemStack> overflow = inventory1.addItem(armor);
                    if (!overflow.isEmpty() && needsDoubleChest && block2 != null && block2.getState() instanceof Chest) {
                        Chest chest2 = (Chest) block2.getState();
                        for (ItemStack overflowItem : overflow.values()) {
                            chest2.getInventory().addItem(overflowItem);
                        }
                    }
                }
            }

            if (offhand != null && offhand.getType() != Material.AIR) {
                Map<Integer, ItemStack> overflow = inventory1.addItem(offhand);
                if (!overflow.isEmpty() && needsDoubleChest && block2 != null && block2.getState() instanceof Chest) {
                    Chest chest2 = (Chest) block2.getState();
                    for (ItemStack overflowItem : overflow.values()) {
                        chest2.getInventory().addItem(overflowItem);
                    }
                }
            }

            // Agregar botellas de experiencia
            int expLevel = player.getLevel();
            int expBottles = Math.max(1, expLevel / 10);
            for (int i = 0; i < expBottles && i < 10; i++) {
                Map<Integer, ItemStack> overflow = inventory1.addItem(new ItemStack(Material.EXPERIENCE_BOTTLE));
                if (!overflow.isEmpty() && needsDoubleChest && block2 != null && block2.getState() instanceof Chest) {
                    Chest chest2 = (Chest) block2.getState();
                    for (ItemStack overflowItem : overflow.values()) {
                        chest2.getInventory().addItem(overflowItem);
                    }
                }
            }
        }
    }

    public ItemStack createReviveBook(Location location, String cause, String killerName) {
        ItemStack book = new ItemStack(Material.BOOK);
        var meta = book.getItemMeta();
        
        if (meta == null) {
            return book;
        }
        
        meta.setDisplayName("§b§l" + getMessage("book.display_name"));
        List<String> lore = new ArrayList<>();
        lore.add("§c§l" + getMessage("book.click_to_revive"));
        lore.add(" ");
        lore.add("§7" + getMessage("book.location") + " §e" + Math.round(location.getX()) + ", " + Math.round(location.getY()) + ", " + Math.round(location.getZ()));
        lore.add("§7" + getMessage("book.cause") + " §c" + cause);
        lore.add("§7" + getMessage("book.killer") + " §6" + killerName);
        lore.add(" ");
        lore.add("§8[MARCA_REVIVE]");  // Marca especial para detección confiable
        meta.setLore(lore);
        
        // Agregar encantamiento invisible para que brille
        if (meta instanceof BookMeta) {
            BookMeta bookMeta = (BookMeta) meta;
            bookMeta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 0, true);
            bookMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        
        book.setItemMeta(meta);
        return book;
    }

    public boolean isReviveBook(ItemStack item) {
        if (item == null || item.getType() != Material.BOOK) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        
        // Revisar el display name
        String displayName = meta.getDisplayName();
        if (displayName != null && displayName.contains("Libro de Resurrección")) {
            return true;
        }
        
        // Revisar marca en el lore
        List<String> lore = meta.getLore();
        if (lore != null) {
            for (String line : lore) {
                if (line != null && line.contains("[MARCA_REVIVE]")) {
                    return true;
                }
            }
        }
        
        return false;
    }

    public void revivePlayer(Player player, Player reviver) {
        if (player == null) {
            return;
        }

        UUID uuid = player.getUniqueId();
        Location deathLocation = pendingRevives.remove(uuid);
        if (deathLocation == null) {
            return;
        }

        player.setGameMode(GameMode.SURVIVAL);
        player.teleport(deathLocation.clone().add(0.5, 1.0, 0.5));
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20.0F);
        
        // 3 sonidos de portal del end en pitch 0, 1 y 2
        player.playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 1.0F, 0.0F);
        player.playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 1.0F, 1.0F);
        player.playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 1.0F, 2.0F);
        
        player.sendTitle("\u00a7a\u00a7l" + getMessage("permadeath.revived_title"), "\u00a77" + getMessage("permadeath.back_to_life"), 5, 40, 10);
        player.sendMessage("\u00a7a[Permadeath] \u00a77" + getMessage("permadeath.back_to_life_msg"));
        
        // Broadcast a todos los jugadores quien revivio al jugador
        String reviverName = reviver != null ? reviver.getName() : "Sistema";
        Bukkit.broadcastMessage(ChatColor.GOLD + "[Permadeath] " + ChatColor.YELLOW + reviverName + ChatColor.GRAY + " " + getMessage("permadeath.revived") + " " + ChatColor.YELLOW + player.getName());
        
        // Crear particulas negras de resurrección
        spawnResurrectionParticles(player.getLocation(), player);
        
        // Increment revive count and check achievements
        if (reviver != null) {
            incrementReviveCount(reviver.getUniqueId());
            checkReviveAchievements(reviver);
        }
    }
    
    private void spawnResurrectionParticles(Location location, Player player) {
        World world = location.getWorld();
        if (world == null) return;
        
        // Partículas negras subiendo desde el piso
        for (int i = 0; i < 30; i++) {
            double x = location.getX() + (Math.random() - 0.5) * 1.5;
            double z = location.getZ() + (Math.random() - 0.5) * 1.5;
            double y = location.getY() + (i * 0.1);
            world.spawnParticle(Particle.ASH, x, y, z, 1, 0, 0, 0, 0.1);
        }
        
        // Explosión de partículas negras en esfera después de 0.5 segundos
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
            double radius = 2.0;
            int particleCount = 50;
            for (int i = 0; i < particleCount; i++) {
                double angle = (2 * Math.PI * i) / particleCount;
                double x = location.getX() + Math.cos(angle) * radius;
                double z = location.getZ() + Math.sin(angle) * radius;
                double y = location.getY() + 1.0;
                world.spawnParticle(Particle.ASH, x, y, z, 3, Math.random() - 0.5, Math.random() - 0.5, Math.random() - 0.5, 0.15);
            }
        }, 10L);
    }

    public boolean hasPendingRevive(UUID uuid) {
        return pendingRevives.containsKey(uuid);
    }
    
    public Location getDeathLocation(UUID uuid) {
        return pendingRevives.get(uuid);
    }

    public void setModeByWeather(boolean isThundering) {
        if (isThundering && autoWeatherEnabled) {
            setPermadeathEnabled(true);
            triggerToggleSoundAll();
            Bukkit.broadcastMessage(ChatColor.GOLD + "[Permadeath] " + ChatColor.GRAY + getMessage("permadeath.enabled_weather"));
            
            // Obtener duración real de la tormenta en ticks y convertir a segundos
            World world = Bukkit.getWorlds().get(0);
            int thunderDurationTicks = world.getThunderDuration();
            int stormDuration = thunderDurationTicks / 20; // Convertir ticks a segundos
            
            // Iniciar temporizador en actionbar mostrando tiempo RESTANTE
            final int[] taskId = new int[1];
            taskId[0] = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
                long startTime = weatherTimers.getOrDefault(taskId[0], System.currentTimeMillis());
                long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
                long remainingSeconds = Math.max(0, stormDuration - elapsedSeconds);
                
                for (Player player : Bukkit.getOnlinePlayers()) {
                    String actionBarText = ChatColor.GOLD + "[" + getMessage("storm") + "] " + ChatColor.YELLOW + remainingSeconds + "s";
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(actionBarText));
                }
            }, 0, 20);
            weatherTimers.put(taskId[0], System.currentTimeMillis());
        } else if (!isThundering && autoWeatherEnabled) {
            setPermadeathEnabled(false);
            triggerToggleSoundAll();
            Bukkit.broadcastMessage(ChatColor.GOLD + "[Permadeath] " + ChatColor.GRAY + getMessage("permadeath.disabled_weather"));
            
            // Cancelar temporizadores
            for (Integer taskId : new ArrayList<>(weatherTimers.keySet())) {
                Bukkit.getScheduler().cancelTask(taskId);
                weatherTimers.remove(taskId);
            }
            
            // Limpiar actionbars
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));
            }
        }
    }
    
    public boolean isAutoWeatherEnabled() {
        return autoWeatherEnabled;
    }
    
    public void setAutoWeatherEnabled(boolean enabled) {
        this.autoWeatherEnabled = enabled;
        getConfig().set("permadeath.auto-weather", enabled);
        saveConfig();
    }
    
    // Language system
    public String getLanguage() {
        return language;
    }
    
    public void setLanguage(String lang) {
        this.language = lang;
        getConfig().set("language", lang);
        saveConfig();
    }
    
    public String getMessage(String key) {
        Map<String, Map<String, String>> messages = new HashMap<>();
        
        // Spanish messages
        Map<String, String> es = new HashMap<>();
        es.put("permadeath.activated", "Modo permadeath activado.");
        es.put("permadeath.deactivated", "Modo permadeath desactivado.");
        es.put("permadeath.enabled_weather", "Modo permadeath ACTIVADO por tormenta.");
        es.put("permadeath.disabled_weather", "Modo permadeath DESACTIVADO la tormenta pasó.");
        es.put("permadeath.active_warning", "El modo permadeath está activado. Si mueres, serás expulsado del servidor.");
        es.put("permadeath.attention", "¡ATENCIÓN!");
        es.put("permadeath.active_on_server", "El modo permadeath está ACTIVO en este servidor");
        es.put("permadeath.died", "murió en");
        es.put("permadeath.revived", "revivio a");
        es.put("permadeath.you_died", "Has muerto");
        es.put("permadeath.active", "El modo permadeath está activo");
        es.put("permadeath.back_normal", "Has vuelto al modo normal");
        es.put("permadeath.death_permanent", "La muerte será permanente");
        es.put("permadeath.revived_title", "¡REVIVIDO!");
        es.put("permadeath.back_to_life", "Volviste a la vida en el lugar de tu muerte");
        es.put("permadeath.back_to_life_msg", "Has vuelto a la vida.");
        es.put("permadeath.dead_title", "Estás muerto");
        es.put("permadeath.get_book", "Recoge el libro dorado de la caja y haz clic derecho");
        es.put("permadeath.died_at", "Moriste en:");
        es.put("permadeath.copy_teleport", "(copiar para teleportarse)");
        es.put("permadeath.toggle_on", "Permadeath activado");
        es.put("permadeath.toggle_off", "Permadeath desactivado");
        es.put("storm", "Tormenta");
        es.put("achievement.nigromante", "¡Logro desbloqueado: Nigromante!");
        es.put("achievement.necrokinesis", "¡Logro desbloqueado: Necrokinesis!");
        es.put("achievement.sadico", "¡Logro desbloqueado: Sádico!");
        es.put("achievement.sssadico", "¡Logro desbloqueado: SSSádico!");
        es.put("book.display_name", "✦ Libro de Resurrección ✦");
        es.put("book.click_to_revive", ">> CLICK DERECHO PARA REVIVIR <<");
        es.put("book.location", "Ubicación:");
        es.put("book.cause", "Causa:");
        es.put("book.killer", "Victimario:");
        es.put("head.lore", "El cráneo del difunto");
        
        // English messages
        Map<String, String> en = new HashMap<>();
        en.put("permadeath.activated", "Permadeath mode activated.");
        en.put("permadeath.deactivated", "Permadeath mode deactivated.");
        en.put("permadeath.enabled_weather", "Permadeath mode ACTIVATED by storm.");
        en.put("permadeath.disabled_weather", "Permadeath mode DEACTIVATED storm passed.");
        en.put("permadeath.active_warning", "Permadeath mode is active. If you die, you will be kicked from the server.");
        en.put("permadeath.attention", "ATTENTION!");
        en.put("permadeath.active_on_server", "Permadeath mode is ACTIVE on this server");
        en.put("permadeath.died", "died at");
        en.put("permadeath.revived", "revived");
        en.put("permadeath.you_died", "You died");
        en.put("permadeath.active", "Permadeath mode is active");
        en.put("permadeath.back_normal", "You are back to normal mode");
        en.put("permadeath.death_permanent", "Death will be permanent");
        en.put("permadeath.revived_title", "REVIVED!");
        en.put("permadeath.back_to_life", "You came back to life at the place of your death");
        en.put("permadeath.back_to_life_msg", "You have come back to life.");
        en.put("permadeath.dead_title", "You are dead");
        en.put("permadeath.get_book", "Get the golden book from the chest and right-click");
        en.put("permadeath.died_at", "You died at:");
        en.put("permadeath.copy_teleport", "(copy to teleport)");
        en.put("permadeath.toggle_on", "Permadeath activated");
        en.put("permadeath.toggle_off", "Permadeath deactivated");
        en.put("storm", "Storm");
        en.put("achievement.nigromante", "Achievement unlocked: Necromancer!");
        en.put("achievement.necrokinesis", "Achievement unlocked: Necrokinesis!");
        en.put("achievement.sadico", "Achievement unlocked: Sadist!");
        en.put("achievement.sssadico", "Achievement unlocked: SSSadist!");
        en.put("book.display_name", "✦ Resurrection Book ✦");
        en.put("book.click_to_revive", ">> RIGHT CLICK TO REVIVE <<");
        en.put("book.location", "Location:");
        en.put("book.cause", "Cause:");
        en.put("book.killer", "Killer:");
        en.put("head.lore", "The skull of the deceased");
        
        messages.put("es", es);
        messages.put("en", en);
        
        Map<String, String> langMessages = messages.getOrDefault(language, messages.get("es"));
        return langMessages.getOrDefault(key, key);
    }
    
    // Statistics management
    private void loadStatistics() {
        if (getConfig().contains("statistics.reviveCounts")) {
            for (String uuidStr : getConfig().getConfigurationSection("statistics.reviveCounts").getKeys(false)) {
                UUID uuid = UUID.fromString(uuidStr);
                reviveCounts.put(uuid, getConfig().getInt("statistics.reviveCounts." + uuidStr));
            }
        }
        if (getConfig().contains("statistics.headCounts")) {
            for (String uuidStr : getConfig().getConfigurationSection("statistics.headCounts").getKeys(false)) {
                UUID uuid = UUID.fromString(uuidStr);
                headCounts.put(uuid, getConfig().getInt("statistics.headCounts." + uuidStr));
            }
        }
    }
    
    private void saveStatistics() {
        for (Map.Entry<UUID, Integer> entry : reviveCounts.entrySet()) {
            getConfig().set("statistics.reviveCounts." + entry.getKey().toString(), entry.getValue());
        }
        for (Map.Entry<UUID, Integer> entry : headCounts.entrySet()) {
            getConfig().set("statistics.headCounts." + entry.getKey().toString(), entry.getValue());
        }
        saveConfig();
    }
    
    public int getReviveCount(UUID uuid) {
        return reviveCounts.getOrDefault(uuid, 0);
    }
    
    public int getHeadCount(UUID uuid) {
        return headCounts.getOrDefault(uuid, 0);
    }
    
    public void incrementReviveCount(UUID uuid) {
        reviveCounts.put(uuid, reviveCounts.getOrDefault(uuid, 0) + 1);
        saveStatistics();
    }
    
    public void incrementHeadCount(UUID uuid) {
        headCounts.put(uuid, headCounts.getOrDefault(uuid, 0) + 1);
        saveStatistics();
    }
    
    // Achievement system
    private void checkReviveAchievements(Player player) {
        UUID uuid = player.getUniqueId();
        int count = getReviveCount(uuid);
        
        if (count == 1) {
            grantAchievement(player, "nigromante");
        } else if (count == 10) {
            grantAchievement(player, "necrokinesis");
        }
    }
    
    public void checkHeadAchievements(Player player) {
        UUID uuid = player.getUniqueId();
        int count = getHeadCount(uuid);
        
        if (count == 1) {
            grantAchievement(player, "sadico");
        } else if (count == 10) {
            grantAchievement(player, "sssadico");
        }
    }
    
    private void grantAchievement(Player player, String achievementKey) {
        String message = getMessage("achievement." + achievementKey);
        player.sendMessage(ChatColor.GOLD + "[Permadeath] " + ChatColor.YELLOW + message);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
        
        // Try to grant actual Minecraft advancement if exists
        try {
            NamespacedKey advancementKey = new NamespacedKey(this, achievementKey);
            Advancement advancement = Bukkit.getAdvancement(advancementKey);
            if (advancement != null) {
                AdvancementProgress progress = player.getAdvancementProgress(advancement);
                if (!progress.isDone()) {
                    progress.awardCriteria("trigger");
                }
            }
        } catch (Exception e) {
            // Advancement doesn't exist, just show message
        }
    }
}
