package me.deprilula28.jdacmdframework;

import lombok.Getter;
import me.deprilula28.jdacmdframework.annotations.ReflectionExecutor;
import me.deprilula28.jdacmdframework.discordbotsorgapi.DiscordBotsOrg;
import me.deprilula28.jdacmdframework.exceptions.CommandArgsException;
import me.deprilula28.jdacmdframework.exceptions.InvalidCommandSyntaxException;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CommandFramework extends ListenerAdapter {
    public static final String FRAMEWORK_VERSION = "1.1.3";
    private List<JDA> shards;
    @Getter private Settings settings;
    @Getter private final List<Command> commands = new ArrayList<>();
    private final Map<String, Command> aliasMap = new HashMap<>();
    private final List<Command.Executor> before = new ArrayList<>();
    private final List<Command.Executor> after = new ArrayList<>();
    private final Map<Class<? extends Event>, List<EventHandler>> eventHandlers = new HashMap<>();

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
    public static interface AdapterCommand {
        void use(Command command);
    }

    @FunctionalInterface
    public static interface EventHandler<T> {
        void handle(T event) throws Exception;
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
        return Command.registerCommand(aliases, executor, adapter, settings, commands, aliasMap);
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
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        if (event.getMessage().getMentionedUsers().contains(event.getJDA().getSelfUser()) || !(event.getChannel() instanceof TextChannel)) {
            String message = settings.getMentionedMessage();
            if (!(event.getChannel() instanceof TextChannel) || (event.getTextChannel().canTalk() && event.getMessage().getContentRaw().length() == 21))
                if (message != null) event.getChannel().sendMessage(message).queue();
                else if (settings.getMentionedMessageGetter() != null) event.getChannel()
                    .sendMessage(settings.getMentionedMessageGetter().apply(event.getGuild())).queue();
        } else {
            String prefix = settings.getPrefixGetter() == null ? settings.getPrefix()
                    : settings.getPrefixGetter().apply(event.getGuild());
            if (event.getMessage().getRawContent().startsWith(prefix)) {
                if (!event.getTextChannel().canTalk()) {
                    String cantTalkMessage = settings.getDmOnCantTalk();
                    if (cantTalkMessage != null)
                        event.getAuthor().openPrivateChannel().queue(it -> it.sendMessage(cantTalkMessage).queue());
                    return;
                }

                String commandContent = event.getMessage().getRawContent().substring(prefix.length());
                List<String> args = settings.isJoinQuotedArgs() ? quotedArgsJoinedSplit(commandContent) :
                        Arrays.asList(commandContent.split(" "));
                String command = args.get(0);

                if (!aliasMap.containsKey(command)) return;
                if (settings.isRemoveCommandMessages()) event.getMessage().delete().queue();

                Command handle = aliasMap.get(command);
                Runnable function = () -> {
                    CommandContext context = CommandContext.builder()
                            .author(event.getAuthor()).authorMember(event.getGuild().getMember(event.getAuthor()))
                            .guild(event.getGuild()).channel((TextChannel) event.getChannel()).message(event.getMessage())
                            .args(args).jda(event.getJDA()).framework(this).currentCommand(handle)
                            .commandTree(Collections.singletonList(handle)).build();

                    try {
                        before.forEach(it -> it.execute(context));
                        handle.execute(context);
                        after.forEach(it -> it.execute(context));
                    } catch (Exception e) {
                        if (e instanceof InvalidCommandSyntaxException) {
                            context.send("❌ Invalid syntax!\n" + handle.getUsage());
                        } else if (e instanceof CommandArgsException) {
                            context.send("❌ " + e.getMessage());
                        } else settings.getCommandExceptionFunction().accept(context, e);
                    }
                };

                if (settings.isAsync()) threadPool.submit(function);
                else function.run();
            }
        }
    }

    @Override
    public void onGenericEvent(Event event) {
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

    public DiscordBotsOrg setupDiscordBotsOrg(String dblToken) {
        DiscordBotsOrg dbo = DiscordBotsOrg.builder()
                .botID(shards.get(0).getSelfUser().getId()).shardCount(shards.size()).token(dblToken)
                .build();

        dbo.setStats(shards.stream().map(it -> it.getGuilds().size()).collect(Collectors.toList()));

        handleEvent(GuildJoinEvent.class, event -> dbo.setStats(shards.indexOf(event.getJDA()),
                event.getJDA().getGuilds().size()));
        handleEvent(GuildLeaveEvent.class, event -> dbo.setStats(shards.indexOf(event.getJDA()),
                event.getJDA().getGuilds().size()));

        return dbo;
    }
}
