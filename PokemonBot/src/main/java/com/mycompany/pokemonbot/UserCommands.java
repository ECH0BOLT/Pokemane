package com.mycompany.pokemonbot;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageUpdater;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.Button;
import org.javacord.api.entity.message.component.SelectMenu;
import org.javacord.api.entity.message.component.SelectMenuOption;
import org.javacord.api.entity.message.embed.Embed;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.interaction.ButtonClickEvent;
import org.javacord.api.event.interaction.SelectMenuChooseEvent;
import org.javacord.api.event.message.MessageCreateEvent;
import org.xml.sax.SAXException;

public class UserCommands {
    private Pokemon recent;
    private Storage storage;
    // ListenerManager<SelectMenuChooseListener> listenerManager;

    UserCommands(){}
    UserCommands(Long serverID) {
        storage = new Storage(serverID);
    }

    /**
     * @param event
     *              takes the message event listened to by PokemonBot.java and
     *              handles it based on what is sent
     * @param api
     *              takes the api in order to build and send appropriate messages
     *              based on the event
     */
    public void incomingMessage(MessageCreateEvent event, DiscordApi api) {
        User adam = null;
        if (event.getMessageAuthor().isRegularUser() == true) {
            adam = event.getMessageAuthor().asUser().get();
            if (storage.isUser(adam)) {
                Random random = new Random();
                double randNum = random.nextDouble();
                if (randNum < .01) {
                    storage.getIndividualUser(adam.getId()).changePokeballs(3, 1);
                } else if (randNum < .05) {
                    storage.getIndividualUser(adam.getId()).changePokeballs(2, 1);
                } else if (randNum < .2) {
                    storage.getIndividualUser(adam.getId()).changePokeballs(1, 1);
                } else if (randNum < .4) {
                    storage.getIndividualUser(adam.getId()).changePokeballs(0, random.nextInt(3) + 1);
                }
            }
        } else {
            return;
        }
        TextChannel channel = event.getChannel();
        String[] split = event.getMessageContent().split("\\s+");
        Message message = event.getMessage();

        // .UserStorage.setNickname(nickname);
        switch (split[0]) {
            case "!pmstarter":
                // if commander isn't a user run starter (creates a profile and lets them pick a
                // gen 1 starter)
                // else youre a trainer already...dont be greedy and good luck catching them all
                // storage.create(starter);
                if (!storage.isUser(adam)) {
                    // if the user doesnt have an account/profile
                    boolean cm;
                    boolean sq;
                    boolean bulb;
                    cm = sq = bulb = false;

                    // MessageBuilder is a way to construct complex messages. ActionRow is container
                    // for interactable things like Buttons. Buttons come in green (success), red
                    // (danger), grey (secondary/Link), bluish (primary). this Button constructor
                    // is(customId,label, boolean disable).
                    new MessageBuilder().setContent("Select the Pokemon starter you would like: ")
                            .addComponents(
                                    ActionRow.of(
                                            Button.danger("Charmander", "Charmander", cm),
                                            Button.primary("Squirtle", " Squirtle", sq),
                                            Button.success("Bulbasaur", "Bulbasaur", bulb)))
                            .send(channel);
                } else
                    channel.sendMessage("You already have a pokemon, ya fool.");
                break;

            case "!pmhelp":
                // displays the current help text
                channel.sendMessage(
                        "The list of current commands are as follows:\n!pmstarter- lets you chose a starter if you don't have one already\n!pmbp- lets you view your backpack\n!pmhelp...\n!pmclaim- lets you claim an event pokemon\n!pmbat- lets you battle another trainer\n!pmtrade- lets you trade pokemon \n!generate- legacy command that lets you generate a random gen1 Pokemon ");
                break;

            case "!pmclaim":
                // if commander is a trainer, claim the most recent generated pokemon
                // fail conditions: not a trainer, already claimed, expired.
                if (storage.isUser(adam)) {
                    // if the user doesnt have an account/profile
                    if (recent != null) {
                        new MessageBuilder()
                                .setContent("Choose a Pokeball to try to catch the Pokemon!")
                                .addComponents(
                                        ActionRow.of(
                                                Button.primary("Pokeball", "Pokeball"),
                                                Button.primary("Great Ball", "Great Ball"),
                                                Button.primary("Ultra Ball", "Ultra Ball"),
                                                Button.primary("Master Ball", "Master Ball")))
                                .send(channel);
                    }
                } else {
                    channel.sendMessage(
                            "You haven't claimed a starter yet...You won't be able to catch pokemon without your starter\n");
                }
                break;

            case "!pmbat":
                // requires commander to be a user
                // requires target to be an user
                // max time for battle:
                // storage.battle(User,User)
                // storage.battle->battle
                System.out.println("pmbat");
                if (split.length == 2) {

                    User steve = message.getMentionedUsers().get(0);
                    System.out.println(adam.getName()+" has declared battle against "+steve.getName());
                    if (storage.isUser(adam) && storage.isUser(steve) && !adam.equals(steve)) {
                        // if the user doesnt have an account/profile
                        // Storage.battle(adam,steve, channel);
                        Battle.choosePokemon(storage.getIndividualUser(adam.getId()),
                                storage.getIndividualUser(steve.getId()), channel);
                    } else
                        channel.sendMessage(
                                "You have nothing to battle with...\nEither you or your buddy must use !pmstarter to begin your adventure");
                } else
                    channel.sendMessage("You have provided an invalid command.");
                break;

            case "!pmtrade":
                // requires 2 valid userss (commander, target)
                // once in trade they can select pokemon or cancel
                // storage.trade(User,User)
                // storage.trade->trade
                String[] parts = message.getContent().split(" ");
                if (parts.length == 2) {
                    User steve = message.getMentionedUsers().get(0);
                    Server server = channel.asServerTextChannel().get().getServer();
                    String nickA = adam.getDisplayName(server);
                    String nickB = steve.getDisplayName(server);

                    if (!adam.getIdAsString().equals(steve.getIdAsString())) {
                        if (storage.isUser(adam) && storage.isUser(steve)) {
                            UserStorage adamStorage = storage.getIndividualUser(adam.getId());
                            adamStorage.setNickname(nickA);
                            UserStorage steveStorage = storage.getIndividualUser(steve.getId());
                            steveStorage.setNickname(nickB);

                            if (adamStorage.isTrade() && steveStorage.isTrade()) {
                                Trade.createPokemonButtons(adamStorage, steveStorage, channel);
                            } else
                                channel.sendMessage(
                                        "You must finish your other trade transaction before starting a new one. Both of you.");

                        } else if (!storage.isUser(adam)) {
                            channel.sendMessage("You have nothing to trade with. Type '!pmstarter' to get started!");
                        } else if (!storage.isUser(steve)) {
                            channel.sendMessage(
                                    steve.getNickname(server).orElse(steve.getName()) + " has nothing to trade with. <@"
                                            + steve.getIdAsString() + ">, try typing '!pmstarter' to get started!");
                        }
                    } else if (adam.getIdAsString().equals(steve.getIdAsString())) {
                        channel.sendMessage("You can't trade with yourself, " + adam.getNickname(server) + "!");
                    }
                } else {
                    channel.sendMessage(
                            "Incorrect formatting. Please use '!pmtrade @(user you wish to trade with) . \n");
                }
                break;

            case "!generate":
                // temp command that generates an event/"wild" Pokemon

                if(adam.getIdAsString().contentEquals("251887678935662602") || adam.getIdAsString().contentEquals("1110682517658402897")) {
                    if(split.length == 3){generateSpeciesLevel(channel, split[1], split[2]);}else if(split.length == 2){generateSpecies(channel, split[1]);} else generate(channel);}
//                channel.sendMessage(adam.getIdAsString());
                break;

            case "!pmfart":
                channel.sendMessage("Pffffft!");
            break;
            case "!pmbp":
                if (storage.isUser(adam)) {
                    new MessageBuilder().setContent("Welcome to your handy-dandy Backpack: ")
                            .addComponents(
                                    ActionRow.of(
                                            Button.primary("Items", "Items"), Button.primary("Pokemon", "Pokemon")))
                            .send(channel);

                    // channel.sendMessage(storage.backpack(adam.getId()));
                } else
                    channel.sendMessage("You don't have anything right now...ya goofy.");
                break;

            case "!save":
                storage.writeToXML();
        }
    }

    public void setRecent(Pokemon p) {
        recent = p;
    }

    public Pokemon getRecent() {
        return recent;
    }

    public void buttonPress(ButtonClickEvent buttonEvent, DiscordApi api) {


        String buttonCustomId = buttonEvent.getButtonInteraction().getCustomId();
        User adam = buttonEvent.getInteraction().getUser();
        UserStorage adamStorage = storage.getIndividualUser(adam.getId());
        TextChannel ch = buttonEvent.getButtonInteraction().getChannel().get();

        if (buttonCustomId.equals("delete")) {
            buttonEvent.getButtonInteraction().getMessage().delete();
        }

        // LOGIC FOR PMSTARTER BUTTONS
        if (!storage.isUser(adam) && (buttonCustomId.equals("Charmander") || buttonCustomId.equals("Squirtle")
                || buttonCustomId.equals("Bulbasaur"))) {
            UserStorage newUser = new UserStorage(adam.getId());
            int chosenStarter = 0;

            buttonEvent.getButtonInteraction().createImmediateResponder().setContent("You've chosen: ")
                    .append(buttonEvent.getButtonInteraction().getCustomId()).append(" as your starter!").respond();
            new MessageUpdater(buttonEvent.getButtonInteraction().getMessage()).removeAllComponents()
                    .addActionRow(Button.danger("Charmander", "Charmander", true),
                            Button.primary("Squirtle", " Squirtle", true),
                            Button.success("Bulbasaur", "Bulbasaur", true))
                    .applyChanges();
            // buttonEvent.getButtonInteraction().getMessage().delete();

            if (buttonCustomId.equals("Charmander")) {
                chosenStarter = 4;
            } else if (buttonCustomId.equals("Squirtle")) {
                chosenStarter = 7;
            } else if (buttonCustomId.equals("Bulbasaur")) {
                chosenStarter = 1;
            }
            try {
                Pokemon p = new Pokemon(chosenStarter, 10);
                newUser.addPokemon(p);
                ch.sendMessage("Congrats! You have chosen " + p.getSpecies() + " as your starter");

            } catch (SAXException | IOException | ParserConfigurationException e) {
                e.printStackTrace();
            }
            storage.newUser(adam.getId(), newUser);
        }

        if (storage.isUser(adam) && (buttonCustomId.equals("Charmander") || buttonCustomId.equals("Squirtle")
                || buttonCustomId.equals("Bulbasaur"))) {
            buttonEvent.getButtonInteraction().createImmediateResponder().setContent(
                    "Silly Billy, you already got a starter.... Don't be greedy now. Good luck on catching them all[TM]\n")
                    .respond();
        }

        // LOGIC FOR PMCLAIM BUTTONS
        if (storage.isUser(adam) && (buttonCustomId.equals("Pokeball") || buttonCustomId.equals("Great Ball")
                || buttonCustomId.equals("Ultra Ball") || buttonCustomId.equals("Master Ball"))) {

            Random random = new Random();
            Server server = buttonEvent.getInteraction().getChannel().get().asServerTextChannel().get().getServer();
            // Catch rates for each type of Pokeball
            double catchRate = -1;
            if (buttonCustomId.equals("Pokeball") && adamStorage.getPokeballs()[0] != 0) {
                catchRate = 0.4;
                adamStorage.changePokeballs(0, -1);
            } else if (buttonCustomId.equals("Great Ball") && adamStorage.getPokeballs()[1] != 0) {
                catchRate = 0.60;
                adamStorage.changePokeballs(1, -1);
            } else if (buttonCustomId.equals("Ultra Ball") && adamStorage.getPokeballs()[2] != 0) {
                catchRate = 0.80;
                adamStorage.changePokeballs(2, -1);
            } else if (buttonCustomId.equals("Master Ball") && adamStorage.getPokeballs()[3] != 0) {
                catchRate = 1.0;
                adamStorage.changePokeballs(3, -1);
            }
            if (recent != null && recent.getSpecies() != null) {
                if (catchRate != -1 && random.nextDouble() < catchRate) {
                    // The Pokemon was caught!

                    storage.claim(adam, recent, ch);
                    ch.sendMessage(adam.getDisplayName(server) + " caught " + recent.getSpecies() + "!");
                    new MessageUpdater(buttonEvent.getButtonInteraction().getMessage()).removeAllComponents()
                            .addActionRow(Button.primary("Pokeball", "Pokeball", true),
                                    Button.primary("Great Ball", "Great Ball", true),
                                    Button.primary("Ultra Ball", "Ultra Ball", true),
                                    Button.primary("Master Ball", "Master Ball", true))
                            .applyChanges();
                    recent = null;
                } else if (catchRate == -1) {
                    ch.sendMessage("Choose a different Pokeball to try to catch " + recent.getSpecies());
                } else {
                    ch.sendMessage("Oh no! " + recent.getSpecies() + " broke free!");
                }
            }
        }

        // LOGIC FOR PMBP BUTTONS
        if (storage.isUser(adam) && (buttonCustomId.equals("Items") || buttonCustomId.equals("Pokemon")
                || buttonCustomId.equals("Close Backpack"))) {
            // POKEMON button PMBP
            if (buttonCustomId.equals("Pokemon")) {
                List<SelectMenuOption> optionsAdam = new ArrayList<SelectMenuOption>();
                int bpCount = 0;
                for (Pokemon p : storage.getIndividualUser(adam.getId()).getPokemon()) {
                    optionsAdam
                            .add(SelectMenuOption.create(p.getSpecies(), String.valueOf(bpCount),
                                    "CP: " + Integer.toString(p.getCP())));
                    bpCount++;
                }
                new MessageUpdater(buttonEvent.getButtonInteraction().getMessage())
                        .removeAllComponents().setContent("Your Pokemon").addActionRow(
                                SelectMenu.createStringMenu("pokemonBP", "Click here to select a pokemon",
                                        optionsAdam, false))
                        .addActionRow(Button.primary("Items", "Items"),
                                Button.danger("Close Backpack", "Close Backpack"))
                        .applyChanges();

            }
            // ITEMS button PMBP
            if (buttonCustomId.equals("Items")) {
                String ballString = "";
                int tempBalls[] = adamStorage.getPokeballs();
                ballString = "Pokeball: " + tempBalls[0];
                ballString += ", Great Ball: " + tempBalls[1];
                ballString += ", Ultra Ball: " + tempBalls[2];
                ballString += ", Master Ball: " + tempBalls[3];
                new MessageUpdater(buttonEvent.getButtonInteraction().getMessage()).removeAllComponents()
                        .setContent("Your Items: " + ballString).addActionRow(
                                Button.primary("Pokemon", "Pokemon"))
                        .applyChanges();
            }
            if (buttonCustomId.equals("Close Backpack")) {
                new MessageUpdater(buttonEvent.getButtonInteraction().getMessage()).removeAllComponents()
                        .setContent("Backpack Closed")
                        .applyChanges();

            }
        }
        // LOGIC for PMBP embeds
        if (buttonCustomId.equals("candy") || buttonCustomId.equals("evolve") || buttonCustomId.equals("release")) {
            Embed tempEmbed = buttonEvent.getButtonInteraction().getMessage().getEmbeds().get(0);
            String species = tempEmbed.getTitle().get();
            String p[] = tempEmbed.getDescription().get().split(": ");
            int cp = Integer.parseInt(p[1]);
            String t1 = tempEmbed.getFields().get(0).getValue();
            String t2 = tempEmbed.getFields().get(1).getValue();
            int level = Integer.parseInt(tempEmbed.getFields().get(2).getValue());
            Pokemon mentionedPokemon = adamStorage.findPokemon(species, cp, t1, t2, level);
            if (mentionedPokemon != null) {
                if (buttonCustomId.equals("release")) {

                    adamStorage.releasePokemon(mentionedPokemon);
                    new MessageUpdater(buttonEvent.getButtonInteraction().getMessage()).removeAllComponents()
                            .setContent(mentionedPokemon.getSpecies()
                                    + " has left your company. Do not be sad... you did the right thing.")
                            .applyChanges();
                } else {
                    if (buttonCustomId.equals("candy")) {
                        if (adamStorage.getCandy()[mentionedPokemon.getBaseForm()] >= 3
                                && mentionedPokemon.getLevel() != 100) {
                            mentionedPokemon.levelUp();
                            adamStorage.changeCandy(mentionedPokemon.getBaseForm(), -3);
                        } else {
                            ch.sendMessage("Not enough candies to level up");
                        }
                    }
                    if (buttonCustomId.equals("evolve")) {
                        // add to check the evolve level
                        if (adamStorage.getCandy()[mentionedPokemon.getBaseForm()] >= mentionedPokemon.getEvolutionCost()
                                && mentionedPokemon.getEvolutionCost() != -1) {
                            adamStorage.changeCandy(mentionedPokemon.getBaseForm(), -mentionedPokemon.getEvolutionCost());
                            mentionedPokemon.evolve();
                        }else if(mentionedPokemon.getEvolutionCost() == -1){ch.sendMessage(mentionedPokemon.getSpecies() + " does not evolve");

                        } else {
                            ch.sendMessage("Not enough candies to evolve ");
                        }
                    }
                    new MessageUpdater(buttonEvent.getButtonInteraction().getMessage())
                            .addEmbed(mentionedPokemon.toMessage().setFooter("Candies: " + adamStorage.getCandy()[mentionedPokemon.getBaseForm()])).addActionRow(
                                    Button.primary("candy", "Level Up"),
                                    Button.primary("evolve", "Evolve"),
                                    Button.primary("release", "Release"),
                                    Button.danger("delete", "Hide"))
                            .applyChanges();
                }

            } else
                System.out.println(adam.getName() + " does not have permission to interact with this bp menu");
        }

    }

    public void backpackMenu(SelectMenuChooseEvent selectionEvent, DiscordApi api) {

        User guy = selectionEvent.getInteraction().getUser();
        UserStorage adamStorage = storage.getIndividualUser(guy.getId());
        TextChannel ch = selectionEvent.getInteraction().getChannel().get();

        String str = (selectionEvent.getSelectMenuInteraction().getChosenOptions().get(0).getValue());
        if (guy.getIdAsString().equals(String.valueOf(adamStorage.getUserID()))) {
            int indexer = Integer.parseInt(str);
            Pokemon selectedMon = adamStorage.getPokemon().get(indexer);

            new MessageBuilder().addEmbed(selectedMon.toMessage().setFooter("Candies: " + adamStorage.getCandy()[selectedMon.getBaseForm()]))
                    .addActionRow(
                            Button.primary("candy", "Level Up"),
                            Button.primary("evolve", "Evolve"),
                            Button.primary("release", "Release"),
                            Button.danger("delete", "Hide")

                    ).send(ch);

            // ch.sendMessage("hi");

        }
        // selectionEvent.getInteraction().createImmediateResponder().respond();

    }

    public void save() {
        storage.writeToXML();
    }

    public void load(File f) {
        storage.load(f);
    }

    public void battleMenu(SelectMenuChooseEvent selectionEvent, DiscordApi api) {
        User guy = selectionEvent.getInteraction().getUser();

        UserStorage guyStorage = storage.getIndividualUser(guy.getId());
        TextChannel ch = selectionEvent.getInteraction().getChannel().get();
        String[] str = (selectionEvent.getSelectMenuInteraction().getChosenOptions().get(0).getValue()).split(":");
        int indexer = Integer.parseInt(str[1]);
        if (guyStorage.battleList.size() < 3) {
            guyStorage.battleList.add(guyStorage.getPokemon().get(indexer));
            if (guyStorage.battleList.size() == 3) {
                ch.sendMessage("<@" + guy.getId() + ">, you have selected all of your pokemon for the battle.");
                if(storage.getIndividualUser(selectionEvent.getSelectMenuInteraction().getMessage().getMentionedUsers().get(1).getId()).battleList.size()==3 &&
                storage.getIndividualUser(selectionEvent.getSelectMenuInteraction().getMessage().getMentionedUsers().get(0).getId()).battleList.size()==3
                ){
                    selectionEvent.getSelectMenuInteraction().getMessage().delete();{

                    Battle.battleTime(
                            storage.getIndividualUser(selectionEvent.getSelectMenuInteraction().getMessage()
                                    .getMentionedUsers().get(0).getId()),
                            storage.getIndividualUser(selectionEvent.getSelectMenuInteraction().getMessage()
                                    .getMentionedUsers().get(1).getId()),
                            ch);
                }

            }
        }
        } else
            ch.sendMessage("<@" + guy.getId() + ">, you may not send anymore to the battlefield.");
    }
    

    public void tradeMenu(SelectMenuChooseEvent selectionEvent, User guy, DiscordApi api) {

        UserStorage guyStorage = storage.getIndividualUser(guy.getId());
        String[] str = (selectionEvent.getSelectMenuInteraction().getChosenOptions().get(0).getValue()).split(":");
        int indexer = Integer.parseInt(str[1]);
        guyStorage.setTrade(guyStorage.pokemonList.get(indexer - 1));

    }

    public void generate(TextChannel channel) {
        // temp command that generates an event/"wild" Pokemon
        try {
            recent = new Pokemon();
            channel.sendMessage(recent.toMessage());
//            channel.sendMessage("ID: " + String.valueOf(recent.getID()) + " Base Form: " + String.valueOf(recent.getBaseForm()));

        } catch (SAXException | IOException | ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    public void generateSpecies(TextChannel channel, String id) {
        // temp command that generates an event/"wild" Pokemon
        try {
            recent = new Pokemon(Integer.parseInt(id));
            channel.sendMessage(recent.toMessage());
//            channel.sendMessage("ID: " + String.valueOf(recent.getID()) + " Base Form: " + String.valueOf(recent.getBaseForm()));


        } catch (SAXException | IOException | ParserConfigurationException e) {
            e.printStackTrace();
        }
    }
    public void generateSpeciesLevel(TextChannel channel, String id, String level) {
        // temp command that generates an event/"wild" Pokemon
        try {
            recent = new Pokemon(Integer.parseInt(id), Integer.parseInt(level));
            channel.sendMessage(recent.toMessage());
//            channel.sendMessage("ID: " + String.valueOf(recent.getID()) + " Base Form: " + String.valueOf(recent.getBaseForm()));

        } catch (SAXException | IOException | ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    public void tradeButton(ButtonClickEvent buttonEvent, DiscordApi api) {

        String buttonCustomId = buttonEvent.getButtonInteraction().getCustomId();
        TextChannel channel = buttonEvent.getButtonInteraction().getMessage().getChannel();
        User presser = buttonEvent.getButtonInteraction().getUser();
        Message message = buttonEvent.getButtonInteraction().getMessage();
        List<User> mentionedUsers = message.getMentionedUsers();
        User temp = mentionedUsers.get(0);
        UserStorage adamStorage = storage.getIndividualUser(mentionedUsers.get(0).getId());
        UserStorage steveStorage = storage.getIndividualUser(mentionedUsers.get(1).getId());
        ArrayList<SelectMenuOption> options = new ArrayList<>();
        ArrayList<SelectMenuOption> options2 = new ArrayList<>();
        Server server = channel.asServerTextChannel().get().getServer();
        String presserNick = presser.getDisplayName(server);

        // Allows either user mentioned in the first or second message to use the Cancel
        // Trade button
        if (buttonCustomId.equals("Cancel Trade") && (adamStorage.getUserID() == presser.getId() ||
                steveStorage.getUserID() == presser.getId())) {
            new MessageUpdater(buttonEvent.getButtonInteraction().getMessage()).removeAllComponents()
                    .setContent("This trade has been cancelled by " + presserNick + ". ")
                    .applyChanges();
            steveStorage.setTrade(null);
            adamStorage.setTrade(null);
        }

        // Allows only the user that originally types !pmtrade to use the Send Trade
        // button
        else if (buttonCustomId.equals("Send Trade") && adamStorage.getUserID() == presser.getId()) {

            // Gets the pokemon at the tradeIndex positon in each user's pokemon list
            Pokemon offered = adamStorage.trade;
            Pokemon received = steveStorage.trade;

            // Prevents the listener used in the first message from interfering with any
            // other messages

            options.add(SelectMenuOption.create(offered.getSpecies(), String.valueOf(adamStorage.getUserID()),
                    String.valueOf(adamStorage.getUserID())));
            options2.add(SelectMenuOption.create(received.getSpecies(), String.valueOf(steveStorage.getUserID()),
                    String.valueOf(steveStorage.getUserID())));

            MessageBuilder messageBuilder = new MessageBuilder()
                    .setContent("<@" + adamStorage.getIdAsString() + "> has sent you a trade offer, <@"
                            + steveStorage.getIdAsString() + ">.");

            // Creates the message to display the Pokemon being offerred by the first user
            messageBuilder.addComponents(ActionRow.of(
                    SelectMenu.createStringMenu("offeredPokemon", adamStorage.getNickname() + " offers: "
                            + offered.getSpecies() + " CP: " + offered.getCP(), options, true)));

            // Creates the message to display the Pokemon the first user wishes to receive
            messageBuilder.addComponents(ActionRow.of(
                    SelectMenu.createStringMenu("receivedPokemon", adamStorage.getNickname() + " receives: "
                            + received.getSpecies() + " CP: " + received.getCP(), options2, true)));

            // Creates the Accept Trade and Cancel Trade buttons
            messageBuilder.addComponents(ActionRow.of(
                    Button.danger("Cancel Trade", "Cancel Trade", false)));
            messageBuilder.addComponents(ActionRow.of(
                    Button.success("Accept Trade", "Accept Trade", false)));
            messageBuilder.send(channel);

            // Removes the Components from the original message and replaces it with a
            // statement confirming its success
            new MessageUpdater(message).removeAllComponents()
                    .setContent("This trade has been initiated by " + presserNick + ". ")
                    .applyChanges();

        }

        // Allows only the user receiving the trade offer to use the Accept Trade button
        if (buttonCustomId.equals("Accept Trade") && presser.getId() == steveStorage.getUserID()) {
            Pokemon offered = adamStorage.trade;
            Pokemon received = steveStorage.trade;
            // Updates the second message by removing every component and replacing it with
            // a statement confirming the trade has been accepted
            new MessageUpdater(message).removeAllComponents()
                    .setContent("This trade has been accepted. Congratulations to " + adamStorage.nickname
                            + " on your new " + received.getSpecies()
                            + " and congratulations to " + steveStorage.nickname + " on your new "
                            + offered.getSpecies() + "!")
                    .applyChanges();
            UserStorage tempStorage = storage.getIndividualUser(temp.getId());

            // Sends the trade sender's Storage, trade accepter's storage, the channel, and
            // the index containing the pokemon list locations for each user's selected
            // pokemon
            Trade.trade(tempStorage, steveStorage, channel);
        }

        // If the user clicking the buttons was not a user allowed to user them,
        // displays a message telling them they do not have permission.
        else if (buttonCustomId.equals("Cancel Trade")
                && ((presser.getId() != adamStorage.getUserID()) && presser.getId() != steveStorage.getUserID())) {
            channel.sendMessage(
                    "Nice try. You do not have permission to cancel this trade, " + presserNick + ".");
        } else if (buttonCustomId.equals("Accept Trade") && (presser.getId() != steveStorage.getUserID())) {
            channel.sendMessage(
                    "Nice try. You do not have permission to accept this trade, " + presserNick + ".");
        } else if (buttonCustomId.equals("Send Trade") && (presser.getId() != adamStorage.getUserID())) {
            channel.sendMessage(
                    "Nice try. You do not have permission to send this trade, " + presserNick + ".");
        }

    }

    public String getShiny(int index){
        return shinySprites[index - 1];
    }
    private String[] shinySprites = {
            "https://static.wikia.nocookie.net/pokemongo/images/d/d9/Bulbasaur_shiny.png/revision/latest?cb=20220516180036",
            "https://static.wikia.nocookie.net/pokemongo/images/7/72/Ivysaur_shiny.png/revision/latest?cb=20220516192251",
            "https://static.wikia.nocookie.net/pokemongo/images/c/ce/Venusaur_shiny.png/revision/latest?cb=20220516214627",
            "https://static.wikia.nocookie.net/pokemongo/images/8/8d/Charmander_shiny.png/revision/latest?cb=20220516180745",
            "https://static.wikia.nocookie.net/pokemongo/images/1/1e/Charmeleon_shiny.png/revision/latest?cb=20220516180819",
            "https://static.wikia.nocookie.net/pokemongo/images/d/d5/Charizard_shiny.png/revision/latest?cb=20220516180710",
            "https://static.wikia.nocookie.net/pokemongo/images/2/26/Squirtle_shiny.png/revision/latest?cb=20220516211808",
            "https://static.wikia.nocookie.net/pokemongo/images/6/65/Wartortle_shiny.png/revision/latest?cb=20220516215238",
            "https://static.wikia.nocookie.net/pokemongo/images/d/dd/Blastoise_shiny.png/revision/latest?cb=20220516175520",
            "https://static.wikia.nocookie.net/pokemongo/images/d/d8/Caterpie_shiny.png/revision/latest?cb=20220516180527",
            "https://static.wikia.nocookie.net/pokemongo/images/6/66/Metapod_shiny.png/revision/latest?cb=20220516200152",
            "https://static.wikia.nocookie.net/pokemongo/images/7/73/Butterfree_shiny.png/revision/latest?cb=20220516180142",
            "https://static.wikia.nocookie.net/pokemongo/images/d/db/Weedle_shiny.png/revision/latest?cb=20220516215348",
            "https://static.wikia.nocookie.net/pokemongo/images/4/4e/Kakuna_shiny.png/revision/latest?cb=20220516192730",
            "https://static.wikia.nocookie.net/pokemongo/images/8/8d/Beedrill_shiny.png/revision/latest?cb=20220516175141",
            "https://static.wikia.nocookie.net/pokemongo/images/3/32/Pidgey_shiny.png/revision/latest?cb=20220516202736",
            "https://static.wikia.nocookie.net/pokemongo/images/0/02/Pidgeotto_shiny.png/revision/latest?cb=20220516202715",
            "https://static.wikia.nocookie.net/pokemongo/images/8/85/Pidgeot_shiny.png/revision/latest?cb=20220516202651",
            "https://static.wikia.nocookie.net/pokemongo/images/3/31/Rattata_shiny.png/revision/latest?cb=20220516204320",
            "https://static.wikia.nocookie.net/pokemongo/images/c/c8/Raticate_shiny.png/revision/latest?cb=20220516204237",
            "https://static.wikia.nocookie.net/pokemongo/images/0/07/Spearow_shiny.png/revision/latest?cb=20220516211611",
            "https://static.wikia.nocookie.net/pokemongo/images/f/fa/Fearow_shiny.png/revision/latest?cb=20220516184640",
            "https://static.wikia.nocookie.net/pokemongo/images/1/1b/Ekans_shiny.png/revision/latest?cb=20220516183936",
            "https://static.wikia.nocookie.net/pokemongo/images/9/9f/Arbok_shiny.png/revision/latest?cb=20220516174213",
            "https://static.wikia.nocookie.net/pokemongo/images/f/fe/Pikachu_shiny.png/revision/latest?cb=20220516202846",
            "https://static.wikia.nocookie.net/pokemongo/images/5/5a/Raichu_shiny.png/revision/latest?cb=20220516204017",
            "https://static.wikia.nocookie.net/pokemongo/images/6/60/Sandshrew_shiny.png/revision/latest?cb=20220516205318",
            "https://static.wikia.nocookie.net/pokemongo/images/e/ed/Sandslash_shiny.png/revision/latest?cb=20220516205341",
            "https://static.wikia.nocookie.net/pokemongo/images/1/1a/Nidoran%E2%99%80_shiny.png/revision/latest?cb=20220516201234",
            "https://static.wikia.nocookie.net/pokemongo/images/5/53/Nidorina_shiny.png/revision/latest?cb=20220516201249",
            "https://static.wikia.nocookie.net/pokemongo/images/3/35/Nidoqueen_shiny.png/revision/latest?cb=20220516201205",
            "https://static.wikia.nocookie.net/pokemongo/images/c/c0/Nidoran%E2%99%82_shiny.png/revision/latest?cb=20220516201241",
            "https://static.wikia.nocookie.net/pokemongo/images/4/48/Nidorino_shiny.png/revision/latest?cb=20220516201311",
            "https://static.wikia.nocookie.net/pokemongo/images/e/e1/Nidoking_shiny.png/revision/latest?cb=20220516201141",
            "https://static.wikia.nocookie.net/pokemongo/images/f/fd/Clefairy_shiny.png/revision/latest?cb=20220516181352",
            "https://static.wikia.nocookie.net/pokemongo/images/0/00/Clefable_shiny.png/revision/latest?cb=20220516181329",
            "https://static.wikia.nocookie.net/pokemongo/images/1/13/Vulpix_shiny.png/revision/latest?cb=20220516215102",
            "https://static.wikia.nocookie.net/pokemongo/images/7/74/Ninetales_shiny.png/revision/latest?cb=20220516201353",
            "https://static.wikia.nocookie.net/pokemongo/images/8/85/Jigglypuff_shiny.png/revision/latest?cb=20220516192345",
            "https://static.wikia.nocookie.net/pokemongo/images/e/e6/Wigglytuff_shiny.png/revision/latest?cb=20220516215622",
            "https://static.wikia.nocookie.net/pokemongo/images/7/78/Zubat_shiny.png/revision/latest?cb=20220404175022",
            "https://static.wikia.nocookie.net/pokemongo/images/a/ad/Golbat_shiny.png/revision/latest?cb=20220404175224",
            "https://static.wikia.nocookie.net/pokemongo/images/2/29/Oddish_shiny.png/revision/latest?cb=20220516201749",
            "https://static.wikia.nocookie.net/pokemongo/images/4/45/Gloom_shiny.png/revision/latest?cb=20220516190132",
            "https://static.wikia.nocookie.net/pokemongo/images/7/7a/Vileplume_shiny.png/revision/latest?cb=20220516214851",
            "https://static.wikia.nocookie.net/pokemongo/images/4/45/Paras_shiny.png/revision/latest?cb=20220516202244",
            "https://static.wikia.nocookie.net/pokemongo/images/6/64/Parasect_shiny.png/revision/latest?cb=20220516202315",
            "https://static.wikia.nocookie.net/pokemongo/images/f/fd/Venonat_shiny.png/revision/latest?cb=20220516214548",
            "https://static.wikia.nocookie.net/pokemongo/images/b/bb/Venomoth_shiny.png/revision/latest?cb=20220516214524",
            "https://static.wikia.nocookie.net/pokemongo/images/c/c7/Diglett_shiny.png/revision/latest?cb=20220516182812",
            "https://static.wikia.nocookie.net/pokemongo/images/b/b3/Dugtrio_shiny.png/revision/latest?cb=20220516183509",
            "https://static.wikia.nocookie.net/pokemongo/images/e/e7/Meowth_shiny.png/revision/latest?cb=20220516200015",
            "https://static.wikia.nocookie.net/pokemongo/images/5/57/Persian_shiny.png/revision/latest?cb=20220516202450",
            "https://static.wikia.nocookie.net/pokemongo/images/f/f8/Psyduck_shiny.png/revision/latest?cb=20220516203653",
            "https://static.wikia.nocookie.net/pokemongo/images/e/ed/Golduck_shiny.png/revision/latest?cb=20220516190302",
            "https://static.wikia.nocookie.net/pokemongo/images/d/da/Mankey_shiny.png/revision/latest?cb=20220516195410",
            "https://static.wikia.nocookie.net/pokemongo/images/2/26/Primeape_shiny.png/revision/latest?cb=20220516203553",
            "https://static.wikia.nocookie.net/pokemongo/images/6/64/Growlithe_shiny.png/revision/latest?cb=20220516190908",
            "https://static.wikia.nocookie.net/pokemongo/images/1/16/Arcanine_shiny.png/revision/latest?cb=20220516174245",
            "https://static.wikia.nocookie.net/pokemongo/images/f/f8/Poliwag_shiny.png/revision/latest?cb=20220516203214",
            "https://static.wikia.nocookie.net/pokemongo/images/6/65/Poliwhirl_shiny.png/revision/latest?cb=20220516203236",
            "https://static.wikia.nocookie.net/pokemongo/images/d/d7/Poliwrath_shiny.png/revision/latest?cb=20220516203307",
            "https://static.wikia.nocookie.net/pokemongo/images/7/7e/Abra_shiny.png/revision/latest?cb=20220516173600",
            "https://static.wikia.nocookie.net/pokemongo/images/5/56/Kadabra_shiny.png/revision/latest?cb=20220516192659",
            "https://static.wikia.nocookie.net/pokemongo/images/d/d7/Alakazam_shiny.png/revision/latest?cb=20220516173902",
            "https://static.wikia.nocookie.net/pokemongo/images/7/7c/Machop_shiny.png/revision/latest?cb=20220516194801",
            "https://static.wikia.nocookie.net/pokemongo/images/e/e2/Machoke_shiny.png/revision/latest?cb=20220516194737",
            "https://static.wikia.nocookie.net/pokemongo/images/9/98/Machamp_shiny.png/revision/latest?cb=20220516194707",
            "https://static.wikia.nocookie.net/pokemongo/images/9/93/Bellsprout_shiny.png/revision/latest?cb=20220516175308",
            "https://static.wikia.nocookie.net/pokemongo/images/6/6c/Weepinbell_shiny.png/revision/latest?cb=20220516215420",
            "https://static.wikia.nocookie.net/pokemongo/images/6/69/Victreebel_shiny.png/revision/latest?cb=20220516214800",
            "https://static.wikia.nocookie.net/pokemongo/images/5/5b/Tentacool_shiny.png/revision/latest?cb=20220516213136",
            "https://static.wikia.nocookie.net/pokemongo/images/2/25/Tentacruel_shiny.png/revision/latest?cb=20220516213200",
            "https://static.wikia.nocookie.net/pokemongo/images/a/ae/Geodude_shiny.png/revision/latest?cb=20220516185740",
            "https://static.wikia.nocookie.net/pokemongo/images/8/8a/Graveler_shiny.png/revision/latest?cb=20220516190648",
            "https://static.wikia.nocookie.net/pokemongo/images/b/b7/Golem_shiny.png/revision/latest?cb=20220516190333",
            "https://static.wikia.nocookie.net/pokemongo/images/e/ea/Ponyta_shiny.png/revision/latest?cb=20220516203338",
            "https://static.wikia.nocookie.net/pokemongo/images/e/ea/Rapidash_shiny.png/revision/latest?cb=20220516204153",
            "https://static.wikia.nocookie.net/pokemongo/images/3/36/Slowpoke_shiny.png/revision/latest?cb=20220516211057",
            "https://static.wikia.nocookie.net/pokemongo/images/a/a9/Slowbro_shiny.png/revision/latest?cb=20220516210954",
            "https://static.wikia.nocookie.net/pokemongo/images/f/fa/Magnemite_shiny.png/revision/latest?cb=20220516195047",
            "https://static.wikia.nocookie.net/pokemongo/images/c/c6/Magneton_shiny.png/revision/latest?cb=20220516195119",
            "https://static.wikia.nocookie.net/pokemongo/images/8/8f/Farfetch%27d_shiny.png/revision/latest?cb=20220516184610",
            "https://static.wikia.nocookie.net/pokemongo/images/2/2f/Doduo_shiny.png/revision/latest?cb=20220516182952",
            "https://static.wikia.nocookie.net/pokemongo/images/4/49/Dodrio_shiny.png/revision/latest?cb=20220516182920",
            "https://static.wikia.nocookie.net/pokemongo/images/0/0d/Seel_shiny.png/revision/latest?cb=20220516205818",
            "https://static.wikia.nocookie.net/pokemongo/images/a/a4/Dewgong_shiny.png/revision/latest?cb=20220516182645",
            "https://static.wikia.nocookie.net/pokemongo/images/0/0c/Grimer_shiny.png/revision/latest?cb=20220516190733",
            "https://static.wikia.nocookie.net/pokemongo/images/e/e0/Muk_shiny.png/revision/latest?cb=20220516200917",
            "https://static.wikia.nocookie.net/pokemongo/images/d/df/Shellder_shiny.png/revision/latest?cb=20220516210130",
            "https://static.wikia.nocookie.net/pokemongo/images/4/45/Cloyster_shiny.png/revision/latest?cb=20220516181446",
            "https://static.wikia.nocookie.net/pokemongo/images/3/36/Gastly_shiny.png/revision/latest?cb=20220516185637",
            "https://static.wikia.nocookie.net/pokemongo/images/b/b8/Haunter_shiny.png/revision/latest?cb=20220516191222",
            "https://static.wikia.nocookie.net/pokemongo/images/1/15/Gengar_shiny.png/revision/latest?cb=20220516185708",
            "https://static.wikia.nocookie.net/pokemongo/images/b/ba/Onix_shiny.png/revision/latest?cb=20220516201906",
            "https://static.wikia.nocookie.net/pokemongo/images/2/25/Drowzee_shiny.png/revision/latest?cb=20220516183358",
            "https://static.wikia.nocookie.net/pokemongo/images/2/20/Hypno_shiny.png/revision/latest?cb=20220516192055",
            "https://static.wikia.nocookie.net/pokemongo/images/8/82/Krabby_shiny.png/revision/latest?cb=20220516193154",
            "https://static.wikia.nocookie.net/pokemongo/images/3/3d/Kingler_shiny.png/revision/latest?cb=20220516192921",
            "https://static.wikia.nocookie.net/pokemongo/images/b/be/Voltorb_shiny.png/revision/latest?cb=20220516215012",
            "https://static.wikia.nocookie.net/pokemongo/images/9/9f/Electrode_shiny.png/revision/latest?cb=20220516184111",
            "https://static.wikia.nocookie.net/pokemongo/images/2/2e/Exeggcute_shiny.png/revision/latest?cb=20220516184450",
            "https://static.wikia.nocookie.net/pokemongo/images/2/29/Exeggutor_shiny.png/revision/latest?cb=20180530020300",
            "https://static.wikia.nocookie.net/pokemongo/images/a/a7/Cubone_shiny.png/revision/latest?cb=20220516182241",
            "https://static.wikia.nocookie.net/pokemongo/images/a/a4/Marowak_shiny.png/revision/latest?cb=20220516195614",
            "https://static.wikia.nocookie.net/pokemongo/images/6/62/Hitmonlee_shiny.png/revision/latest?cb=20220516191640",
            "https://static.wikia.nocookie.net/pokemongo/images/7/7b/Hitmonchan_shiny.png/revision/latest?cb=20220516191616",
            "https://static.wikia.nocookie.net/pokemongo/images/c/c9/Lickitung_shiny.png/revision/latest?cb=20220516193855",
            "https://static.wikia.nocookie.net/pokemongo/images/5/5c/Koffing_shiny.png/revision/latest?cb=20220516193118",
            "https://static.wikia.nocookie.net/pokemongo/images/8/8b/Weezing_shiny.png/revision/latest?cb=20220516215441",
            "https://static.wikia.nocookie.net/pokemongo/images/5/5d/Rhyhorn_shiny.png/revision/latest?cb=20220516204802",
            "https://static.wikia.nocookie.net/pokemongo/images/a/a9/Rhydon_shiny.png/revision/latest?cb=20220516204720",
            "https://static.wikia.nocookie.net/pokemongo/images/2/21/Chansey_shiny.png/revision/latest?cb=20220516180638",
            "https://static.wikia.nocookie.net/pokemongo/images/5/5c/Tangela_shiny.png/revision/latest?cb=20220516212939",
            "https://static.wikia.nocookie.net/pokemongo/images/7/70/Kangaskhan_shiny.png/revision/latest?cb=20220516192751",
            "https://static.wikia.nocookie.net/pokemongo/images/c/ca/Horsea_shiny.png/revision/latest?cb=20220516191847",
            "https://static.wikia.nocookie.net/pokemongo/images/d/d6/Seadra_shiny.png/revision/latest?cb=20220516205643",
            "https://static.wikia.nocookie.net/pokemongo/images/5/53/Goldeen_shiny.png/revision/latest?cb=20220516190239",
            "https://static.wikia.nocookie.net/pokemongo/images/6/6a/Seaking_shiny.png/revision/latest?cb=20220516205716",
            "https://static.wikia.nocookie.net/pokemongo/images/8/88/Staryu_shiny.png/revision/latest?cb=20220516212054",
            "https://static.wikia.nocookie.net/pokemongo/images/9/9d/Starmie_shiny.png/revision/latest?cb=20220516212032",
            "https://static.wikia.nocookie.net/pokemongo/images/7/7d/Mr._Mime_shiny.png/revision/latest?cb=20220516200809",
            "https://static.wikia.nocookie.net/pokemongo/images/2/2f/Scyther_shiny.png/revision/latest?cb=20220516205620",
            "https://static.wikia.nocookie.net/pokemongo/images/2/24/Jynx_shiny.png/revision/latest?cb=20220516192541",
            "https://static.wikia.nocookie.net/pokemongo/images/1/12/Electabuzz_shiny.png/revision/latest?cb=20220516183958",
            "https://static.wikia.nocookie.net/pokemongo/images/7/7b/Magmar_shiny.png/revision/latest?cb=20220516194956",
            "https://static.wikia.nocookie.net/pokemongo/images/c/ce/Pinsir_shiny.png/revision/latest?cb=20220516203017",
            "https://static.wikia.nocookie.net/pokemongo/images/c/c9/Tauros_shiny.png/revision/latest?cb=20220516213050",
            "https://static.wikia.nocookie.net/pokemongo/images/d/df/Magikarp_shiny.png/revision/latest?cb=20220516194925",
            "https://static.wikia.nocookie.net/pokemongo/images/3/3f/Gyarados_shiny.png/revision/latest?cb=20220516191103",
            "https://static.wikia.nocookie.net/pokemongo/images/4/4d/Lapras_shiny.png/revision/latest?cb=20220516193445",
            "https://static.wikia.nocookie.net/pokemongo/images/f/fa/Ditto_shiny.png/revision/latest?cb=20220516182836",
            "https://static.wikia.nocookie.net/pokemongo/images/0/0b/Eevee_shiny.png/revision/latest?cb=20220516183901",
            "https://static.wikia.nocookie.net/pokemongo/images/e/e5/Vaporeon_shiny.png/revision/latest?cb=20220516214437",
            "https://static.wikia.nocookie.net/pokemongo/images/3/35/Jolteon_shiny.png/revision/latest?cb=20220516192431",
            "https://static.wikia.nocookie.net/pokemongo/images/0/0d/Flareon_shiny.png/revision/latest?cb=20220516184929",
            "https://static.wikia.nocookie.net/pokemongo/images/f/f7/Porygon_shiny.png/revision/latest?cb=20220516203436",
            "https://static.wikia.nocookie.net/pokemongo/images/2/22/Omanyte_shiny.png/revision/latest?cb=20220516201823",
            "https://static.wikia.nocookie.net/pokemongo/images/8/8f/Omastar_shiny.png/revision/latest?cb=20220516201846",
            "https://static.wikia.nocookie.net/pokemongo/images/e/e1/Kabuto_shiny.png/revision/latest?cb=20220516192605",
            "https://static.wikia.nocookie.net/pokemongo/images/b/b9/Kabutops_shiny.png/revision/latest?cb=20220516192627",
            "https://static.wikia.nocookie.net/pokemongo/images/c/cc/Aerodactyl_shiny.png/revision/latest?cb=20220516173721",
            "https://static.wikia.nocookie.net/pokemongo/images/8/83/Snorlax_shiny.png/revision/latest?cb=20220516211337",
            "https://static.wikia.nocookie.net/pokemongo/images/b/b5/Articuno_shiny.png/revision/latest?cb=20220516174546",
            "https://static.wikia.nocookie.net/pokemongo/images/2/2c/Zapdos_shiny.png/revision/latest?cb=20220516173249",
            "https://static.wikia.nocookie.net/pokemongo/images/e/e2/Moltres_shiny.png/revision/latest?cb=20220516163711",
            "https://static.wikia.nocookie.net/pokemongo/images/1/18/Dratini_shiny.png/revision/latest?cb=20220516183228",
            "https://static.wikia.nocookie.net/pokemongo/images/d/df/Dragonair_shiny.png/revision/latest?cb=20220516183115",
            "https://static.wikia.nocookie.net/pokemongo/images/c/c4/Dragonite_shiny.png/revision/latest?cb=20220516183138",
            "https://static.wikia.nocookie.net/pokemongo/images/3/3d/Mewtwo_shiny.png/revision/latest?cb=20220516200249",
            "https://static.wikia.nocookie.net/pokemongo/images/5/5b/Mew_shiny.png/revision/latest?cb=20220516200216" };

}