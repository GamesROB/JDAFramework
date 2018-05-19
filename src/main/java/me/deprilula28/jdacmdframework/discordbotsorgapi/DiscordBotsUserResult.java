package me.deprilula28.jdacmdframework.discordbotsorgapi;

import lombok.Data;

@Data
public class DiscordBotsUserResult extends DiscordBotsBaseUser{
    private boolean admin;
    private boolean webMod;
    private boolean artist;
    private boolean certifiedDev;
    private boolean supporter;
}
