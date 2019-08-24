package discord2telegram;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Channel;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.List;

public class DiscordListener extends ListenerAdapter {
    private TelegramListener telegram;
    private List<Long> channels;
    private String token;
    private JDA jda;
    private boolean started;

    public static void main(String[] args) {
        DiscordListener discord = new DiscordListener();
        discord.stop();
    }

    public DiscordListener()  {
        token = Utils.getFirstLineFromFile("User.token");
        channels = Utils.convertStringToLong(Utils.getAllLinesFromFile("DiscordChannels.setup"));

        makeJDA();
    }

    public boolean makeJDA()  {
        System.out.println("inside makeJDA, token: " + token);

        try {
            JDABuilder builder = new JDABuilder(AccountType.CLIENT);
            builder.setToken(token);
            builder.addEventListener(this);
            jda = builder.build();
            jda.awaitReady();

            return true;
        }
        catch (Exception e)  {
            e.printStackTrace();
            return false;
        }
    }

    public DiscordListener(TelegramListener telegram)  {
        this();
        this.telegram = telegram;
    }

    public void setTelegram(TelegramListener telegram)  {
        this.telegram = telegram;
    }

    public void setToken(String token)  {
        this.token = token;
    }

    public void start()  {started =  true;}
    public void stop()  {started = false;}

    public boolean isOn()  {return started;}
    public boolean isOff()  {return !started;}

    public void setChannels(List<Long> channels)  {
        this.channels = channels;
    }

    public String viewChannels()  {
        String result = "";

        int count=1;
        for (Long id: channels)  {
            System.out.println("id: " + id);
            Channel channel = jda.getTextChannelById(id);

            if (channel!=null) {
                result += "*DiscordChannel" + count + "* " + "_" + channel.getName() + "_" + " from Server *" + channel.getGuild().getName() + "*, " +
                        "id = `" + id + "`\n";
                count++;
            }
        }

        return result;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event)  {
        Long channelId = ((MessageReceivedEvent) event).getChannel().getIdLong();
        if (channels.contains(channelId)) {
            Message discordMessage = ((MessageReceivedEvent) event).getMessage();

            System.out.println(discordMessage.getContentRaw());

            SendMessage sendMessage = new SendMessage();
            sendMessage.setText(discordMessage.getContentRaw());

            if (isOn())  {
                for (Long telegramChannel : telegram.getChannels()) {
                    sendMessage.setChatId(telegramChannel);
                    telegram.sendMessage(discordMessage, telegramChannel);
                }
            }
        }
    }
}
