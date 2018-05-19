package me.deprilula28.jdacmdframework.discordbotsorgapi;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.gson.*;
import lombok.Builder;

import java.util.ArrayList;
import java.util.List;

@Builder
public class DiscordBotsOrg {
    private static final String URL_ROOT = "https://discordbots.org/api/";
    private String token;
    @Builder.Default private String userAgent = "deprilula28's BotListAPI for Java";
    @Builder.Default private String botID = null;
    @Builder.Default private Gson gson = new GsonBuilder().setPrettyPrinting().enableComplexMapKeySerialization().create();
    @Builder.Default private int botsPageSize = 25;
    @Builder.Default private int shardCount = 0;

    private <T> T getType(String apiResult, Class<T> type) {
        JsonObject gsonObject = gson.fromJson(apiResult, JsonObject.class);
        if (gsonObject.has("error")) throw new APIError(gsonObject.get("error").getAsString());
        return type == JsonObject.class ? (T) gsonObject : type == null ? null : gson.fromJson(gsonObject, type);
    }

    public List<DiscordBotsBotResult> getBots() {
        return getBots(1);
    }

    public List<DiscordBotsBotResult> getBots(int page) {
        JsonObject obj = getType(HttpRequest.get(URL_ROOT + "bots?limit=" + botsPageSize + "&offset=" + (page - 1) * botsPageSize)
                .authorization(token).userAgent(userAgent).body(),
                JsonObject.class);

        JsonArray results = obj.get("results").getAsJsonArray();
        List<DiscordBotsBotResult> bots = new ArrayList<>();
        results.forEach(cur -> bots.add(gson.fromJson(cur, DiscordBotsBotResult.class)));

        return bots;
    }

    public DiscordBotsUserResult getUser(String id) {
        return getType(HttpRequest.get(URL_ROOT + "users/" + id)
                        .authorization(token).userAgent(userAgent).body(),
                DiscordBotsUserResult.class);
    }

    public DiscordBotsBotResult getBot() {
        return getBot(botID);
    }

    public DiscordBotsBotResult getBot(String id) {
        return getType(HttpRequest.get(URL_ROOT + "bots/" + id)
                       .authorization(token).userAgent(userAgent).body(),
                DiscordBotsBotResult.class);
    }

    public List<DiscordBotsUserResult> getVoterIDs() {
        return gson.fromJson(HttpRequest.get(URL_ROOT + "bots/" + botID + "/votes?onlyids=true")
                        .authorization(token).userAgent(userAgent).body(),
                    List.class);
    }

    public List<DiscordBotsUserResult> getVoters() {
        JsonArray results = gson.fromJson(HttpRequest.get(URL_ROOT + "bots/" + botID + "/votes")
                        .authorization(token).userAgent(userAgent).body(),
                JsonArray.class);

        List<DiscordBotsUserResult> voters = new ArrayList<>();
        results.forEach(cur -> voters.add(gson.fromJson(cur, DiscordBotsUserResult.class)));

        return voters;
    }

    public DiscordBotsBotStatistics getStats() {
        return getType(HttpRequest.get(URL_ROOT + "bots/" + botID + "/stats")
                .authorization(token).userAgent(userAgent).body(),
                DiscordBotsBotStatistics.class);
    }

    public void setStats(int serverCount) {
        JsonObject apiRequest = new JsonObject();
        apiRequest.add("server_count", new JsonPrimitive(serverCount));

        getType(HttpRequest.post(URL_ROOT + "bots/" + botID + "/stats")
                        .userAgent(userAgent).authorization(token).acceptJson().contentType("application/json")
                        .send(gson.toJson(serverCount)).body(),
                null);
    }

    public void setStats(int shardID, int serverCount) {
        JsonObject apiRequest = new JsonObject();
        apiRequest.add("server_count", new JsonPrimitive(serverCount));
        apiRequest.add("shard_id", new JsonPrimitive(shardID));
        apiRequest.add("shard_count", new JsonPrimitive(shardCount));

        getType(HttpRequest.post(URL_ROOT + "bots/" + botID + "/stats")
                        .userAgent(userAgent).authorization(token).acceptJson().contentType("application/json")
                        .send(gson.toJson(apiRequest)).body(),
                null);
    }

    public void setStats(List<Integer> shards) {
        JsonObject apiRequest = new JsonObject();
        apiRequest.add("shards", gson.toJsonTree(shards));

        getType(HttpRequest.post(URL_ROOT + "bots/" + botID + "/stats")
                        .userAgent(userAgent).authorization(token).acceptJson().contentType("application/json")
                        .send(gson.toJson(apiRequest)).body(),
                null);
    }
}
