# Pokémane
Creators: Garrett Keaton, Hayden Duplantier, Spencer Malone, Jonathan Lagarrigue, Koby Ramsey, Brandon de Baroncelli  

How to Set Up:  
  1. Pull all files from this repository  
  2. Open the file in any desired IDE.  
  3. Add the Bot to a discord Server or Create a new Bot using the same source code (need admin in discord server):  
        3a. How to Add the Bot to a server (May have interference with other servers using the bot)  
              i. Paste https://discord.com/api/oauth2/authorize?client_id=1076208826434465903&permissions=8&scope=bot into a common browser  
              ii. Choose a server for the bot to join  
              
        3b. How to Create a unique Bot using the same source code (More setup required)  
              i. Follow directions at https://www.ionos.com/digitalguide/server/know-how/creating-discord-bot/ to learn how to create a unique bot  
              ii. In PokemonBot.java change the Authentication key to the unique token key for your now created Bot  
  4.Running the Bot through an IDE should now cause the bot to come online and be useable.
  5. type !defualtchannel in any text channel you want the bot to use as its default for its messages.
  
  
COMMON ISSUES WITH SETUP: Depending on the host's file system, the file "pokemonMaster.xml" which controls what Pokémon get summoned
                          may not get found. If this is an issue, go to Pokemon.java and change the location where it is searching for the file.
                          You may also need to create a folder called "servers" in the root of the project folder (aka \The-People-Project\servers)
                           
How to Use:  
        The Bot currently uses basic commands that are posted inside of discord chat channels.  
                    Commands:  
                        IMPORTANT:
                        !defaultchannel- sets the default channel that the bot will generate pokemon to. Should do as soon as you start the code.
                        --------------------------------------
                        !pmhelp gives the list of commands.  
                        !pmstarter- lets you chose a starter if you don't have one already  
                        !pmbp- lets you view your backpack  
                        !pmclaim- lets you claim an event pokemon  
                        !pmbat- lets you battle another trainer  
                        !pmtrade- lets you trade pokemon   
                        -----------------------------------
                        !generate- testing legacy command that lets you generate a random gen1 Pokemon (generated every 5 minutes)
                        !save- command for testing the saving of the XMLs 
                        
