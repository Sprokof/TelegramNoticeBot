package telegramBot.bot;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

import telegramBot.service.RemindServiceImpl;
import telegramBot.validate.Validate;
import telegramBot.command.CommandContainer;
import telegramBot.entity.Remind;
import telegramBot.sendRemind.SendRemind;
import telegramBot.service.SendMessageServiceImpl;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Pattern;

@Getter
@Component
public class TelegramBot extends TelegramLongPollingBot {
    private final static Map<String, List<String>> commands;

    static{
        commands = new HashMap<>();
    }


    private static String tokenFromFile() {
        try {
            return new BufferedReader(new InputStreamReader(
                    new FileInputStream("C:/Users/user/Desktop/token.txt"))).readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Value("${bot.username}")
    private String botUsername;
    private final String botToken = tokenFromFile();
    private final String COMMAND_PREFIX = "/";
    private final CommandContainer commandContainer;
    @Getter
    private final SendMessageServiceImpl sendMessageService;
    private final SendRemind sendRemind;


    public TelegramBot() {
        this.sendMessageService = new SendMessageServiceImpl(this);
        this.commandContainer = new CommandContainer(sendMessageService);
        this.sendRemind = new SendRemind(sendMessageService);
    }

    @Override
    public void onUpdateReceived(Update update) {
        String command = "";
        String chatId;
        if (update.hasMessage() && update.getMessage().hasText()) {
            chatId = update.getMessage().getChatId().toString();
            commands.putIfAbsent(chatId, new ArrayList<String>());
            String message = update.getMessage().getText().trim();
            if (message.startsWith(COMMAND_PREFIX)) {
                command = message.split(" ")[0].toLowerCase(Locale.ROOT);
                this.commandContainer.retrieveCommand(command).execute(update);
                commands.get(chatId).add(command);
            } else {
                if (lastCommand(chatId).equals("/add")) {
                    acceptNewRemindFromUser(update);
                } else if (lastCommand(chatId).equals("/show")) {
                    if (acceptDateFromUser(update)) {
                        try {
                            if (!this.sendRemind.showRemindsByDate(update.getMessage().getChatId().toString(),
                                    update.getMessage().getText())) {
                                this.sendMessageService.sendMessage(update.getMessage().getChatId().toString(),
                                        "Не получилось найти напоминания, возможно " +
                                                "вы указали уже прошедшую дату, либо на эту дату нет напоминаний");
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        this.sendMessageService.sendMessage(update.getMessage().getChatId().toString(),
                                "Вероятно вы ошиблись в формате даты. Повторите команду /show и " +
                                        "введите дату в верном формате");
                    }
                }
            }
        }

        executeRemind();
    }

    private String getDateFromUserInput(String input) {
        int firstIndexOfDate = 0;
        int lastIndexOfDate = input.length();
        for (int i = 0; i < input.split("").length; i++) {
            if (String.valueOf(input.charAt(i)).matches("[0-9]")
                    && String.valueOf(input.charAt(i + 1)).
                    matches("\\p{P}")) {
                firstIndexOfDate = (i - 1);
                break;
            }
        }
        return input.substring(firstIndexOfDate, lastIndexOfDate);
    }


    private String getRemindContentFromUserInput(String input) {
        return input.substring(0, input.length() - getDateFromUserInput(input).length());
    }

    private void acceptNewRemindFromUser(Update update) {
        String chatId = update.getMessage().getChatId().toString();
        String input = update.getMessage().getText();
        boolean isContains;
        if (isCorrectInput(input)) {
            String defaultFlag = String.valueOf(true);
            try {
                Remind remind = new Remind(chatId, getRemindContentFromUserInput(input),
                        getDateFromUserInput(input).
                                replaceAll("\\p{P}", "\\."),
                        defaultFlag, 0, 0, String.valueOf(false));
                isContains = RemindServiceImpl.newRemindService().isContainsInDB(remind);
                if (!isContains) {
                    if (saveRemind(remind)) {
                        this.sendMessageService.sendMessage(chatId, "Напоминание успешно" +
                                " добавлено.");
                    } else {
                        this.sendMessageService.sendMessage(chatId,
                                "Напоминание не было добавлено, проверьте формат даты (dd.mm.yyyy) ." +
                                        "Возможно, вы указали уже прошедшую дату. " +
                                        "После введите команду /add для повторного добавления.");
                    }
                } else {
                    this.sendMessageService.sendMessage(chatId,
                            "Данное напоминание было добавлено ранее.");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            this.sendMessageService.sendMessage(chatId,
                    "Напоминание не было добавлено, проверьте формат даты (dd.mm.yyyy) . " +
                            "Возможно, что вы указали уже прошедшую дату. " +
                            "После введите команду /add для повторного добавления.");
        }
        commands.clear();
    }

    private boolean isCorrectInput(String input) {
        Pattern p = Pattern.compile("[Aa-zZ\\s][0-9]{2}\\p{P}[0-9]{2}\\p{P}[0-9]{4}");
        boolean textWithRightDate = p.matcher(input).find();
        if (textWithRightDate) {
            return Validate.date(getDateFromUserInput(input).split("\\p{P}")[0],
                    getDateFromUserInput(input).split("\\p{P}")[1],
                    getDateFromUserInput(input).split("\\p{P}")[2]);
        } else {
            return false;
        }
    }

    private boolean acceptDateFromUser(Update update) {
        String input = update.getMessage().getText();
        Pattern p = Pattern.compile("[0-9]{2}\\p{P}[0-9]{2}\\p{P}[0-9]{4}");
        boolean isDate = p.matcher(input).find();
        if (isDate) {
            String[] dateArray = input.split("\\p{P}");
            Validate.date(dateArray[0], dateArray[1], dateArray[2]);
            return true;
        } else {
            return false;
        }
    }

    private boolean saveRemind(Remind remind) {
        return RemindServiceImpl.newRemindService().saveRemind(remind);
    }

    private void executeRemind() {
        new Thread(() -> {
            try {
                while (true) {
                    TelegramBot.this.sendRemind.send();
                    printComplete();
                    Thread.sleep(420000);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private synchronized String lastCommand(String chatId) {
        while (commands.get(chatId).isEmpty()) {
            try {
                wait();
            } catch (InterruptedException e) {
                System.out.println("Something went wrong");
            }
        }
        notify();

        int lastIndex = commands.get(chatId).size()-1;
        return commands.get(chatId).get(lastIndex);
    }

    private void printComplete() {
        System.out.println("...COMPLETE...");
    }

}

























