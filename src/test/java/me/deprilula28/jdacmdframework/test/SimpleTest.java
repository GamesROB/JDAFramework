package me.deprilula28.jdacmdframework.test;

import me.deprilula28.jdacmdframework.CommandContext;
import me.deprilula28.jdacmdframework.CommandFramework;
import me.deprilula28.jdacmdframework.Settings;
import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

public class SimpleTest {
    public static void main(String[] args) throws Exception {
        JDA jda = new JDABuilder(AccountType.BOT).setToken(args[0]).build();
        Settings settings = Settings.builder()
                .prefix("!").async(true).protectMentionEveryone(true)
                .build();
        CommandFramework framework = new CommandFramework(jda, settings);

        // Adding commands
        framework.command("hello", SimpleTest::hello).setUsage("!hello <message>");
        framework.command("ping", SimpleTest::ping);

        framework.listenEvents();
    }

    private static String ping(CommandContext context) {
        long start = System.currentTimeMillis();
        context.send("Ping..").then(message ->
                context.edit(String.format("Pong! Took **%s**ms.", System.currentTimeMillis() - start)));
        return null;
    }

    private static String hello(CommandContext context) {
        return String.format("Hello %s! Your input was %s.", context.getAuthor().getAsMention(), context.next());
    }
}
