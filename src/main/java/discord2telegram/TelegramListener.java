package discord2telegram;

import net.dv8tion.jda.core.entities.Channel;
import net.dv8tion.jda.core.entities.Message;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TelegramListener extends TelegramLongPollingBot {
    private List<Long> channels;
    private String token;

    private enum Stage {NORMAL, SWITCH_DISCORD_USER, ADD_DISCORD, ADD_TELEGRAM, REMOVE_DISCORD, REMOVE_TELEGRAM;}
    private Stage stage;

    int countKeyboard = 0;
    private DiscordListener discord;

    static {
        ApiContextInitializer.init();

    }

    public TelegramListener()  {
        token = Utils.getFirstLineFromFile("TelegramBot.token");
        channels = Utils.convertStringToLong(Utils.getAllLinesFromFile("TelegramChannels.setup"));
        stage = Stage.NORMAL;

        TelegramBotsApi botsApi = new TelegramBotsApi();

        try {
            botsApi.registerBot(this);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public TelegramListener(DiscordListener discord)  {
        this();
        this.discord = discord;
        System.out.println("made discord: " + discord.isOn());
    }

    public void setDiscord(DiscordListener discord)  {this.discord = discord;}

    public List<Long> getChannels()  {
        return channels;
    }

    @Override
    public String getBotUsername() {
        return "Discord2TelegramBot";
    }

    @Override
    public String getBotToken() {
        return token;
    }

    @Override
    public void onUpdateReceived(Update update) {
        // We check if the update has a message and the message has text
        if (update.hasMessage() && update.getMessage().hasText()) {
            long chatId = update.getMessage().getChatId();
            String message = update.getMessage().getText();

            SendMessage reply = new SendMessage();
            reply.setChatId(chatId);
            reply.setParseMode("Markdown");

            if (stage!=Stage.NORMAL && message.equalsIgnoreCase("/cancel"))  {
                stage = Stage.NORMAL;
                reply.setText("Canceled the command");
            }

            if (stage==Stage.ADD_DISCORD)  {
                boolean isAdded = Utils.addLineToFile(Utils.stringToLong(message), "DiscordChannels.setup");
                if (isAdded)  {
                    reply.setText("Added new Discord channel to the `DiscordChannels.setup` file!");
                    stage = Stage.NORMAL;
                    discord.setChannels(Utils.convertStringToLong(Utils.getAllLinesFromFile("DiscordChannels.setup")));
                }
                else  {
                    reply.setText("Something went wrong. Are you sure it's a correct Discord channel id? Try to send another or use /cancel command");
                }


            }
            else if (stage==Stage.ADD_TELEGRAM)  {
                boolean isAdded = Utils.addLineToFile(Utils.stringToLong(message), "TelegramChannels.setup");
                if (isAdded)  {
                    reply.setText("Added new Telegram channel to the `TelegramChannels.setup` file!");
                    stage = Stage.NORMAL;
                    channels = Utils.convertStringToLong(Utils.getAllLinesFromFile("TelegramChannels.setup"));
                }
                else  {
                    reply.setText("Something went wrong. Are you sure it's a correct Telegram channel id? Try to send another or use /cancel command");
                }

            }
            else if (stage==Stage.REMOVE_DISCORD)  {
                boolean isRemoved = Utils.removeLineFromFile(message, "DiscordChannels.setup");
                if (isRemoved)  {
                    reply.setText("Removed that Discord channel from `DiscordChannels.setup`!");
                    stage = Stage.NORMAL;
                    discord.setChannels(Utils.convertStringToLong(Utils.getAllLinesFromFile("DiscordChannels.setup")));
                }
                else  {
                    reply.setText("Something went wrong. Are you sure it's a correct Discord channel id? Try to send another or use /cancel command");
                }
            }
            else if (stage==Stage.REMOVE_TELEGRAM)  {
                boolean isRemoved = Utils.removeLineFromFile(message, "TelegramChannels.setup");
                if (isRemoved)  {
                    reply.setText("Removed that Telegram channel from `TelegramChannels.setup`!");
                    stage = Stage.NORMAL;
                    channels = Utils.convertStringToLong(Utils.getAllLinesFromFile("TelegramChannels.setup"));
                }
                else  {
                    reply.setText("Something went wrong. Are you sure it's a correct Telegram channel id? Try to send another or use /cancel command");
                }
            }
            else if (stage==Stage.SWITCH_DISCORD_USER)  {
                boolean isSwitched = Utils.replaceContent(message, "User.token");
                if (isSwitched)  {
                    discord.setToken(Utils.getFirstLineFromFile("User.token"));
                    boolean madeJDA = discord.makeJDA();
                    if (madeJDA)  {
                        reply.setText("Switched Discord user in `User.token`");
                        stage = Stage.NORMAL;
                    }
                    else  {
                        reply.setText("Something went wrong. Are you sure it's a correct Discord user's token? Try to send another or use /cancel command");
                    }
                }
                else  {
                    reply.setText("Something went wrong. Are you sure it's a correct Discord user's token? Try to send another or use /cancel command");
                }
            }

            else if (stage==Stage.NORMAL)  {
                switch (message)  {
                    case "/start":
                        discord.start();
                        reply.setText("Started the bot!");
                        break;
                    case "/stop":
                        discord.stop();
                        reply.setText("Stopped the bot!");
                        break;
                    case "/status":
                        reply.setText("The bot is " + ((discord.isOff()) ? "not " : "") + "monitoring Discord channels");
                        break;
                    case "/viewdiscordchannels":
                        try  {
                            String viewChannels = discord.viewChannels();
                            reply.setText(discord.viewChannels());
                        }
                        catch (Exception e)  {
                            reply.setText("Something went wrong. Please, check your Discord user's token");
                        }
                        break;
                    case "/viewtelegramchannels":
                        reply.setText(viewChannels());
                        break;
                    case "/adddiscordchannel":
                        reply.setText("What is the new Discord channel's id?");
                        stage = Stage.ADD_DISCORD;
                        break;
                    case "/addtelegramchannel":
                        reply.setText("What is the new Telegram channel's id?");
                        stage = Stage.ADD_TELEGRAM;
                        break;
                    case "/removediscordchannel":
                        reply.setText("Which Discord channel would you like to remove? Send its id");
                        stage = Stage.REMOVE_DISCORD;
                        break;
                    case "/removetelegramchannel":
                        reply.setText("Which Telegram channel would you like to remove? Send its id");
                        stage = Stage.REMOVE_TELEGRAM;
                        break;
                    case "/switchdiscorduser":
                        reply.setText("What's your Discord user's token?");
                        stage = Stage.SWITCH_DISCORD_USER;
                        break;
                    default:
                        reply.setText("Unknown command");
                        break;
                }
            }

            try {
                execute(reply); // Call method to send the message
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
            // }
        }

    }

    public void sendMessage(Message message, Long chatId)  {
        String text = message.getContentRaw();

        if (text!=null && text.length()!=0)  {
            sendMessage(text, chatId);
        }

        List<Message.Attachment> attachments = message.getAttachments();
        if (attachments!=null && attachments.size()>0)  {
            for (Message.Attachment attachment: attachments)  {
                sendMessage(attachment.getUrl(), chatId);
            }
        }
    }

    public void sendMessage(File document, Long chatId) {
        SendDocument sendDocument = new SendDocument();
        sendDocument.setChatId(chatId);
        sendDocument.setDocument(document);
        try {
            execute(sendDocument);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String text, Long chatId)  {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText(text);
        sendMessage.setChatId(chatId);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public String startMonitoring()  {
        System.out.println("inside startmornit");

        if (discord.isOn())  {
            discord.stop();
            System.out.println("turning off");
            return "turned off";
        }
        else  {
            discord.start();
            System.out.println("started discord");
            return "turned on";
        }
    }



    public String viewChannels()  {
        String result = "";

        int count = 1;
        for (Long id: channels)  {
            result += "*TelegramChannel" + count + "*: " +  "`" + id + "`\n";
            count++;
        }

        return result;
    }


}




 /*
    public InlineKeyboardMarkup makeKeyboard()  {

        System.out.println("Inside makeKeyboard; hasStarted = " + discord.isOn());
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

        InlineKeyboardButton discordChannelsButton= new InlineKeyboardButton();
        discordChannelsButton.setText("View monitored Discord Channels");
        discordChannelsButton.setCallbackData(discord.viewChannels());

        InlineKeyboardButton telegramChannelsButton = new InlineKeyboardButton();
        telegramChannelsButton.setText("View destination Telegram channels");
        telegramChannelsButton.setCallbackData(viewTelegramChannels());

        InlineKeyboardButton startMonitoring = new InlineKeyboardButton();
        startMonitoring.setText(discord.isOn() ? "Stop monitoring" : "Start" + " monitoring");
        startMonitoring.setCallbackData(startMonitoring());

        List<InlineKeyboardButton> row0 = new ArrayList<>();
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        List<InlineKeyboardButton> row2 = new ArrayList<>();

        row0.add(startMonitoring);
        row1.add(discordChannelsButton);
        row2.add(telegramChannelsButton);
        row2.add(new InlineKeyboardButton().setText("test").setCallbackData("test"));

        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        rowList.add(row0);
        rowList.add(row1);
        rowList.add(row2);
        inlineKeyboardMarkup.setKeyboard(rowList);

        return inlineKeyboardMarkup;
    }

        public void updateKeyboard()  {
        System.out.println("updatekeyboard");
        keyboard.getKeyboard().get(0).get(0).setText(discord.isOn() ? "Stop monitoring" : "Start monitoring");
    }
    */
