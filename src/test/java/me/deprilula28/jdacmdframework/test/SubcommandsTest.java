package me.deprilula28.jdacmdframework.test;

import me.deprilula28.jdacmdframework.CommandContext;
import me.deprilula28.jdacmdframework.CommandFramework;
import me.deprilula28.jdacmdframework.Settings;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.User;

import java.util.stream.Stream;

public class SubcommandsTest {
    public static void main(String[] args) throws Exception {
        JDA jda = new JDABuilder(AccountType.BOT).setToken(args[0]).buildBlocking();
        Settings settings = Settings.builder()
                .prefix("!").async(true).protectMentionEveryone(true)
                .build();
        CommandFramework framework = new CommandFramework(jda, settings);

        // Adding commands
        framework.command("hello", context -> "hello", cmd -> {
            cmd.sub("world", context -> "world");
        });
        framework.command("messagepurge", SubcommandsTest::messagePurge, cmd -> {
            cmd.sub("user", SubcommandsTest::purgeUser);
        });

        framework.listenEvents();
    }

    private static String messagePurge(CommandContext context) {
        context.getChannel().getHistory().retrievePast(context.nextInt()).queue(history -> {
            history.forEach(cur -> cur.delete().queue());
            context.send(String.format("Deleted %s messages!", history.size()));
        });

        return null;
    }

    private static String purgeUser(CommandContext context) {
        User user = context.nextUser();
        context.getChannel().getHistory().retrievePast(context.nextInt()).queue(history -> {
            Stream<Message> delete = history.stream().filter(cur -> cur.getAuthor() == user);
            long amount = delete.count();
            delete.forEach(cur -> cur.delete().queue());
            context.send(String.format("Deleted %s messages from %s!", amount, user.getAsMention()));
        });

        return null;
    }
}
