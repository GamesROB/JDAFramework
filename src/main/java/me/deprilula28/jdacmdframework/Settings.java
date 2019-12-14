package me.deprilula28.jdacmdframework;

import lombok.Builder;
import lombok.Data;
import net.dv8tion.jda.api.entities.Guild;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Data
@Builder(builderClassName = "BuilderSettings")
public class Settings {
    @Builder.Default private String prefix = "!";
    @Builder.Default private Function<Guild, String> prefixGetter = null;
    @Builder.Default private String messageFormat = "→ %s";
    @Builder.Default private String mentionFormat = "%s → %s";
    @Builder.Default private boolean protectMentionEveryone = false;
    @Builder.Default private boolean commandResultMention = false;
    @Builder.Default private boolean joinQuotedArgs = true;
    @Builder.Default private boolean removeCommandMessages = false;
    @Builder.Default private boolean caseIndependent = false;
    @Builder.Default private boolean async = false;
    @Builder.Default private boolean removeReaction = false;
    @Builder.Default private int threadPoolSize = 5;
    @Builder.Default private ExecutorService threadPool = null;
    @Builder.Default private Consumer<String> loggerFunction = System.out::println;
    @Builder.Default private BiConsumer<CommandContext, Exception> commandExceptionFunction = (context, e) -> {
        System.err.println("Failed to handle command " + context.getMessage().getContentRaw() + ":");
        e.printStackTrace();
        context.send("❌ An error occured.");
    };
    @Builder.Default private BiConsumer<String, Exception> genericExceptionFunction = (message, e) -> {
        System.err.println("An error has occured (" + message + "):");
        e.printStackTrace();
    };
    @Builder.Default private String mentionedMessage = null;
    @Builder.Default private String dmOnCantTalk = "I can't talk in that channel!";
    @Builder.Default private Function<Guild, String> mentionedMessageGetter = null;
}
