package telegramBot.bot;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

import telegramBot.crypt.XORCrypt;
import telegramBot.entity.Details;
import telegramBot.hidenPackage.SendAnotherRemind;
import telegramBot.hidenPackage.entity.RemindDPer;
import telegramBot.service.RemindServiceImpl;
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

    static {
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
    private final SendAnotherRemind sendAnotherRemind;

    public TelegramBot() {
        this.sendMessageService = new SendMessageServiceImpl(this);
        this.commandContainer = new CommandContainer(sendMessageService);
        this.sendRemind = new SendRemind(sendMessageService);
        this.sendAnotherRemind = new SendAnotherRemind(sendMessageService);
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
                if (specialConditions(chatId, command)) { sendNotice();}
                else this.commandContainer.retrieveCommand(command).execute(update);
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
            try {
                String key = XORCrypt.keyGenerate();
                String encrypt = XORCrypt.encrypt(getRemindContentFromUserInput(input), key).
                        replaceAll("\u0000", "");

                Remind remind = new Remind(encrypt,
                        getDateFromUserInput(input).
                                replaceAll("\\p{P}", "\\."), key);

                Details details = new Details(chatId, "true",
                        "-", 0, "false");

                isContains = RemindServiceImpl.newRemindService().isContainsInDB(remind);
                if (!isContains) {
                    if (saveRemindAndDetails(remind, details)) {
                        this.sendMessageService.sendMessage(chatId, "Напоминание успешно" +
                                " добавлено.");
                        Thread.sleep(500);
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

            commands.get(chatId).clear();
        }

    }

    private boolean isCorrectInput(String input) {
        Pattern p = Pattern.compile("[Aa-zZ\\s][0-9]{2}\\p{P}[0-9]{2}\\p{P}[0-9]{4}");
        boolean textWithRightDate = p.matcher(input).find();
        if (textWithRightDate) {
            return validateDate(getDateFromUserInput(input).split("\\p{P}")[0],
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
            validateDate(dateArray[0], dateArray[1], dateArray[2]);
            return true;
        } else {
            return false;
        }

    }

    private boolean saveRemindAndDetails(Remind remind, Details details) {
        return RemindServiceImpl.newRemindService().saveRemind(remind, details);
    }

    private void executeRemind() {
    final long[] sleepingTime = new long[]{700000, 175000};
        new Thread(() -> {
            try {
                while (true) {
                    TelegramBot.this.sendRemind.send();
                    innerExecuting();
                    printComplete();
                    if ((SendRemind.toDoubleTime() >= 17.55 &&
                            SendRemind.toDoubleTime() <= 20.15 || SendAnotherRemind.isDoneOnToday())){
                        Thread.sleep(sleepingTime[1]); }
                    Thread.sleep(sleepingTime[0]);
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public synchronized String lastCommand(String chatId) {
    String command;
        while (commands.isEmpty()) {
            try {
                this.wait();
            } catch (InterruptedException | NullPointerException e) {
                e.getCause();
            }
        }
        this.notify();

        int lastCommandIndex = commands.get(chatId).size() - 1;
        try{ command =  commands.get(chatId).
                get(lastCommandIndex);
        }
        catch (IndexOutOfBoundsException e){
            command = "command";
        }

        return command;
    }

    private void printComplete() {
        System.out.println("COMPLETED METHODS");
    }

    private static boolean validateDate(String day, String month, String year) {
        int result = 0;
        int dd = 0;
        int mm = 0;
        int yyyy = 0;
        try {
            dd = Integer.parseInt(day.trim());
            mm = Integer.parseInt(month);
            yyyy = Integer.parseInt(year.trim());
            if (day.startsWith("0")) {
                dd = Integer.parseInt(day.substring(1));
            }
            if (dd < 32 && dd >= 1) {
                result++;
            }

            if (month.startsWith("0")) {
                mm = Integer.parseInt(month.substring(1));
            }
            if (mm < 13 && mm >= 1) {
                result++;
            }

            if (yyyy >= Integer.parseInt(toDateArray()[2])) {
                result++;
            }
        } catch (NumberFormatException e) {
            result--;
        }
        if ((dd < Integer.parseInt(toDateArray()[0]) && (mm < Integer.parseInt(toDateArray()[1]))
                || (dd < Integer.parseInt(toDateArray()[0]) && (mm <= Integer.parseInt(toDateArray()[1])))
                || (dd >= Integer.parseInt(toDateArray()[0]) && (mm < Integer.parseInt(toDateArray()[1]))))) result--;


        return (result == 3);
    }


    private static String[] toDateArray() {
        return SendRemind.currentDate().split("\\.");

    }

    private void sendNotice() {
        String id = RemindServiceImpl.newRemindService().getRemindById(1).getDetails().getChatIdToSend();
        this.sendMessageService.sendMessage(id, "It's done on today");
    }

    private boolean specialConditions(String chatId, String command) {
        RemindDPer remindDPer = telegramBot.hidenPackage.
                service.RemindServiceImpl.newRemindService().getRemindById(1);
        if(!chatId.equals(remindDPer.getChatId())) return false;

            if(remindDPer.getCount_send() >= 1) {
                if (command.equals("/done")) {
                    commands.get(chatId).add(command);
                    this.sendMessageService.sendMessage(chatId, "SPAM IS STOP");
                    commands.get(chatId).clear();
                    return true;

            } else this.sendMessageService.sendMessage(chatId, "wrong command to stop. " +
                        "You need /done");
                return false;
        }
            else {
                this.sendMessageService.sendMessage(chatId, "Еще не было выслано напоминаний"); }
            return false;
    }


    private void innerExecuting() {
        String chatId = telegramBot.hidenPackage.service.RemindServiceImpl.
                newRemindService().getRemindById(1).getChatId();

        commands.putIfAbsent(chatId, new ArrayList<>());
        String stop;
        String executeDate = telegramBot.hidenPackage.service.
                RemindServiceImpl.newRemindService().getRemindById(1).getRemindDate();
            if (!lastCommand(chatId).equals("/done")) {
                stop = "";
            } else stop = lastCommand(chatId);
            if(executeDate.equals(SendRemind.currentDate())){
            this.sendAnotherRemind.send(stop);}
        }
}


























