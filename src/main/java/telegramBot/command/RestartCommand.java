package telegramBot.command;

import org.telegram.telegrambots.meta.api.objects.Update;
import telegramBot.bot.TelegramBot;
import telegramBot.entity.Remind;
import telegramBot.service.RemindServiceImpl;
import telegramBot.service.SendMessageService;

public class RestartCommand implements Command{
    public static String RESTART_COMMAND = "Вы возообновили напоминания.";

    private SendMessageService sendMessageService;

    public RestartCommand(SendMessageService sendMessageService){

        this.sendMessageService = sendMessageService;
    }

    @Override
    public boolean execute(Update update) {
        String chatId = update.getMessage().getChatId().toString();
        if(this.sendMessageService.sendMessage(chatId, RESTART_COMMAND)){
            restart(chatId);
        }
        return true;
    }

    private void restart(String chatId){
        for(Remind r: RemindServiceImpl.newRemindService().getAllRemindsFromDB()){
            if(r.getDetails().getChatIdToSend().equals(chatId))
                RemindServiceImpl.newRemindService().updateIsStopField(r, false);}
    }

}

