# ChatNoir-QABIL-VINCENT


## Instructions pour Compiler les JARs

1. Assurez-vous que vous avez Java installé sur votre machine.
2. Ouvrez un terminal et naviguez vers le répertoire racine du projet `ChatNoir`.
3. Exécutez le script de build :
   ```bash
   ./script/build.sh
   ```
4. Les fichiers JAR seront générés dans le répertoire `build/jar`.

## Instructions pour Exécuter les Clients et le Serveur

### Lancer le Serveur

1. Ouvrez un terminal.
2. Naviguez vers le répertoire `build/jar`.
3. Exécutez le serveur :
   ```bash
   java -jar Server.jar
   ```

### Lancer un Client

1. Ouvrez un autre terminal.
2. Naviguez vers le répertoire `build/jar`.
3. Exécutez un client en fournissant le pseudonyme, l'adresse du serveur et le port :
   ```bash
   java -jar Client.jar <pseudonyme> <adresse_serveur> <port>
   ```
