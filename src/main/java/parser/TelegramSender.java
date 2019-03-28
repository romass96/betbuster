package parser;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;

public class TelegramSender {

    private static final long CHAT_ID = 392160746;
    private static final String API_TOKEN = "895529191:AAFSuh0o0MdGCN4pduT0Ake5enr8lCLCjyk";

    public static void main(String[] args) throws IOException {
        sendMessage("https://www.myscore.com.ua/match/jaY3Dgd9/#match-summary");
    }

    public static void sendMessage(String message) throws IOException {
        String urlString = "https://api.telegram.org/bot%s/sendMessage?chat_id=%s&text=%s";

        urlString = String.format(urlString, API_TOKEN, CHAT_ID, message);

        URL url = new URL(urlString);
        URLConnection conn = url.openConnection();

        StringBuilder sb = new StringBuilder();
        InputStream is = new BufferedInputStream(conn.getInputStream());
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String inputLine = "";
        while ((inputLine = br.readLine()) != null) {
            sb.append(inputLine);
        }
        String response = sb.toString();

    }
}
