package me.deprilula28.jdacmdframework;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import me.deprilula28.jdacmdframework.exceptions.InvalidCommandSyntaxException;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.requests.RestAction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

@Data
@Builder
public class CommandContext {
    private User author;
    private Member authorMember;
    private Guild guild;
    private TextChannel channel;
    private JDA jda;
    private CommandFramework framework;
    private Message message;
    private Event event;
    @Builder.Default private List<Command> commandTree = new ArrayList<>();
    @Builder.Default private List<String> args = new ArrayList<>();
    @Builder.Default private Command currentCommand = null;
    @Builder.Default private String currentReaction = null;
    @Builder.Default private List<User> reactionUsers = null;

    @Builder.Default private RequestPromise<Message> sentMessage = null;

    public RequestPromise<Message> send(String message) {
        return send(message, framework.getSettings().isCommandResultMention());
    }

    public RequestPromise<Message> send(String message, boolean mention) {
        Settings settings = framework.getSettings();

        String editMessage = message;
        if (settings.isProtectMentionEveryone()) editMessage = message
                    .replaceAll("@everyone", "`@everyone`")
                    .replaceAll(guild.getPublicRole().getAsMention(), "`@everyone`");

        final String fixedMessage = editMessage;

        return send(builder -> builder.append(mention
                ? String.format(settings.getMentionFormat(), author.getAsMention(), fixedMessage)
                : String.format(settings.getMessageFormat(), fixedMessage)
        ));
    }

    public RequestPromise<Message> send(MessageEmbed embed) {
        return send(builder -> builder.setEmbed(embed));
    }

    public RequestPromise<Message> send(EmbedBuilder embedBuilder) {
        return send(embedBuilder.build());
    }

    public RequestPromise<Message> send(Consumer<MessageBuilder> builder) {
        MessageBuilder message = new MessageBuilder();
        builder.accept(message);

        sentMessage = currentReaction == null
                ? RequestPromise.forAction(channel.sendMessage(message.build()))
                : sentMessage.morphAction(it -> it.editMessage(message.build()));
        return sentMessage;
    }

    public RequestPromise<Message> edit(String newContent) {
        return edit(newContent, framework.getSettings().isCommandResultMention());
    }

    public RequestPromise<Message> edit(String newContent, boolean mention) {
        Settings settings = framework.getSettings();
        return edit(builder -> builder.append(mention
            ? String.format(settings.getMentionFormat(), author.getAsMention(), newContent)
            : String.format(settings.getMessageFormat(), newContent)
        ));
    }

    public RequestPromise<Message> edit(MessageEmbed embed) {
        return edit(builder -> builder.setEmbed(embed));
    }

    public RequestPromise<Message> edit(EmbedBuilder embedBuilder) {
        return edit(embedBuilder.build());
    }

    public RequestPromise<Message> edit(Consumer<MessageBuilder> builder) {
        if (sentMessage == null) throw new RuntimeException("Asked to edit message in a context where none has been sent.");

        MessageBuilder message = new MessageBuilder();
        builder.accept(message);

        return sentMessage.morphAction(sent -> sent.editMessage(message.build()));
    }

    public RequestPromise<Void> delete() {
        if (sentMessage == null) throw new RuntimeException("Asked to delete message in a context where none was sent.");
        return sentMessage.morphAction(it -> it.delete());
    }

    @Builder.Default private int curArg = 1;

    public String next() {
        if (args.size() > curArg) {
            String arg = args.get(curArg);
            curArg ++;
            return arg;
        }
        throw new InvalidCommandSyntaxException();
    }

    public int nextInt() {
        return Utility.rethrow(n -> new InvalidCommandSyntaxException(), n -> Integer.parseInt(next()));
    }

    public double nextDouble() {
        return Utility.rethrow(n -> new InvalidCommandSyntaxException(), n -> Double.parseDouble(next()));
    }

    @Builder.Default private Pattern userPattern = Pattern.compile("<@[0-9]+>");
    @Builder.Default private Pattern nickedUserPattern = Pattern.compile("<@![0-9]+>");
    @Builder.Default private Pattern userTagPattern = Pattern.compile(".*#[0-9]{4}");
    @Builder.Default private Pattern idPattern = Pattern.compile("[0-9]+");

    public User nextUser() {
        String arg = next();
        if (userPattern.matcher(arg).matches()) return jda.getUserById(arg.substring(2, arg.length() - 1));
        else if (nickedUserPattern.matcher(arg).matches()) return jda.getUserById(arg.substring(3, arg.length() - 1));
        else if (idPattern.matcher(arg).matches())
            return Utility.rethrow(n -> new InvalidCommandSyntaxException(), n -> jda.getUserById(Long.parseLong(arg)));
        else if (userTagPattern.matcher(arg).matches()) {
            return jda.getUsersByName(arg.substring(0, arg.length() - 5), true)
                    .stream().filter(it -> it.getDiscriminator().equals(arg.substring(arg.length() - 4)))
                    .findFirst().orElseThrow(InvalidCommandSyntaxException::new);
        }
        else throw new InvalidCommandSyntaxException();
    }

    public List<String> remaining() {
        if (args.size() > curArg) return args.subList(curArg, args.size());
        else return new ArrayList<>();
    }

    public <T> Optional<T> opt(Supplier<T> supplier) {
        try {
            return Optional.of(supplier.get());
        } catch (InvalidCommandSyntaxException syntaxEx) {
            return Optional.empty();
        }
    }
}