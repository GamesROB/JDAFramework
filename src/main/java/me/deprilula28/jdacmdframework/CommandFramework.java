package me.deprilula28.jdacmdframework;

import lombok.Getter;
import me.deprilula28.jdacmdframework.annotations.ReflectionExecutor;
import me.deprilula28.jdacmdframework.exceptions.CommandArgsException;
import me.deprilula28.jdacmdframework.exceptions.InvalidCommandSyntaxException;
import me.deprilula28.jdacmdframework.executors.CategoriesExecutor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandFramework extends ListenerAdapter {
    public static final String FRAMEWORK_VERSION = "1.2.0";
    private List<JDA> shards;
    @Getter private Settings settings;
    @Getter private final List<Command> commands = new ArrayList<>();
    @Getter private final Map<String, Command> aliasMap = new HashMap<>();
    private final List<Command.Executor> before = new ArrayList<>();
    private final List<Command.Executor> after = new ArrayList<>();
    private final Map<Class<? extends Event>, List<EventHandler>> eventHandlers = new HashMap<>();
    private final Map<String, List<ReactionHandler>> reactionHandlers = new HashMap<>();

    // Bots that aren't sharded
    public CommandFramework(JDA jda, Settings settings) {
        shards = new ArrayList<>();
        shards.add(jda);

        this.settings = settings;
        if (settings.isAsync()) threadPool = settings.getThreadPool() == null ?
                Executors.newFixedThreadPool(settings.getThreadPoolSize()) : settings.getThreadPool();
    }

    // Sharded bots
    public CommandFramework(List<JDA> shards, Settings settings) {
        this.shards = shards;
        this.settings = settings;
        if (settings.isAsync()) threadPool = settings.getThreadPool() == null ?
                Executors.newFixedThreadPool(settings.getThreadPoolSize()) : settings.getThreadPool();
    }

    @FunctionalInterface
    public static interface ReactionHandler {
        void handle(CommandContext context);
    }

    @FunctionalInterface
    public static interface AdapterCommand {
        void use(Command command);
    }

    @FunctionalInterface
    public static interface EventHandler<T> {
        void handle(T event) throws Exception;
    }

    public CategoriesExecutor categoriesExecutor() {
        return new CategoriesExecutor(this);
    }

    /*
    TODO

    public CategoriesExecutor paginatedExecutor() {
        return new CategoriesExecutor(this);
    }
    */

    public void clear() {
        commands.clear();
        aliasMap.clear();
        before.clear();
        after.clear();
        eventHandlers.clear();
        reactionHandlers.clear();
    }

    public void reactionHandler(String emote, ReactionHandler handler) {
        if (!reactionHandlers.containsKey(emote)) reactionHandlers.put(emote, new ArrayList<>());
        reactionHandlers.get(emote).add(handler);
    }

    public Command command(String aliases) {
        return command(aliases, null, n -> {});
    }

    public Command command(String aliases, AdapterCommand adapter) {
        return command(aliases, null, adapter);
    }

    public Command command(String aliases, Command.Executor executor) {
        return command(aliases, executor, n -> {});
    }

    public Command command(String aliases, Command.Executor executor, AdapterCommand adapter) {
        return Command.registerCommand(aliases, executor, adapter, this, commands, aliasMap);
    }

    public Command reflCommand(String aliases, Class<?> clazz) {
        return reflCommand(aliases, clazz, cmd -> {});
    }

    public Command reflCommand(String aliases, Class<?> clazz, AdapterCommand adapter) {
        Command command = command(aliases, null, adapter);
        command.setExecutor(new ReflectionExecutor(clazz, command));
        return command;
    }

    public Command reflCommand(String aliases, Object object) {
        return reflCommand(aliases, object, cmd -> {});
    }

    public Command reflCommand(String aliases, Object object, AdapterCommand adapter) {
        Command command = command(aliases, null, adapter);
        command.setExecutor(new ReflectionExecutor(object, command));
        return command;
    }

    public <T extends Event> CommandFramework handleEvent(Class<T> event, EventHandler<T> handler) {
        if (!eventHandlers.containsKey(event)) eventHandlers.put(event, new ArrayList<>());
        eventHandlers.get(event).add(handler);
        return this;
    }

    public CommandFramework before(Command.Executor preCommand) {
        before.add(preCommand);
        return this;
    }

    public CommandFramework after(Command.Executor postCommand) {
        after.add(postCommand);
        return this;
    }

    public void listenEvents() {
        shards.forEach(cur -> cur.addEventListener(this));
        log(String.format("Command framework listening for events. %s commands were registered.", commands.size()));
    }

    private ExecutorService threadPool;

    public void useThreadPool(Runnable runnable) {
        if (settings.isAsync()) threadPool.execute(runnable);
        else runnable.run();
    }

    @Override
    public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) {
        if (event.getReactionEmote().isEmote() || event.getUser().isBot()) return;
        String reaction = event.getReactionEmote().getName();

        if (reactionHandlers.containsKey(reaction)) {
            event.getChannel().retrieveMessageById(event.getMessageId()).queue(message -> {
                if (!message.getAuthor().equals(event.getJDA().getSelfUser())) return;
                message.getReactions().stream()
                        .filter(it -> !it.getReactionEmote().isEmote() && it.getReactionEmote().getName().equals(reaction))
                        .findAny().ifPresent(emote -> emote.retrieveUsers().queue(users -> {
                    if (!users.contains(event.getJDA().getSelfUser())) return;

                    CommandContext context = CommandContext.builder()
                            .author(event.getUser()).authorMember(event.getGuild().getMember(event.getUser()))
                            .guild(event.getGuild()).channel((TextChannel) event.getChannel()).message(message)
                            .sentMessage(RequestPromise.valueProvided(message)).event(event)
                            .jda(event.getJDA()).framework(this).currentReaction(reaction).reactionUsers(users).build();

                    try {
                        reactionHandlers.get(reaction).forEach(cur -> cur.handle(context));
                    } catch (Exception e) {
                        if (e instanceof InvalidCommandSyntaxException) {
                            try {
                                event.getReaction().removeReaction(event.getUser()).queue();
                            } catch (PermissionException ex) {}
                        } else settings.getCommandExceptionFunction().accept(context, e);
                    }
                }));
            });
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        try {
            if (event.getAuthor().isBot() || !(event.getChannel() instanceof TextChannel)) return;

            if (event.getMessage().getMentionedUsers().contains(event.getJDA().getSelfUser())) {
                String message = settings.getMentionedMessage();
                System.out.println(event.getMessage().getContentRaw().length());
                if ((event.getTextChannel().canTalk() && event.getMessage().getContentRaw().length() < 23))
                    if (message != null) event.getChannel().sendMessage(message).queue();
                    else if (settings.getMentionedMessageGetter() != null) event.getChannel()
                            .sendMessage(settings.getMentionedMessageGetter().apply(event.getGuild())).queue();
            } else {
                String prefix = settings.getPrefixGetter() == null ? settings.getPrefix()
                        : settings.getPrefixGetter().apply(event.getGuild());
                if (event.getMessage().getContentRaw().startsWith(prefix) || (settings.isCaseIndependent() &&
                        event.getMessage().getContentRaw().startsWith(prefix.toUpperCase()))) {
                    if (!event.getTextChannel().canTalk()) {
                        String cantTalkMessage = settings.getDmOnCantTalk();
                        if (cantTalkMessage != null)
                            event.getAuthor().openPrivateChannel().queue(it -> it.sendMessage(cantTalkMessage).queue());
                        return;
                    }

                    String commandContent = event.getMessage().getContentRaw().substring(prefix.length());
                    List<String> args = settings.isJoinQuotedArgs() ? quotedArgsJoinedSplit(commandContent) :
                            Arrays.asList(commandContent.split(" "));
                    if (args.isEmpty()) return;
                    String command = args.get(0);

                    if (!aliasMap.containsKey(command)) return;
                    if (settings.isRemoveCommandMessages()) event.getMessage().delete().queue();

                    Command handle = aliasMap.get(command);
                    Runnable function = () -> {
                        CommandContext context = CommandContext.builder()
                                .author(event.getAuthor()).authorMember(event.getGuild().getMember(event.getAuthor()))
                                .guild(event.getGuild()).channel((TextChannel) event.getChannel()).message(event.getMessage())
                                .args(args).jda(event.getJDA()).framework(this).currentCommand(handle).event(event)
                                .commandTree(new ArrayList<>(Collections.singletonList(handle))).build();
                        boolean runningAfter = false;

                        try {
                            for (Command.Executor executor : before) {
                                Object result = executor.execute(context);
                                if (result == null) continue;

                                if (result instanceof String) context.send((String) result);
                                else if (result instanceof MessageEmbed) context.send((MessageEmbed) result);
                                else if (result instanceof EmbedBuilder) context.send((EmbedBuilder) result);
                                else if (result instanceof Consumer) context.send((Consumer<MessageBuilder>) result);
                                else context.send(result.toString());
                                return;
                            }
                            handle.execute(context);
                            runningAfter = true;
                            runAfter(context);
                        } catch (Exception e) {
                            if (e instanceof InvalidCommandSyntaxException) {
                                context.send("❌ Invalid syntax!\n" + handle.getUsage());
                            } else if (e instanceof CommandArgsException) {
                                context.send("❌ " + e.getMessage());
                            } else settings.getCommandExceptionFunction().accept(context, e);
                            if (!runningAfter) runAfter(context);
                        }
                    };

                    if (settings.isAsync()) threadPool.submit(function);
                    else function.run();
                }
            }
        } catch (Exception e) {
            settings.getGenericExceptionFunction().accept("Handling command", e);
        }
    }

    private void runAfter(CommandContext context) {
        after.forEach(it -> {
            try {
                it.execute(context);
            } catch (Exception ex) {
                settings.getCommandExceptionFunction().accept(context, ex);
            }
        });
    }

    @Override
    public void onGenericEvent(GenericEvent event) {
        if (eventHandlers.containsKey(event.getClass())) {
            eventHandlers.get(event.getClass()).forEach(cur -> {
                try {
                    cur.handle(event);
                } catch (Exception e) {
                    settings.getGenericExceptionFunction().accept("Handling event; Handler: " +
                            cur.getClass().getName() + ", Event: " + event.toString(), e);
                }
            });
        }
    }

    private Pattern joinQuotedArgsPattern = Pattern.compile("\"(.*?)\"|[^\\s]+");

    private List<String> quotedArgsJoinedSplit(String content) {
        List<String> args = new ArrayList<>();
        Matcher matcher = joinQuotedArgsPattern.matcher(content);

        while (matcher.find()) {
            if (matcher.group(1) != null) {
                String arg = matcher.group(1);
                if (arg.endsWith("\\")) args.addAll(Arrays.asList(arg.substring(0, arg.length() - 1).split(" ")));
                else args.add(arg);
            } else {
                args.add(matcher.group().replaceAll("\\" + "\"", "\"")); // Replacing \" in command to "
            }
        }

        return args;
    }

    private void log(String message) {
        settings.getLoggerFunction().accept(message);
    }
}
