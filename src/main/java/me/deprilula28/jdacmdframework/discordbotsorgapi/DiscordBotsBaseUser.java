package me.deprilula28.jdacmdframework.discordbotsorgapi;

import lombok.Data;

import java.net.MalformedURLException;
import java.net.URL;

@Data
public class DiscordBotsBaseUser {
    private static final String USER_AVATAR_PATH = "https://cdn.discordapp.com/avatars/";

    private String defAvatar;
    private String avatar;
    private String id;
    private String discriminator;
    private String username;

    public URL getAvatarURL() throws MalformedURLException {
        return new URL(USER_AVATAR_PATH + id + "/" + avatar + ".png");
    }

    public URL getDefaultAvatarURL() throws MalformedURLException {
        return new URL(USER_AVATAR_PATH + id + "/" + defAvatar + ".png");
    }
}
