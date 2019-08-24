package discord2telegram;

public class Discord2TelegramBot {
    public static void main(String[] args) {
        DiscordListener discord = new DiscordListener();
        TelegramListener telegram = new TelegramListener(discord);
        discord.setTelegram(telegram);
    }

}
