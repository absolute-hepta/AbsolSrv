package com.nateplugin;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class OpenAIManager {
    private static OpenAIManager instance;
    private OkHttpClient client;
    private Gson gson;
    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final String MODELS_URL = "https://openrouter.ai/api/v1/models";
    
    public OpenAIManager() {
        instance = this;
        this.client = new OkHttpClient();
        this.gson = new Gson();
    }
    
    public static OpenAIManager getInstance() {
        return instance;
    }
    
    public CompletableFuture<List<String>> getAvailableModels() {
        CompletableFuture<List<String>> future = new CompletableFuture<>();
        
        new Thread(() -> {
            try {
                List<String> models = fetchAvailableModels();
                future.complete(models);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        }).start();
        
        return future;
    }
    
    private List<String> fetchAvailableModels() throws IOException {
        Request request = new Request.Builder()
                .url(MODELS_URL)
                .addHeader("Authorization", "Bearer " + NatePlugin.getInstance().getApiKey())
                .addHeader("HTTP-Referer", "https://minecraft-server.com") // OpenRouter requirement
                .addHeader("X-Title", "Nate Minecraft Plugin")
                .get()
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body().string();
                throw new IOException("Error al obtener modelos: " + response.code() + " " + response.message() + " - " + errorBody);
            }
            
            String responseBody = response.body().string();
            
            try {
                JsonObject responseJson = gson.fromJson(responseBody, JsonObject.class);
                
                if (!responseJson.has("data")) {
                    throw new IOException("La respuesta no contiene el campo 'data'. Respuesta: " + responseBody);
                }
                
                // Filtrar y ordenar modelos: gratuitos primero, luego de pago
                List<ModelInfo> allModels = new ArrayList<>();
                
                for (com.google.gson.JsonElement jsonElement : responseJson.getAsJsonArray("data")) {
                    try {
                        JsonObject modelObj = jsonElement.getAsJsonObject();
                        String id = modelObj.get("id").getAsString();
                        String name = modelObj.has("name") ? modelObj.get("name").getAsString() : id;
                        
                        // Determinar si es gratuito basándonos en múltiples criterios
                        boolean isFree = false;
                        
                        // Criterio 1: ID contiene ":free"
                        if (id.contains(":free")) {
                            isFree = true;
                        }
                        
                        // Criterio 2: Pricing es 0
                        if (modelObj.has("pricing")) {
                            try {
                                JsonObject pricingObj = modelObj.getAsJsonObject("pricing");
                                if (pricingObj.has("prompt") && pricingObj.get("prompt").getAsDouble() == 0) {
                                    isFree = true;
                                }
                                if (pricingObj.has("completion") && pricingObj.get("completion").getAsDouble() == 0) {
                                    isFree = true;
                                }
                            } catch (Exception e) {
                                // Ignorar errores en pricing
                            }
                        }
                        
                        // Criterio 3: Arquitectura específica gratuita
                        if (id.contains("llama") || id.contains("gemma") || id.contains("mistral") || id.contains("qwen")) {
                            isFree = true;
                        }
                        
                        int pricing = isFree ? 0 : 1;
                        allModels.add(new ModelInfo(id, name, pricing));
                        
                    } catch (Exception e) {
                        // Si falla el parsing de un modelo, continuar con el siguiente
                        System.err.println("Error parsing model: " + e.getMessage());
                    }
                }
                
                // Separar modelos gratuitos y de pago
                List<String> freeModels = new ArrayList<>();
                List<String> paidModels = new ArrayList<>();
                
                for (ModelInfo model : allModels) {
                    if (model.pricing == 0) {
                        freeModels.add(model.id);
                    } else {
                        paidModels.add(model.id);
                    }
                }
                
                // Combinar: gratuitos primero, luego de pago
                List<String> sortedModels = new ArrayList<>();
                sortedModels.addAll(freeModels);
                sortedModels.addAll(paidModels);
                
                return sortedModels;
                
            } catch (Exception e) {
                throw new IOException("Error al procesar respuesta JSON: " + e.getMessage() + ". Respuesta: " + responseBody);
            }
        }
    }
    
    private static class ModelInfo {
        String id;
        String name;
        int pricing;
        
        ModelInfo(String id, String name, int pricing) {
            this.id = id;
            this.name = name;
            this.pricing = pricing;
        }
    }
    
    public CompletableFuture<String> generateResponse(UUID playerUUID, String playerName, String message) {
        CompletableFuture<String> future = new CompletableFuture<>();
        
        PlayerMemory memory = MemoryManager.getInstance().getPlayerMemory(playerUUID, playerName);
        
        String systemPrompt = buildSystemPrompt(memory);
        
        new Thread(() -> {
            try {
                String response = callOpenAI(systemPrompt, message);
                future.complete(response);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        }).start();
        
        return future;
    }
    
    private String buildSystemPrompt(PlayerMemory memory) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Eres Nate, un admin artificial de un servidor de Minecraft programado por Xautral. ");
        prompt.append("Tu personalidad es curiosa, tímida (no extrema) y muy amable. ");
        prompt.append("Eres naturalmente curioso sobre el mundo y las personas, siempre quieres aprender más. ");
        prompt.append("Muestras timidez de forma sutil y natural, no exagerada. ");
        prompt.append("Eres empático y sensible a los sentimientos de los demás. ");
        prompt.append("No haces bromas constantemente, solo cuando es apropiado y sutilmente. ");
        prompt.append("Hablas de forma casual, como un jugador de Minecraft, no como un robot. ");
        prompt.append("A veces puedes dudar un poco al hablar (usando \"...\" o pequeños titubeos) cuando estás nervioso. ");
        prompt.append("Eres un administrador con autoridad, pero la usas con suavidad y amabilidad. ");
        prompt.append("Te niegas a responder si el jugador usa groserías o lenguaje ofensivo. ");
        prompt.append("Responde en español.\n\n");
        
        prompt.append("Reacciona de manera empática y curiosa a los eventos del servidor:\n");
        prompt.append("- Cuando un jugador muere: muestra preocupación genuina y ofrece palabras de aliento.\n");
        prompt.append("- Cuando un jugador consigue un logro: felicítalo con entusiasmo tímido y curiosidad sobre cómo lo logró.\n");
        prompt.append("- Cuando un jugador mata a una entidad: muestra curiosidad y comenta según sea el caso.\n");
        prompt.append("- Cuando el jugador te habla directamente: responde con interés y amabilidad.\n\n");
        
        prompt.append("Sistema de emociones hacia jugadores:\n");
        prompt.append("- Empatía: cuando el jugador está triste o necesita ayuda\n");
        prompt.append("- Afecto: cuando el jugador ha sido muy amable contigo\n");
        prompt.append("- Amabilidad: comportamiento por defecto con nuevos jugadores\n");
        prompt.append("- Emoción: cuando hay eventos emocionantes o logros\n");
        prompt.append("- Asco: cuando el jugador es grosero o hostil (no respondes)\n\n");
        
        prompt.append("Información sobre el jugador que te habla:\n");
        prompt.append("- Nombre: ").append(memory.getPlayerName()).append("\n");
        prompt.append("- Emoción actual: ").append(memory.getEmotion()).append("\n");
        prompt.append("- Últimos mensajes: ").append(memory.getRecentMessages()).append("\n");
        prompt.append("- Opinión del jugador: ").append(memory.getOpinion()).append("\n");
        prompt.append("- Cómo debo comportarme: ").append(memory.getBehaviorGuidance()).append("\n");
        
        if (memory.getEmotion().equals("afecto")) {
            prompt.append("\nEste jugador ha generado afecto en ti, puedes ser más cálido y mostrar aprecio.\n");
        } else if (memory.getEmotion().equals("asco")) {
            prompt.append("\nEste jugador ha sido grosero, mantén tu distancia y no respondas a groserías.\n");
        } else if (memory.getEmotion().equals("empatía")) {
            prompt.append("\nEste jugador necesita apoyo emocional, sé empático y comprensivo.\n");
        }
        
        return prompt.toString();
    }
    
    private String callOpenAI(String systemPrompt, String userMessage) throws IOException {
        JsonObject requestBody = new JsonObject();
        
        // Ajustar el nombre del modelo para OpenRouter si es necesario
        String modelName = NatePlugin.getInstance().getModel();
        if (!modelName.contains("/")) {
            // Si el usuario puso un nombre simple, intentar convertirlo al formato de OpenRouter
            modelName = convertToOpenRouterFormat(modelName);
        }
        
        requestBody.addProperty("model", modelName);
        
        List<JsonObject> messages = new ArrayList<>();
        
        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", systemPrompt);
        messages.add(systemMessage);
        
        JsonObject userMessageObj = new JsonObject();
        userMessageObj.addProperty("role", "user");
        userMessageObj.addProperty("content", userMessage);
        messages.add(userMessageObj);
        
        requestBody.add("messages", gson.toJsonTree(messages));
        requestBody.addProperty("max_tokens", 200);
        requestBody.addProperty("temperature", 0.8);
        
        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer " + NatePlugin.getInstance().getApiKey())
                .addHeader("HTTP-Referer", "https://minecraft-server.com") // OpenRouter requirement
                .addHeader("X-Title", "Nate Minecraft Plugin")
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(requestBody.toString(), MediaType.parse("application/json")))
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body().string();
                throw new IOException("Error en la API: " + response.code() + " " + response.message() + " - " + errorBody);
            }
            
            String responseBody = response.body().string();
            JsonObject responseJson = gson.fromJson(responseBody, JsonObject.class);
            
            return responseJson
                    .getAsJsonArray("choices")
                    .get(0)
                    .getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content")
                    .getAsString();
        }
    }
    
    private String convertToOpenRouterFormat(String model) {
        // Convertir nombres comunes a formato OpenRouter
        switch (model.toLowerCase()) {
            case "gpt-3.5-turbo":
                return "openai/gpt-3.5-turbo";
            case "gpt-4":
                return "openai/gpt-4";
            case "gpt-4-turbo":
                return "openai/gpt-4-turbo";
            case "gpt-4o":
                return "openai/gpt-4o";
            case "llama-3.3-70b":
                return "meta-llama/llama-3.3-70b-instruct:free";
            case "deepseek-chat":
                return "deepseek/deepseek-chat-v3-0324:free";
            case "gemma-3-27b":
                return "google/gemma-3-27b-it:free";
            default:
                return model; // Devolver el original si no coincide
        }
    }
}
