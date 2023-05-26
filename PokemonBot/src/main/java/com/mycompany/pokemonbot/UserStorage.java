package com.mycompany.pokemonbot;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @group The People Project
 * @author @BrandonDeB
 * @date 3/19/2023
 */
@XmlRootElement(name = "user")
@XmlAccessorType(XmlAccessType.FIELD)
public class UserStorage {
    int[] candyStorage = new int[152];
    ArrayList<Pokemon> pokemonList = new ArrayList<Pokemon>();
    ArrayList<Pokemon> battleList = new ArrayList<Pokemon>();
    Pokemon trade = null;
    int[] pokeballs = new int[5];
    long userID;
    String nickname;

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public UserStorage() {
        userID = (long) 0;
    }

    public UserStorage(long id) {
        userID = id;
        pokeballs[0] = 5;
        pokeballs[1] = 2;
        pokeballs[2] = 1;
        pokeballs[3] = 1;
        pokeballs[4] = 0;
    }

    public void setTrade(Pokemon p) {
        trade = p;
    }

    public boolean isTrade() {
        if (trade == null)
            return true;
        return false;
    }

    // returns the whole array list of every single pokemon held by the user
    public ArrayList<Pokemon> getPokemon() {
        return pokemonList;
    }

    // returns the amount of every type of Pokeball the user has
    public int[] getPokeballs() {
        return pokeballs;
    }

    // returns the amount of candy for every pokemon
    public int[] getCandy() {
        return candyStorage;
    }

    // Can take a positive or negative number of candy to set for the user
    public void changeCandy(int pokedexNum, int amount) {
        candyStorage[pokedexNum] += amount;
    }

    // adds a pokemon to the user's storage
    public void addPokemon(Pokemon pikachu) {
        pokemonList.add(pikachu);
    }

    // send an equivalent pokemon to the method and it will remove it from the array
    // list
    public void releasePokemon(Pokemon pikachu) {
        pokemonList.remove(pikachu);
    }

    // adds amount of pokeballs to the type. Could also subtract pokeballs using
    // this
    public void changePokeballs(int pokeballType, int amount) {
        pokeballs[pokeballType] += amount;
    }

    public void setUserID(long id) {
        userID = id;
    }

    public long getUserID() {
        return userID;
    }

    public String getNickname() {
        return this.nickname;
    }

    public boolean updatePokemon(ArrayList<Pokemon> ar) {
        pokemonList = ar;
        return true;
    }

    public void printPokemon() {
        for (Pokemon p : pokemonList) {
            System.out.println(p.getSpecies());
        }
    }

    public Pokemon findPokemon(String species, int cp, String typeOne, String typeTwo, int level) {

        for (Pokemon p : pokemonList) {

            if (p.getSpecies().equals(species)) {
//                System.out.println("Species was matched!");
                if (p.getCP() == cp && p.getLevel() == level) {
//                    System.out.println("CP and LVL was matched!");
                    if (p.getTypeOne().equals(typeOne) && p.getTypeTwo().equals(typeTwo)) {
//                        System.out.println("MATCH FOUND!");
                        return p;
                    }
                }

            }

        }
        return null;
    }

    public String getIdAsString() {
        return Long.toString(userID);
    }
}