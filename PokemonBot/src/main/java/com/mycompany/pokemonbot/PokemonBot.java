package com.mycompany.pokemonbot;

import java.io.File;
import java.io.IOException;
import java.util.*;

import javax.xml.parsers.ParserConfigurationException;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.intent.Intent;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.xml.sax.SAXException;

/**
 * @group The People Project
 * @author @BrandonDeB, @YamiKir
 * @date 4/11/2023
 */
public class PokemonBot {

    private static final Map<Long, PokemonBot> bots = new HashMap<Long, PokemonBot>();
    public Long serverID;
    public UserCommands commands;
    public TextChannel defaultChannel;

    PokemonBot(Long serverID, TextChannel defaultChannel, DiscordApi api) {
        commands = new UserCommands(serverID);
        this.serverID = serverID;
        this.defaultChannel = defaultChannel;
        File f = new File("PokemonBot\\servers\\" + serverID + ".xml");
        if (f.exists() && !f.isDirectory()) {
            commands.load(f);
        }
    }

    public static void main(String[] args) throws SAXException, IOException, ParserConfigurationException {

        // login you can change the token to use on your own personal bots just
        // rememeber to change back when committing
        DiscordApi api = new DiscordApiBuilder()
                .setToken("MTEwMzM5MzAzMDA3OTI1MDU4NA.G-Md0t.8lbBNUlM0IVvPuDfafqqvMF3_0-85LJkum1Ddk")
                .addIntents(Intent.MESSAGE_CONTENT)
                .login().join();

        for (Server server : api.getServers()) {
            bots.put(server.getId(), new PokemonBot(server.getId(), server.getTextChannels().get(0), api));
        }

        // when the bot joins a server during runtime it will generate new instance for
        // the server
        api.addServerJoinListener(event -> {
            File f = new File("PokemonBot\\servers\\" + event.getServer().getId() + ".xml");
            if (f.exists() && !f.isDirectory()) {
                bots.get(event.getServer().getId()).commands.load(f);
            } else {
                bots.put(event.getServer().getId(),
                        new PokemonBot(event.getServer().getId(), event.getServer().getTextChannels().get(0), api));
            }

        });
        api.addSelectMenuChooseListener(selectionEvent -> {
            if (selectionEvent.getSelectMenuInteraction() != null) {
                User selector = selectionEvent.getSelectMenuInteraction().getUser();
                selectionEvent.getInteraction().createImmediateResponder().respond();
                System.out.println("SelectionMenu Name: " + selectionEvent.getSelectMenuInteraction().getCustomId());

                if (selectionEvent.getSelectMenuInteraction().getCustomId().equals("pokemonBP")) {
                    System.out.println("SelectionMenu Name: TRUE");
                    bots.get(selectionEvent.getInteraction().getServer().get().getId()).commands
                            .backpackMenu(selectionEvent, api);
                }

                if (selectionEvent.getSelectMenuInteraction().getCustomId().equals("BattlepokemonAdam")
                        || selectionEvent.getSelectMenuInteraction().getCustomId().equals("BattlepokemonSteve")) {
                    if (selectionEvent.getSelectMenuInteraction().getCustomId().equals("BattlepokemonAdam")
                            && selector.equals(selectionEvent.getSelectMenuInteraction().getMessage()
                                    .getMentionedUsers().get(0))) {
                        bots.get(selectionEvent.getInteraction().getServer().get().getId()).commands
                                .battleMenu(selectionEvent, api);
                    } else if (selectionEvent.getSelectMenuInteraction().getCustomId().equals("BattlepokemonSteve")
                            && selector.equals(selectionEvent.getSelectMenuInteraction().getMessage()
                                    .getMentionedUsers().get(1))) {
                        bots.get(selectionEvent.getInteraction().getServer().get().getId()).commands
                                .battleMenu(selectionEvent, api);
                    }

                }
                if (selectionEvent.getSelectMenuInteraction().getCustomId().equals("TradepokemonAdam")
                        || selectionEvent.getSelectMenuInteraction().getCustomId().equals("TradepokemonSteve")) {
                    if (selectionEvent.getSelectMenuInteraction().getCustomId().equals("TradepokemonAdam")
                            && selector.equals(selectionEvent.getSelectMenuInteraction().getMessage()
                                    .getMentionedUsers().get(0))) {
                        bots.get(selectionEvent.getInteraction().getServer().get().getId()).commands.tradeMenu(
                                selectionEvent,
                                selectionEvent.getSelectMenuInteraction().getMessage().getMentionedUsers().get(0), api);
                    }

                    if (selectionEvent.getSelectMenuInteraction().getCustomId().equals("TradepokemonSteve")
                            && selector.equals(selectionEvent.getSelectMenuInteraction().getMessage()
                                    .getMentionedUsers().get(0))) {
                        bots.get(selectionEvent.getInteraction().getServer().get().getId()).commands.tradeMenu(
                                selectionEvent,
                                selectionEvent.getSelectMenuInteraction().getMessage().getMentionedUsers().get(1), api);
                    }

                }
            }

        });
        // Message creation listener
        api.addMessageCreateListener(event -> {
            if (event.getMessageContent().equals("!defaultchannel")) {
                bots.get(event.getServer().get().getId()).defaultChannel = event.getChannel();
            }
            bots.get(event.getServer().get().getId()).commands.incomingMessage(event, api);

        });

        // adding Listeners to the api so it can hear the buttonClickEvent and react.
        api.addButtonClickListener(buttonEvent -> {

            if (buttonEvent.getButtonInteraction() != null) {
                String buttonCustomId = buttonEvent.getButtonInteraction().getCustomId();
                System.out.println(buttonCustomId);
                buttonEvent.getButtonInteraction().createImmediateResponder().respond();
                // If the button pressed is a trade button
                if (buttonCustomId.equals("Cancel Trade") || buttonCustomId.equals("Accept Trade")
                        || buttonCustomId.equals("Send Trade")) {
                    bots.get(buttonEvent.getInteraction().getServer().get().getId()).commands.tradeButton(buttonEvent,
                            api);
                }
                // If the button pressed is not a trade button
                else {
                    bots.get(buttonEvent.getInteraction().getServer().get().getId()).commands.buttonPress(buttonEvent,
                            api);
                }
            }
        });

        Timer timer = new Timer();
        TimerTask saveAll = new TimerTask() {
            @Override
            public void run() {
                for (Map.Entry<Long, PokemonBot> entry : bots.entrySet()) {
                    entry.getValue().commands.save();
                }
            }
        };

        TimerTask generate = new TimerTask() {

            @Override
            public void run() {

                for (Map.Entry<Long, PokemonBot> entry : bots.entrySet()) {
                    entry.getValue().commands.generate(entry.getValue().defaultChannel);
                }

            }
        };

        TimerTask gimme = new TimerTask() {
            @Override
        public void run() {

            for (Map.Entry<Long, PokemonBot> entry : bots.entrySet()) {
                entry.getValue().commands.gimme(entry.getValue().defaultChannel);
            }

        }
        };

        // saves all storages every 5 minutes
        timer.schedule(saveAll, 0L, 1000 * 60 * 5);
        // generates new pokemon every 2 minutes
        timer.schedule(generate, 0L, 1000 * 60 * 15);
//        timer.schedule(gimme, 0L, 1000 * 60 * 15);


    }

}