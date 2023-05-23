package com.mycompany.pokemonbot;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.w3c.dom.Document;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @group The People Project
 * @author @BrandonDeB, @YamiKir
 * @date 4/11/2023
 * 
 */
@XmlRootElement(name = "pokemon")
@XmlAccessorType(XmlAccessType.FIELD)
public class Pokemon {
    @XmlTransient
    private static File file = new File("PokemonBot\\pokemonXML.xml");
    @XmlTransient
    private DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    @XmlTransient
    private DocumentBuilder db = dbf.newDocumentBuilder();
    @XmlTransient
    private Document document = db.parse(file); // parsed XML file containing all the Pokemon data
    @XmlTransient
    private static boolean normalized = false;
    @XmlTransient
    private static NodeList allPokemon; // list containing all the data from all pokemon
    @XmlTransient
    private static Random random = new Random();
    @XmlTransient
    private static float[] combatPowerMod = new float[102];

    // unique attributes for each pokemon
    private int idNumber; // pokedex number
    private int hp;
    private String species;
    private int[] baseStats = new int[3]; // Stats in the order: Attack, Defense, Stamina
    private int[] scaledStats = new int[4]; // Stats in the order: Attack, Defense, Stamina, Combat Power
    private int[] ivs = new int[3];
    private String type1;
    private String type2;
    private int level; // level of the pokemon. Actually have half values so it's 2x the displayed
                       // amount
    private String fastAttack;
    private String chargedAttack;
    private int evolutionCost;

    // Generates a pokemon of a random species and level
    // Used for generating wild pokemon users can catch/claim
    Pokemon() throws SAXException, IOException, ParserConfigurationException {
        // only normalizes the XML first time a pokemon is generated upon initilization
        if (normalized == false) {
            document.getDocumentElement().normalize();
            allPokemon = document.getElementsByTagName("species");
            normalized = true;
            writeCPMs();
        }
        ivs[0] = random.nextInt(16);
        ivs[1] = random.nextInt(16);
        ivs[2] = random.nextInt(16);
        generate(random.nextInt(149) + 1, (random.nextInt(102) + 1));
    }

    // Generates a pokemon of specified type and level
    // Primary example is the generation of starters in Storage.java
    Pokemon(int idNumber, int level) throws SAXException, IOException, ParserConfigurationException {
        if (normalized == false) {
            document.getDocumentElement().normalize();
            allPokemon = document.getElementsByTagName("species");
            normalized = true;
            writeCPMs();
        }
        ivs[0] = random.nextInt(16);
        ivs[1] = random.nextInt(16);
        ivs[2] = random.nextInt(16);
        generate(idNumber, level);
    }

    /**
     * @param idNumber
     *                 takes the pokedex number of the pokemon to generate
     * @param level
     *                 takes the level to generate the pokemon at
     */
    private void generate(int idNumber, int level) {
        System.out.println("generating random");
        this.idNumber = idNumber;
        this.level = level;
        Node nNode = allPokemon.item(idNumber);
        Element eElement = (Element) nNode;
        species = eElement.getElementsByTagName("name").item(0).getTextContent().trim();
        // Takes the base stats found in the standard pokemon games
        int HP = Integer
                .parseInt(eElement.getElementsByTagName("hp").item(0).getTextContent().replaceAll("[^0-9]", ""));
        int ATK = Integer
                .parseInt(eElement.getElementsByTagName("hp").item(0).getTextContent().replaceAll("[^0-9]", ""));
        int DEF = Integer
                .parseInt(eElement.getElementsByTagName("def").item(0).getTextContent().replaceAll("[^0-9]", ""));
        int SPA = Integer
                .parseInt(eElement.getElementsByTagName("spa").item(0).getTextContent().replaceAll("[^0-9]", ""));
        int SPD = Integer
                .parseInt(eElement.getElementsByTagName("spd").item(0).getTextContent().replaceAll("[^0-9]", ""));
        int SPE = Integer
                .parseInt(eElement.getElementsByTagName("spe").item(0).getTextContent().replaceAll("[^0-9]", ""));
        this.hp = HP;
        // scaledAttack and Defense take the base stats and modify them to the Pokemon
        // GO form of stats
        int scaledAttack = (int) Math.round(2 * ((7.0 / 8.0) * Math.max(ATK, SPA) + (1.0 / 8.0) * Math.min(ATK, SPA)));
        double speedMod = 1 + ((SPE - 75) / 500.0);
        int scaledDefense = (int) Math.round(2 * ((5.0 / 8) * Math.max(SPD, DEF) + (3.0 / 8) * Math.min(SPD, DEF)));
        baseStats[0] = (int) Math.round(scaledAttack * speedMod) + ivs[0];// + random.nextInt(16);
        baseStats[1] = (int) Math.round(scaledDefense * speedMod) + ivs[1];// + random.nextInt(16);
        baseStats[2] = (int) Math.floor(HP * 1.75 + 50) + ivs[2];// + random.nextInt(16);
        // (int) Math.floor(Math.max(10, (baseStats[0]*Math.pow(baseStats[1],
        // 0.5)*Math.pow(baseStats[2], 0.5)*Math.pow(combatPowerMod[level-1],2))/10));
        scaleStats();
        generateMove(eElement);
        if (eElement.getElementsByTagName("evolution").getLength() != 0) {
            evolutionCost = Integer.parseInt(
                    eElement.getElementsByTagName("evolution").item(0).getTextContent().replaceAll("[^0-9]", ""));
        } else {
            evolutionCost = -1;
        }
        System.out.println("Evolution cost = " + evolutionCost);
        type1 = eElement.getElementsByTagName("typeone").item(0).getTextContent().trim();
        try {
            type2 = eElement.getElementsByTagName("typetwo").item(0).getTextContent().trim();
        } catch (NullPointerException ex) {
            System.out.println("The pokemon is monotype");
        }

    }

    // There is a specific value that combat power gets modified by for each pokemon
    // level. This just reads
    // the list of set combat power modifiers and stores them in a static array
    private void writeCPMs() {
        try {
            File CPMs = new File("PokemonBot/CPM values.txt");
            Scanner reader = new Scanner(CPMs);
            int count = 0;
            while (reader.hasNextLine()) {
                combatPowerMod[count] = Float.parseFloat(reader.nextLine());
                count++;
            }
            reader.close();
        } catch (FileNotFoundException ex) {
            System.out.println("The file CPM values was not found");
            ex.printStackTrace();
        }
    }

    // scales the base stats of a pokemon to make them match pokemon GO's stat
    // system
    private void scaleStats() {
        scaledStats[0] = (int) ((int) baseStats[0] * combatPowerMod[level - 1]);
        scaledStats[1] = (int) ((int) baseStats[1] * combatPowerMod[level - 1]);
        scaledStats[2] = (int) ((int) baseStats[2] * combatPowerMod[level - 1]);
        scaledStats[3] = (int) Math.floor(Math.max(10, (baseStats[0] * Math.pow(baseStats[1], 0.5)
                * Math.pow(baseStats[2], 0.5) * Math.pow(combatPowerMod[level - 1], 2)) / 10));
    }

    // reads all potential attacks of a pokemon and generates 2
    // One attack will be a 'fast attack' while the other is a 'charged attack'
    private void generateMove(Element pokemon) {
        NodeList allAttacks = pokemon.getElementsByTagName("attack");
        ArrayList<Node> fastMoves = new ArrayList<Node>();
        ArrayList<Node> chargeMoves = new ArrayList<Node>();
        for (int i = 0; i < allAttacks.getLength(); i++) {
            if (allAttacks.item(i).getTextContent().trim().equals("Fast attack")) {
                fastMoves.add(allAttacks.item(i));
            } else if (allAttacks.item(i).getTextContent().trim().equals("Charged attack")) {
                chargeMoves.add(allAttacks.item(i));
            }
        }

        fastAttack = fastMoves.get(random.nextInt(fastMoves.size())).getAttributes().getNamedItem("id").getNodeValue()
                .trim();
        chargedAttack = chargeMoves.get(random.nextInt(chargeMoves.size())).getAttributes().getNamedItem("id")
                .getNodeValue().trim();

    }

    public String getFast(Pokemon p) {
        return p.fastAttack;
    }

    public String getCharged(Pokemon p) {
        return p.chargedAttack;
    }

    public String getSpecies() {
        return species;
    }

    public int getCP() {
        return scaledStats[3];
    }

    public int getID() {
        return idNumber;
    }

    public int getBaseForm() {
        Boolean done = false;
        int currentPosition = idNumber - 1;
        while (!done) {
            Node currentPokemon = allPokemon.item(currentPosition);
            Element pokemonElement = (Element) currentPokemon;
            System.out.println("currentPosition = " + currentPosition);
            if (pokemonElement.getElementsByTagName("evolution").getLength() != 0 && currentPosition != 0) {
                currentPosition--;
            } else {
                done = true;
            }
        }
        return currentPosition;
    }

    public int getEvolutionCost() {
        return evolutionCost;
    }

    // Generates a basic string displaying some Pokemon attributes to help with
    // testing
    public String toString() {
        return idNumber + "\n" + species + "\nLevel: " + (level / 2.0 + 0.5) + "\nType: " + type1 + "/" + type2
                + "\nCombat Power: " + scaledStats[3] + "\nMoves: " + fastAttack + " and " + chargedAttack;
    }

    // returns an Embed that contains basic pokemon information for message building
    public EmbedBuilder toMessage() {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(getSpecies())
                .setDescription("CP: " + Integer.toString(getCP()))
                .setThumbnail(
                        "https://www.popsockets.com/dw/image/v2/BFSM_PRD/on/demandware.static/-/Sites-popsockets-master-catalog/default/dw7fe2333f/images/hi-res/Enamel_Pokeball_01_Top-View.png?sw=800&sh=800")
                .addInlineField("Type One", getTypeOne())
                .addInlineField("Type Two", getTypeTwo())
                .addInlineField("Level", getLevelAsString())
                .setImage(getSpeciesPicture());
        return embed;
    }

    // returns url for pokemon's picture as a string
    public String getSpeciesPicture() {
        Node nNode = allPokemon.item(idNumber);
        Element eElement = (Element) nNode;
        return eElement.getElementsByTagName("picture").item(0).getTextContent().trim();
    }

    public String getTypeOne() {
        return type1;
    }

    public String getTypeTwo() {
        if (type2 != null) {
            return type2;
        } else {
            return "";
        }
    }

    public void levelUp() {
        level++;
        scaleStats();
    }

    public void evolve() {
        int eeveeBonus = 0;
        if (species.equals("Eevee")) {
            eeveeBonus += random.nextInt(3);
        }
        generate(idNumber + 1 + eeveeBonus, level);
    }

    public int getLevel() {
        return level;
    }

    public String getLevelAsString() {
        return ((Integer) level).toString();
    }

    public void setHP(int hp) {
        this.hp = hp;
    }

    public int getHP() {
        return baseStats[2];
    }

    public int curHP() {
        return this.hp;
    }

    public boolean isAlive() {
        return this.hp >= 1;
    }
}