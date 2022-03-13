package telegramBot.service;

import telegramBot.dao.MessageDAOImpl;
import telegramBot.entity.Message;

import java.util.List;

public class MessageServiceImpl implements MessageService {

    private MessageDAOImpl messageDAO;

    public MessageServiceImpl(MessageDAOImpl messageDAO){
        this.messageDAO = (MessageDAOImpl) messageDAO;

    }

    @Override
    public void save(Message message) {
        this.messageDAO.save(message);
    }

    @Override
    public void deleteAllMessages() {
        this.messageDAO.deleteAllMessages();
    }


    public static MessageServiceImpl newMessageService(){
        return new MessageServiceImpl(new MessageDAOImpl());
    }

    @Override
    public List<Message> getAllMessages() {
        return this.messageDAO.getAllMessages();
    }
}
