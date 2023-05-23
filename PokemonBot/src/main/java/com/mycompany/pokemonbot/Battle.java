package com.mycompany.pokemonbot;

import java.util.ArrayList;
import java.util.List;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.SelectMenu;
import org.javacord.api.entity.message.component.SelectMenuOption;
import org.javacord.api.entity.message.embed.EmbedBuilder;

public class Battle {

    /*
     *
     * brings up the menue that allows two people to battle one antother
     * only the desired users are able to select their pokemon to prevent other
     * users from interfering with battles
     * trainers can also select 3 different pokemon to bring into battle, no more
     * and no less.
     * This also resets the hp of the pokemon that battled back to full after the
     * battle has ended, then clears the players battlelist
     */
    public static void battleTime(UserStorage adamStorage, UserStorage steveStorage, TextChannel ch) {
        battlePokemon(adamStorage.battleList, steveStorage.battleList, ch);

        for (int i = 0; i < 3; i++) {
            adamStorage.battleList.get(i).setHP(adamStorage.battleList.get(i).getHP());
            steveStorage.battleList.get(i).setHP(steveStorage.battleList.get(i).getHP());
        }
        adamStorage.battleList.clear();
        steveStorage.battleList.clear();

    }

    // makes the battle pokemon lists, and has the logic for actually sequencing
    // through the pokemon in each players battle list, allowing each players 3
    // pokemon to automatically fight each other in a psedo-turn based combat.
    public static void battlePokemon(ArrayList<Pokemon> aP, ArrayList<Pokemon> sP, TextChannel channel) {
        Pokemon p1 = aP.get(0);
        Pokemon p2 = aP.get(1);
        Pokemon p3 = aP.get(2);
        Pokemon p4 = sP.get(0);
        Pokemon p5 = sP.get(1);
        Pokemon p6 = sP.get(2);

        int p1Index = 0; // Index of the current Pokemon for Player 1
        int p2Index = 0; // Index of the current Pokemon for Player 2
        Pokemon currentP1 = p1; // Get the first Pokemon for Player 1
        Pokemon currentP2 = p4; // Get the first Pokemon for Player 2

        while (currentP1.isAlive() && currentP2.isAlive()) {
            Pokemon winner = fight(currentP1, currentP2, channel);
            if (winner == currentP1) {
                // Player 1's Pokemon won the fight
                channel.sendMessage(currentP1.getSpecies() + " won the fight against "+currentP2.getSpecies()+"!");
                currentP1.toMessage();
                p2Index++;
                if (p2Index == 3) {
                    // All of Player 2's Pokemon have been defeated
                    break;
                }
                switch (p2Index) {
                    case 1:
                        currentP2 = p5; // Switch to the second Pokemon for Player 2
                        break;
                    case 2:
                        currentP2 = p6; // Switch to the third Pokemon for Player 2
                        break;
                    default:
                        break;
                }
            } else {
                channel.sendMessage(currentP2.getSpecies()  + " won the fight against "+currentP1.getSpecies()+"!");
                currentP2.toMessage();
                // Player 2's Pokemon won the fight
                p1Index++;
                if (p1Index == 3) {
                    // All of Player 1's Pokemon have been defeated
                    break;
                }
                switch (p1Index) {
                    case 1:
                        currentP1 = p2; // Switch to the second Pokemon for Player 1
                        break;
                    case 2:
                        currentP1 = p3; // Switch to the third Pokemon for Player 1
                        break;
                    default:
                        break;
                }
            }
        }

        // Determine the winner of the battle
        if (currentP1.isAlive()) {
            // Player 1 won, and outputs a embed of the pokemon of the winner and its
            // current hp

            new MessageBuilder()

                    .addEmbed(new EmbedBuilder()
                            .setAuthor("Player 1 is the Supreme Victor!!!!!")
                            .setTitle(currentP1.getSpecies())
                            .setDescription("HP : " + currentP1.curHP())
                            .setImage(currentP1.getSpeciesPicture()))
                    .send(channel);
        } else {
            // Player 2 won, and outputs a embed of the pokemon of the winner and its
            // current hp

            new MessageBuilder()

                    .addEmbed(new EmbedBuilder()
                            .setAuthor("Player 2 is the Supreme Victor!!!!!")
                            .setTitle(currentP2.getSpecies())
                            .setDescription("HP : " + currentP2.curHP())
                            .setImage(currentP2.getSpeciesPicture()))
                    .send(channel);
        }

        // Code for the menu that the users are presented with to select three pokemon
        // that they will be fighting with, pulling from userStorage to get these
        // pokemon.
    }

    public static void choosePokemon(UserStorage adamStorage, UserStorage steveStorage, TextChannel channel) {

        List<SelectMenuOption> optionsAdam = new ArrayList<SelectMenuOption>();
        int count = 0;
        for (Pokemon p : adamStorage.getPokemon()) {
            System.out.println(p.getSpecies());
            optionsAdam.add(SelectMenuOption.create(p.getSpecies(),
                    String.valueOf(adamStorage.getUserID()) + ":" + String.valueOf(count),
                    Integer.toString(p.getCP())));
            count++;
        }
        List<SelectMenuOption> optionsSteve = new ArrayList<SelectMenuOption>();
        count = 0;
        for (Pokemon p : steveStorage.getPokemon()) {
            System.out.println(p.getSpecies());
            optionsSteve.add(SelectMenuOption.create(p.getSpecies(),
                    String.valueOf(steveStorage.getUserID()) + ":" + String.valueOf(count),
                    Integer.toString(p.getCP())));
            count++;
        }

        MessageBuilder messageBuilder = new MessageBuilder()
                .setContent("Pick 3 Pokemon to bring to battle, " + "<@" + adamStorage.getUserID() + "> and <@"
                        + steveStorage.getUserID() + ">!")
                .addComponents(ActionRow.of(
                        SelectMenu.createStringMenu("BattlepokemonAdam", "Click here to select a pokemon", optionsAdam,
                                false)));
        messageBuilder.addComponents(ActionRow.of(
                SelectMenu.createStringMenu("BattlepokemonSteve", "Click here to select a pokemon", optionsSteve,
                        false)))
                .send(channel);

        // code for actually making each players team fight, having each pokemon attack
        // one another until one player has no pokemon with hp >= 0 remaining.
    }

    private static Pokemon fight(Pokemon player1Current, Pokemon player2Current, TextChannel channel) {
        while (player1Current.curHP() > 0 && player2Current.curHP() > 0) {
            int damageToPlayer1 = (int) Math.ceil(player2Current.getCP() * 0.025);
            int damageToPlayer2 = (int) Math.ceil(player1Current.getCP() * 0.025);
            player2Current.setHP(player2Current.curHP() - damageToPlayer2);
            if (player2Current.curHP() <= 0) {
                return player1Current;
            }
            player1Current.setHP(player1Current.curHP() - damageToPlayer1);
            if (player1Current.curHP() <= 0) {
                return player2Current;
            }
            //channel.sendMessage(player1Current.getSpecies() + " HP: " + player1Current.curHP());
            //channel.sendMessage(player2Current.getSpecies() + " HP: " + player2Current.curHP());
        }
        if (player2Current.curHP() <= 0) {
            return player1Current;
        } else {
            return player2Current;
        }
    }

}