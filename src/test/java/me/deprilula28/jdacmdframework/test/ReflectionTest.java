package me.deprilula28.jdacmdframework.test;

import me.deprilula28.jdacmdframework.CommandContext;
import me.deprilula28.jdacmdframework.CommandFramework;
import me.deprilula28.jdacmdframework.Settings;
import me.deprilula28.jdacmdframework.annotations.CommandExecutor;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.User;

public class ReflectionTest {
    public static void main(String[] args) throws Exception {
        JDA jda = new JDABuilder(AccountType.BOT).setToken(args[0]).buildBlocking();
        Settings settings = Settings.builder()
                .prefix("!").async(true).protectMentionEveryone(true)
                .build();
        CommandFramework framework = new CommandFramework(jda, settings);

        // Adding commands
        framework.reflCommand("say", ReflectionSayCommand.class);
        framework.reflCommand("mention", new ReflectionMentionCommand());

        framework.listenEvents();
    }

    public static class ReflectionSayCommand {
        @CommandExecutor
        public static String say(CommandContext context, String toSay) {
            return toSay;
        }
    }

    public static class ReflectionMentionCommand {
        @CommandExecutor
        public String mention(CommandContext context, User mention, String say) {
            return String.format("%s, %s tells you %s", mention.getAsMention(), context.getAuthor().getAsMention(), say);
        }
    }
}
