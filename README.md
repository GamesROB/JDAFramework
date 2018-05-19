# DepsJDAFramework
A command framework for JDA (Java Discord API) with modern and simple syntax.

## Usage Example
```java
public void loadFramework() {
  CommandFramework framework = new CommandFramework(jda, settings);

  // Adding commands
  framework.command("hello", this::hello).setUsage("!hello <message>");
  framework.command("ping", this::ping);

  framework.listenEvents();
}

private String ping(CommandContext context) {
    long start = System.currentTimeMillis();
    context.send("Ping..").then(message -> 
    context.edit(String.format("Pong! Took **%s**ms.", System.currentTimeMillis() - start)));
    return null;
}

private String hello(CommandContext context) {
    return String.format("Hello %s! Your input was %s.", context.getAuthor().getAsMention(), context.next());
}
```

## Features
* Straightforward sintax
* CommandContext instance with simple argument methods like nextInt(), nextDouble() and remaining()
* [Reflection command executions](https://github.com/deprilula28/DepsJDAFramework/blob/29058ee92d1951d30848b91bd68a3b7c7b61c803/src/test/java/me/deprilula28/jdacmdframework/test/ReflectionTest.java)

## Start using it
"# JDAFramework" 
