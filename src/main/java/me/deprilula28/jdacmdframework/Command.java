package me.deprilula28.jdacmdframework;

import lombok.Data;
import me.deprilula28.jdacmdframework.exceptions.CommandArgsException;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

@Data
public class Command {
    private List<String> aliases;
    private String name;

    @FunctionalInterface
    public interface Executor {
        Object execute(CommandContext context);
    }

    private Executor executor;
    private Settings settings;
    private String usage;

    private final Map<String, Command> subCommandAliasMap = new HashMap<>();
    private final List<Command> subCommands = new ArrayList<>();
    private final List<Function<CommandContext, String>> predicates = new ArrayList<>();

    public void execute(CommandContext context) {
        predicates.forEach(it -> {
            String text = it.apply(context);
            if (text != null) throw new CommandArgsException(text);
        });

        int argCount = context.getArgs().size();
        if (argCount > 0 && subCommandAliasMap.containsKey(context.getArgs().get(0))) {
            Command subCommand = subCommandAliasMap.get(context.getArgs().get(0));
            context.setCurrentCommand(subCommand);
            context.getCommandTree().add(subCommand);

            context.setCurArg(context.getCurArg() + 1);

            subCommand.execute(context);
            return;
        }

        Object result = executor.execute(context);
        if (result != null) {
            if (result instanceof String) context.send((String) result);
            else if (result instanceof MessageEmbed) context.send((MessageEmbed) result);
            else if (result instanceof EmbedBuilder) context.send((EmbedBuilder) result);
            else context.send(result.toString());
        }
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
        return registerCommand(aliases, handler, adapter, settings, subCommands, subCommandAliasMap);
    }

    public static Command registerCommand(String aliases, Executor handler, CommandFramework.AdapterCommand adapter,
                                       Settings settings, List<Command> commandList, Map<String, Command> aliasMap) {
        String[] aliasList = aliases.split(" ");
        String name = aliasList[0];

        Command command = new Command();
        command.setSettings(settings);
        command.setName(name);
        command.setAliases(Arrays.asList(aliasList));
        command.setUsage(String.format("%s%s", settings.getPrefix(), name));
        if (handler != null) command.setExecutor(handler);
        adapter.use(command);

        commandList.add(command);
        Arrays.stream(aliasList).forEach(cur -> aliasMap.put(cur, command));

        return command;
    }
}
