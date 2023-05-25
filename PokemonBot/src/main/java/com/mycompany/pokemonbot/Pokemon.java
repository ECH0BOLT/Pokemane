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
    private boolean shiny = false;
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

    Pokemon(int idNumber) throws SAXException, IOException, ParserConfigurationException {
        if (normalized == false) {
            document.getDocumentElement().normalize();
            allPokemon = document.getElementsByTagName("species");
            normalized = true;
            writeCPMs();
        }
        ivs[0] = random.nextInt(16);
        ivs[1] = random.nextInt(16);
        ivs[2] = random.nextInt(16);
        double rand = Math.random();
        if(rand < .5) this.shiny = true;
        generate(idNumber, (random.nextInt(102) + 1));
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
        double rand = Math.random();
        if(rand < .04) this.shiny = true;
        System.out.println("generating random");
        this.idNumber = idNumber;
        this.level = level;
        Node nNode = allPokemon.item(idNumber - 1);
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
        if (idNumber < 4) return 1;
        //eevee handling
        if (idNumber > 132 && idNumber < 137) return 133;

        int currentPosition = idNumber;
        Node currentPokemon = allPokemon.item(currentPosition - 1);
        Node prevPokemon = allPokemon.item(currentPosition - 2);
        Node prev2Pokemon = allPokemon.item(currentPosition - 3);
        Element pokemonElement = (Element) currentPokemon;
        Element prevElement = (Element) prevPokemon;
        Element prev2Element = (Element) prev2Pokemon;
        System.out.println("currentPosition = " + (currentPosition));
        if (pokemonElement.getElementsByTagName("evolution").getLength() == 0 && prevElement.getElementsByTagName("evolution").getLength() != 0 && prev2Element.getElementsByTagName("evolution").getLength() != 0) {
            return currentPosition - 2;
        } else if (pokemonElement.getElementsByTagName("evolution").getLength() == 0 && prevElement.getElementsByTagName("evolution").getLength() != 0 && prev2Element.getElementsByTagName("evolution").getLength() == 0) {
            return currentPosition - 1;
        } else if (pokemonElement.getElementsByTagName("evolution").getLength() == 0 && prevElement.getElementsByTagName("evolution").getLength() == 0) {
            return currentPosition;
        } else if (pokemonElement.getElementsByTagName("evolution").getLength() != 0 && prevElement.getElementsByTagName("evolution").getLength() == 0) {
            return currentPosition;
        } else if (pokemonElement.getElementsByTagName("evolution").getLength() != 0 && prevElement.getElementsByTagName("evolution").getLength() != 0) {
            return currentPosition - 1;
        } else return currentPosition;
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
        if(this.shiny){
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("✧" + getSpecies() + "✧")
                    .setDescription("CP: " + Integer.toString(getCP()))
                    .setThumbnail(
                            "https://www.popsockets.com/dw/image/v2/BFSM_PRD/on/demandware.static/-/Sites-popsockets-master-catalog/default/dw7fe2333f/images/hi-res/Enamel_Pokeball_01_Top-View.png?sw=800&sh=800")
                    .addInlineField("Type One", getTypeOne())
                    .addInlineField("Type Two", getTypeTwo())
                    .addInlineField("Level", getLevelAsString())
                    .setImage(getSpeciesPicture());
            return embed;
        }
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
    public String[] shinySprites = {
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
    // returns url for pokemon's picture as a string
    public String getSpeciesPicture() {
        if(this.shiny) return shinySprites[this.getID() - 1];
        Node nNode = allPokemon.item(idNumber - 1);
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
    public boolean isShiny() {
        return this.shiny;
    }

}