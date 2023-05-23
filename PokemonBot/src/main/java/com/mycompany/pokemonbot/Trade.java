package com.mycompany.pokemonbot;

import java.util.ArrayList;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.Button;
import org.javacord.api.entity.message.component.SelectMenu;
import org.javacord.api.entity.message.component.SelectMenuOption;

public class Trade {

    // TO DO: clean up extra old code
    public Trade() {
    }

    // Method to create buttons that allow a user to select any pokemon from either
    // user's storage
    public static void createPokemonButtons(UserStorage adamStorage, UserStorage steveStorage, TextChannel channel) {
        ArrayList<SelectMenuOption> options1 = new ArrayList<>();
        ArrayList<SelectMenuOption> options2 = new ArrayList<>();
        int count = 1;
        for (Pokemon p : adamStorage.getPokemon()) {
            options1.add(SelectMenuOption.create(count + ": " + p.getSpecies(),
                    String.valueOf(adamStorage.getUserID()) + ":" + String.valueOf(count),
                    String.valueOf(adamStorage.getUserID())));
            count++;
        }
        count = 1;
        for (Pokemon p : steveStorage.getPokemon()) {
            options2.add(SelectMenuOption.create(count + ": " + p.getSpecies(),
                    String.valueOf(steveStorage.getUserID()) + ":" + String.valueOf(count),
                    String.valueOf(steveStorage.getUserID())));
            count++;
        }

        MessageBuilder messageBuilder = new MessageBuilder()
                .setContent("<@" + adamStorage.getUserID() + "> is trying to trade with you, <@"
                        + steveStorage.getUserID() + ">")
                .addComponents(ActionRow.of(
                        SelectMenu.createStringMenu("TradepokemonAdam",
                                "Click here to select " + adamStorage.getNickname() + "'s Pokemon.", options1, false)));
        messageBuilder
                .addComponents(ActionRow.of(
                        SelectMenu.createStringMenu("TradepokemonSteve",
                                "Click here to select " + steveStorage.getNickname() + "'s Pokemon.", options2,
                                false)));
        messageBuilder
                .addComponents(ActionRow.of(
                        Button.danger("Cancel Trade", "Cancel Trade", false)));
        messageBuilder
                .addComponents(ActionRow.of(
                        Button.success("Send Trade", "Send Trade", false)));
        messageBuilder.send(channel);
    }

    // Method to actually swap the Pokemon's owners
    public static void trade(UserStorage adam, UserStorage steve, TextChannel ch) {

        adam.addPokemon(steve.trade);
        steve.addPokemon(adam.trade);
        adam.releasePokemon(adam.trade);
        steve.releasePokemon(steve.trade);
        steve.setTrade(null);
        adam.setTrade(null);

    }

}