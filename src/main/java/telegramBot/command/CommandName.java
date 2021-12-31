package telegramBot.command;

public enum CommandName {
    START("/start"),
    STOP("/stop"),
    UNKNOWN("/unknown"),
    ADD("/add"),
    RESTART("/restart");

    private String commandName;

    CommandName(String commandName){this.commandName = commandName;}

    public String getCommandName() {
        return commandName;
    }
}
