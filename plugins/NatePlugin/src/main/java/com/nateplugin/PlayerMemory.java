package com.nateplugin;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

public class PlayerMemory {
    private String playerName;
    private Map<String, Integer> interactionTypes;
    private String attitude;
    private int totalInteractions;
    private long lastInteraction;
    
    // Nuevos campos para el sistema mejorado
    private LinkedList<String> recentMessages; // Últimos 5 mensajes
    private String emotion; // empatía, asco, afecto, amabilidad, emoción, etc.
    private String opinion; // Opinión sobre el jugador
    private String behaviorGuidance; // Cómo comportarse con este jugador
    
    public PlayerMemory(String playerName) {
        this.playerName = playerName;
        this.interactionTypes = new HashMap<>();
        this.attitude = "neutral";
        this.totalInteractions = 0;
        this.lastInteraction = System.currentTimeMillis();
        
        // Inicializar nuevos campos
        this.recentMessages = new LinkedList<>();
        this.emotion = "amabilidad"; // Emoción por defecto
        this.opinion = "desconocido"; // Opinión inicial
        this.behaviorGuidance = "Sé amable y curioso"; // Comportamiento inicial
        
        interactionTypes.put("halagos", 0);
        interactionTypes.put("insultos", 0);
        interactionTypes.put("tratos_amables", 0);
        interactionTypes.put("tratos_rudos", 0);
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public Map<String, Integer> getInteractionTypes() {
        return interactionTypes;
    }
    
    public String getAttitude() {
        return attitude;
    }
    
    public void setAttitude(String attitude) {
        this.attitude = attitude;
    }
    
    public int getTotalInteractions() {
        return totalInteractions;
    }
    
    public long getLastInteraction() {
        return lastInteraction;
    }
    
    // Nuevos getters y setters
    public LinkedList<String> getRecentMessages() {
        return recentMessages;
    }
    
    public void addRecentMessage(String message) {
        recentMessages.addLast(message);
        if (recentMessages.size() > 5) {
            recentMessages.removeFirst();
        }
    }
    
    public String getEmotion() {
        return emotion;
    }
    
    public void setEmotion(String emotion) {
        this.emotion = emotion;
    }
    
    public String getOpinion() {
        return opinion;
    }
    
    public void setOpinion(String opinion) {
        this.opinion = opinion;
    }
    
    public String getBehaviorGuidance() {
        return behaviorGuidance;
    }
    
    public void setBehaviorGuidance(String behaviorGuidance) {
        this.behaviorGuidance = behaviorGuidance;
    }
    
    public void incrementInteraction(String type) {
        interactionTypes.put(type, interactionTypes.getOrDefault(type, 0) + 1);
        totalInteractions++;
        lastInteraction = System.currentTimeMillis();
        updateAttitude();
        updateEmotion();
    }
    
    private void updateAttitude() {
        int halagos = interactionTypes.getOrDefault("halagos", 0);
        int insultos = interactionTypes.getOrDefault("insultos", 0);
        int amables = interactionTypes.getOrDefault("tratos_amables", 0);
        int rudos = interactionTypes.getOrDefault("tratos_rudos", 0);
        
        int positive = halagos + amables;
        int negative = insultos + rudos;
        
        if (positive > negative * 2) {
            attitude = "amigable";
            opinion = "jugador amable y positivo";
            behaviorGuidance = "Sé cálido, confiado y muestra aprecio";
        } else if (negative > positive * 2) {
            attitude = "hostil";
            opinion = "jugador problemático y grosero";
            behaviorGuidance = "Mantén distancia, sé firme pero no hostil";
        } else if (positive > negative) {
            attitude = "positiva";
            opinion = "jugador generalmente positivo";
            behaviorGuidance = "Sé amable y receptivo";
        } else if (negative > positive) {
            attitude = "negativa";
            opinion = "jugador con tendencias negativas";
            behaviorGuidance = "Sé cauteloso pero cortés";
        } else {
            attitude = "neutral";
            opinion = "jugador neutro";
            behaviorGuidance = "Sé amable y curioso";
        }
    }
    
    private void updateEmotion() {
        int insultos = interactionTypes.getOrDefault("insultos", 0);
        int halagos = interactionTypes.getOrDefault("halagos", 0);
        
        if (insultos > 3) {
            emotion = "asco";
            behaviorGuidance = "No respondas a groserías, mantén distancia";
        } else if (halagos > 5) {
            emotion = "afecto";
            behaviorGuidance = "Muestra calidez y aprecio genuino";
        } else if (attitude.equals("amigable")) {
            emotion = "empatía";
            behaviorGuidance = "Sé empático y comprensivo";
        } else if (attitude.equals("positiva")) {
            emotion = "emoción";
            behaviorGuidance = "Muestra entusiasmo tímido y curiosidad";
        } else {
            emotion = "amabilidad";
            behaviorGuidance = "Sé amable y curioso";
        }
    }
}
