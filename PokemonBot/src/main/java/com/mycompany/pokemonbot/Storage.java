package com.mycompany.pokemonbot;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;
import org.apache.commons.io.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.transform.stream.StreamSource;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.user.User;

//import sun.security.util.Length;

/**
 * @group The People Project
 * @author @BrandonDeB, @YamiKir
 * @date 4/11/2023
 * 
 */
@XmlRootElement(name = "storage")
@XmlAccessorType(XmlAccessType.FIELD)
public class Storage {
  // map stores all the data with userIDs as key and an arrayList of their pokemon
  // as the data
  public UserStorageMap userData;
  private Long serverID;

  Storage() {
    serverID = (long) 0;
    userData = null;
  }
  Storage(Long serverID) {
    this.serverID = serverID;
    userData = new UserStorageMap();
  }

  public Long getServerID() {
    return serverID;
  }

  public UserStorageMap getStorageMap(UserStorage adam) {
    return this.userData;
  }

  /**
   * @param adam
   *             Receives a user to check if they exist in server's data
   * @return boolean
   *         returns true if user exists else false
   */
  boolean isUser(User adam) {
    return userData.getUserStorageMap().containsKey(adam.getId());
  }

  /**
   * @param adamID
   *               Uses the long of the user ID to check instead of user object
   * @return boolean
   *         returns true if user exists in server else false
   */
  boolean isUser(Long adamID) {
    return userData.getUserStorageMap().containsKey(adamID);
  }

  /**
   * @param adam
   *                  gets the user who tried to claim a pokemon
   * @param generated
   *                  takes the most recent generated pokemon
   * @param ch
   *                  takes the channel that the pokemon and claim occur in
   */
  void claim(User adam, Pokemon generated, TextChannel ch) {
    // adds generated to the arrayList the user has
    UserStorage temp = userData.getUserStorageMap().get(adam.getId());
    temp.addPokemon(generated);
    temp.changeCandy(generated.getBaseForm(), 3);
    userData.getUserStorageMap().put(adam.getId(), temp);
    System.out.println("Claimed");
  }

  /**
   * @param id
   *           the userID of the backpack to check
   * @return String
   *         returns a string formatted print of the user's backpack
   */
  public String backpack(long id) {
    // used to navigate a user's backpack
    String bpPrint = ("User ID: " + id + "\n");
    for (Pokemon p : userData.getUserStorageMap().get(id).getPokemon()) {
      bpPrint = bpPrint + p.toString() + "\n";
    }
    return bpPrint;
  }

  // adds a new user to the userMap
  public void newUser(Long userID, UserStorage newUserData) {
    userData.getUserStorageMap().put(userID, newUserData);
  }

  // returns the userstorage data of a given user
  public UserStorage getIndividualUser(Long userID) {
    return userData.getUserStorageMap().get(userID);
  }

  public Storage load(long serverID) {
    /*
     * READ STORED DATA AND SEE IF THE SERVER EXISTS
     * IF NOT CREATE NEW STORAGE DATA
     */
    return this;
  }

  // Writes this specific instance of Storage into XML saved by server id
  public void writeToXML() {
    String xmlString = "";
    try {
      JAXBContext context = JAXBContext.newInstance(UserStorageMap.class);
      Marshaller m = context.createMarshaller();

      m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE); // To format XML

      StringWriter sw = new StringWriter();
      m.marshal(userData, sw);
      xmlString = sw.toString();
    } catch (JAXBException e) {
      e.printStackTrace();
    }
    try {
      System.out.println(String.valueOf("PokemonBot\\servers\\" + serverID + ".xml"));
      BufferedWriter writer = new BufferedWriter(
          new FileWriter(String.valueOf("PokemonBot\\servers\\" + serverID + ".xml")));
      writer.write(xmlString);

      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * @param f
   *          file to load to memory
   * @return UserStorageMap
   *         the loaded userstoragemap
   */
  public UserStorageMap load(File f) {
    try {
      JAXBContext jc = JAXBContext.newInstance(UserStorageMap.class);
      Unmarshaller unmarshaller = jc.createUnmarshaller();
      StreamSource streamSource = new StreamSource(new StringReader(FileUtils.readFileToString(f)));
      JAXBElement<UserStorageMap> jaxbElement = unmarshaller.unmarshal(streamSource, UserStorageMap.class);

      UserStorageMap map = jaxbElement.getValue();
      for (Map.Entry<Long, UserStorage> entry : map.getUserStorageMap().entrySet())
        System.out.println("Key = " + entry.getKey() +
            ", Value = " + entry.getValue().getPokemon());
      userData = map;
      return map;
    } catch (IOException | JAXBException e) {
      System.out.println("exception was pushed " + e.getMessage());
      return null;
    }
  }

}