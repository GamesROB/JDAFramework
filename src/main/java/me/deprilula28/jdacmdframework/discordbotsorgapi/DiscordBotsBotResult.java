package me.deprilula28.jdacmdframework.discordbotsorgapi;


import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

@Data
public class DiscordBotsBotResult extends DiscordBotsBaseUser {
    private String invite;
    private String website;
    private String prefix;
    private boolean certifiedBot;
    @SerializedName("lib") private String library;
    @SerializedName("longdesc") private String longDescription;
    @SerializedName("shortdesc") private String shortDescription;
    @SerializedName("server_count") private int serverCount;
    @SerializedName("points") private int upvotes;
    private List<String> owners;
    private List<Integer> shards;
}
