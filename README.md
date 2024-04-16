# Rallye Dashboard FR

Une application Android designé pour le Rallye Routier moto.
Permet de remplacer le dérouleur et un compteur odométre :
- afficher un roadbook converti en une série d'image.
- afficher un odométre piloté par gps (lecture de la vitesse)
- afficher l'heure exacte
- afficher un chronométre déclencher automatiquement après une distance parcourue
- afficher des statistiques de vitesses
- piloté par une télécommande bluetooth

Requier une tablette ou une smartphone Android 8 mini (API 28)
Autorisation requise pour accéder à la position GPS et la lecture du répertoire contenant les roadbooks.

## Manuel de l'utilisateur en français
Le manuel est lisible ici : https://github.com/b0bChoK/Rallye_Dashboard/blob/main/docs/Rallye%20Dashboard%20User%20Manual%20FR.pdf

## Convertion du roadbook pdf en serie d'image
L'application supporte le format IZ Roadbook, convertisser vos roadbook ici https://www.izroadbook.com/roadbook et copier les fichiers sur votre appareil Android

## Télécommande compatible
Supporte les télécommandes multimédia compatible android

## Contact développeur
Albert DEWAS, b0b_ChoK@yahoo.fr

--------------------------------------------------------

# Rally Dashboard EN

An Android application design for motorbike road race (mostly french discipline). 
Allow you to replace the traditional paper roadbook and include an odometer :
- display the roadbook converted in a set of image
- display an odometer reading gps speed
- display current time
- display a chronometer automatically start when moving
- display speed statistics
- remotely controlled by bluetooth media controller

Need a tablet or smartphone with Android 8 minimum (API 28)
Need request to access fine position and read access to directory containing the roadbook.

## Convert a pdf roadbook in a set of image
This app support the format IZ Roadbook, convert them here https://www.izroadbook.com/roadbook and copy/paste the file on your device.

## Remote compatible
Support any medi controller remote

## Contact creator
Albert DEWAS, b0b_ChoK@yahoo.fr

--------------------------------------------------------

# TODO list
- catch media key event without changing system state or launching other media app
- add different mapping for remote controller (ex : barbutton)
- add configuration to setup some parameters :
  - distance before starting chronometer
  - distance to increase / decrease odometer
  - button mapping
- User manual
- included pdf conversion
