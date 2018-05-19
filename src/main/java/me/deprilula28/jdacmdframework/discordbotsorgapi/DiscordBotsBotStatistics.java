package me.deprilula28.jdacmdframework.discordbotsorgapi;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

@Data
public class DiscordBotsBotStatistics {
    @SerializedName("server_count") private int serverCount;
    private List<Integer> shards;
}
