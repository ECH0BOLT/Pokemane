package com.mycompany.pokemonbot;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
 

/*
 * Creates a map that can be used by JaxB to create XML file with the data
 * Acts as an adapter to implement HashMap
 */
@XmlRootElement (name="userStorages")
@XmlAccessorType(XmlAccessType.FIELD)
public class UserStorageMap {
  
  private Map<Long, UserStorage> userStorageMap = new HashMap<Long, UserStorage>();
 
  public Map<Long, UserStorage> getUserStorageMap() {
    return userStorageMap;
  }
 
  public void setUserStorageMap(Map<Long, UserStorage> userStorageMap) {
    this.userStorageMap = userStorageMap;
  }

  
}