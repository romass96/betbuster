import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

public class TelegramBot extends TelegramLongPollingBot {

    private static final long CHAT_ID = 392160746;
    private static final String API_TOKEN = "895529191:AAFSuh0o0MdGCN4pduT0Ake5enr8lCLCjyk";

    public void onUpdateReceived(Update update) {
        if (update.getMessage().getChatId() == CHAT_ID) {
            sendMessage("https://www.myscore.com.ua/match/jaY3Dgd9/#match-summary");
        }
    }

    public String getBotUsername() {
        return "BetBuster";
    }

    public String getBotToken() {
        return API_TOKEN;
    }

    private void sendMessage(String text) {
        SendMessage message = new SendMessage().setChatId(CHAT_ID).setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

}
