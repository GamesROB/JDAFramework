package me.deprilula28.jdacmdframework;

import lombok.Data;
import me.deprilula28.jdacmdframework.exceptions.CommandArgsException;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

@Data
public class Command {
    private final Map<String, Command> subCommandAliasMap = new HashMap<>();
    private final List<Command> subCommands = new ArrayList<>();
    private final List<Function<CommandContext, String>> predicates = new ArrayList<>();
    private final List<String> emotes = new ArrayList<>();

    private List<String> aliases;
    private CommandFramework framework;
    private String name;

    @FunctionalInterface
    public interface Executor {
        Object execute(CommandContext context);
    }

    private Map<String, String> attribs = new HashMap<>();
    private Executor executor;
    private Settings settings;
    private String usage;

    public String attr(String name) {
        return attribs.get(name);
    }

    public Command attr(String name, String value) {
        attribs.put(name, value);
        return this;
    }

    public void execute(CommandContext context) {
        predicates.forEach(it -> {
            String text = it.apply(context);
            if (text != null) throw new CommandArgsException(text);
        });

        if (context.getArgs().size() > context.getCurArg() && subCommandAliasMap.containsKey(context.getArgs().get(context.getCurArg()))) {
            Command subCommand = subCommandAliasMap.get(context.getArgs().get(context.getCurArg()));
            context.setCurrentCommand(subCommand);
            context.setCommandTree(new ArrayList<>(Collections.singletonList(this)));
            context.getCommandTree().add(subCommand);

            context.setCurArg(context.getCurArg() + 1);

            subCommand.execute(context);
            return;
        }

        Object result = executor.execute(context);
        if (result != null) {
            Consumer<Message> reactions = message -> emotes.forEach(it -> message.addReaction(it).queue());

            if (result instanceof String) context.send((String) result).then(reactions);
            else if (result instanceof MessageEmbed) context.send((MessageEmbed) result).then(reactions);
            else if (result instanceof EmbedBuilder) context.send((EmbedBuilder) result).then(reactions);
            else if (result instanceof Consumer) context.send((Consumer<MessageBuilder>) result).then(reactions);
            else context.send(result.toString()).then(reactions);
        }
    }

    public Command react(String emote, CommandFramework.ReactionHandler handler) {
        emotes.add(emote);
        framework.reactionHandler(emote, handler);

        return this;
    }

    public Command reactSub(String emote, String subCommand) {
        if (!subCommandAliasMap.containsKey(subCommand)) throw new RuntimeException("Sub command not defined.");
        emotes.add(emote);
        framework.reactionHandler(emote, context -> {
            context.setArgs(Arrays.asList(aliases.get(0), subCommand));
            context.setCommandTree(Collections.singletonList(this));
            execute(context);
            if (framework.getSettings().isRemoveReaction()) try {
                ((GuildMessageReactionAddEvent) context.getEvent()).getReaction().removeReaction(context.getAuthor()).queue();
            } catch (PermissionException e) {}
        });

        return this;
    }

    public Command filter(Function<CommandContext, String> predicate) {
        predicates.add(predicate);
        return this;
    }

    public Command sub(String aliases) {
        return sub(aliases, null, n -> {});
    }

    public Command sub(String aliases, CommandFramework.AdapterCommand adapter) {
        return sub(aliases, null, adapter);
    }

    public Command sub(String aliases, Executor handler) {
        return sub(aliases, handler, n -> {});
    }

    public Command sub(String aliases, Executor handler, CommandFramework.AdapterCommand adapter) {
        return registerCommand(aliases, handler, adapter, framework, subCommands, subCommandAliasMap);
    }

    public static Command registerCommand(String aliases, Executor handler, CommandFramework.AdapterCommand adapter,
                                       CommandFramework framework, List<Command> commandList, Map<String, Command> aliasMap) {
        String[] aliasList = aliases.split(" ");
        String name = aliasList[0];

        Command command = new Command();
        command.setFramework(framework);
        command.setSettings(framework.getSettings());
        command.setName(name);
        command.setAliases(Arrays.asList(aliasList));
        command.setUsage(String.format("%s%s", framework.getSettings().getPrefix(), name));
        if (handler != null) command.setExecutor(handler);
        adapter.use(command);

        commandList.add(command);
        Arrays.stream(aliasList).forEach(cur -> aliasMap.put(cur, command));

        return command;
    }
}
