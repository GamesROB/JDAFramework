package me.deprilula28.jdacmdframework.executors;

import lombok.AllArgsConstructor;
import lombok.Data;
import me.deprilula28.jdacmdframework.Command;
import me.deprilula28.jdacmdframework.CommandContext;
import me.deprilula28.jdacmdframework.CommandFramework;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public class CategoriesExecutor implements Command.Executor {
    private CommandFramework framework;
    private List<Category> categories;
    private Map<String, Category> categoryAliases;
    private Map<String, Category> emotes;
    private List<Function<CommandContext, String>> before = new ArrayList<>();
    private List<Consumer<MessageBuilder>> after = new ArrayList<>();

    public CategoriesExecutor(CommandFramework framework) {
        this.framework = framework;
    }

    @Override
    public Object execute(CommandContext context) {
        Optional<String> optAlias = context.opt(context::next);
        if (optAlias.isPresent() && categoryAliases.containsKey(optAlias.get())) {
            Category category = categoryAliases.get(optAlias.get());
            return category.getEmoji() + category.getNameGen().apply(context) + "\n" + category.getTextGen().apply(context);
        }

        context.send(builder -> {
            before.forEach(cur -> builder.append(cur.apply(context)).append("\n"));
            categories.forEach(category -> {
                builder.append(category.getEmoji()).append(category.getNameGen().apply(context));
                if (category.isAlwaysShow()) builder.append("\n").append(category.getTextGen().apply(context)).append("\n\n");
                else builder.append(" `").append(category.getMainAlias()).append("`");
            });
            after.forEach(consumer -> consumer.accept(builder));
        });
        return null;
    }

    @AllArgsConstructor
    @Data
    private static class Category {
        private String mainAlias;
        private Function<CommandContext, String> nameGen;
        private Function<CommandContext, String> textGen;
        private boolean alwaysShow;
        private String emoji;
    }

    public CategoriesExecutor category(String aliases, String name, String emoji, Function<CommandContext, String> textGen) {
        return category(aliases, context -> name, emoji, textGen);
    }

    public CategoriesExecutor category(String aliases, Function<CommandContext, String> nameGen, String emoji, Function<CommandContext, String> textGen) {
        return category(aliases, nameGen, textGen, emoji, false);
    }

    public CategoriesExecutor category(String aliases, String name, Function<CommandContext, String> textGen, String emoji, boolean alwaysShow) {
        return category(aliases, context -> name, textGen, emoji, alwaysShow);
    }

    public CategoriesExecutor category(String aliases, Function<CommandContext, String> nameGen, Function<CommandContext, String> textGen, String emoji, boolean alwaysShow) {
        String[] split = aliases.split(" ");
        Category category = new Category(split[0], nameGen, textGen, alwaysShow, emoji);
        for (String alias : split) categoryAliases.put(alias, category);
        categories.add(category);

        return this;
    }

    public CategoriesExecutor before(Function<CommandContext, String> before) {
        this.before.add(before);
        return this;
    }

    public CategoriesExecutor after(Consumer<MessageBuilder> after) {
        this.after.add(after);
        return this;
    }
}
