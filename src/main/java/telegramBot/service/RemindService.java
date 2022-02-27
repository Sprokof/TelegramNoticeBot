package telegramBot.service;

import telegramBot.entity.Remind;

import java.util.List;

public interface RemindService {
    boolean saveRemind(Remind remind);
    void deleteRemind(int index);
    void updateRemindDateField(Remind remind, String newDate);
    void updateMaintenanceField(Remind remind, String maintenance);
    List<Remind> getAllRemindsFromDB();
    Remind getRemindById(int id);
    List<Remind> getAllExecutingRemindsByChatId(String chatId);
    boolean isContainsInDB(Remind remind);
    void updateTimeToSendField(Remind remind, boolean flag);
    void updateCountSendField(Remind remind, int count);
    void updateSendHourField(Remind remind, int hour);
    void updateIsStopField(Remind remind, boolean flag);

}
