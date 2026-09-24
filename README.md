# LiveBridge (Android)

App Android en Kotlin / Jetpack Compose, façon OBS Studio mais pensée mobile, en 3 écrans :

| Écran | Ce que ça fait |
|---|---|
| **Configuration** | Titre, catégorie, destination (YouTube/Twitch/Facebook/serveur perso), micro, réduction de bruit, caméra avant/arrière, aperçu, bouton « Commencer le direct » |
| **Éléments** | Aperçu tactile plein cadre : glisser une source pour la déplacer, pincer pour la redimensionner. Calques en dessous (œil, cadenas, monter/descendre, renommer, supprimer) |
| **En direct** | Tableau de bord : minuteur, débit, réseau, batterie, raccourcis (couper le micro, changer de caméra, enregistrer), bouton « Finir le direct » |

Caméra + micro vers YouTube, Twitch, Facebook ou n'importe quel serveur RTMP/RTMPS/SRT (MediaMTX, SRS,
nginx-rtmp, Red5…), avec logo et texte dans le flux, qualité 480p / 720p / 1080p **en portrait natif**
(pas de bandes noires). Moteur : RootEncoder (`GenericStream`).

VDO.Ninja et OBS (télécommande) ont été retirés à la demande de l'utilisateur : l'app n'ouvre plus
aucun service tiers.

## Lancer le projet

1. Ouvre le dossier dans **Android Studio** (version récente) et laisse la synchronisation Gradle se faire.
   Si Studio propose de mettre à jour AGP / Kotlin / Gradle, accepte.
2. Branche un téléphone Android 8+ (minSdk 26) en mode débogage USB, puis **Run**.
   Un vrai téléphone est nécessaire : l'émulateur n'a pas de vraie caméra.

## Nouveautés v1.6 : système de design et polish UI/UX

- **Système de design** centralisé (`ui/Theme.kt`) : palette élargie (succès/erreur/avertissement/info),
  typographie, rayons de bordure et espacements définis une fois et propagés automatiquement à tous
  les boutons, champs, cartes et menus.
- **Bannières de message** cohérentes (succès/erreur/avertissement/info) à la place du texte rouge brut,
  avec apparition/disparition animée.
- **Transitions en fondu** entre les écrans Configuration / Éléments / En direct.
- **Indicateur DIRECT qui pulse**, indicateur de chargement sur l'aperçu pendant la connexion.
- **État vide** dans Éléments (message + icône quand aucun logo/texte n'a encore été ajouté).
- **Confirmation avant suppression** d'une source (au lieu d'un tap qui supprime instantanément).
- **Zones tactiles agrandies** (44dp minimum) sur les icônes de calques (œil, cadenas, monter/descendre,
  renommer, supprimer).

**Ce qui n'est pas encore fait**, par souci d'honnêteté sur la portée : pas d'écran de première
utilisation (onboarding), pas de bannière dédiée « pas de connexion », pas de bottom sheets (l'app n'en
a pas besoin structurellement pour l'instant). Comme pour tout le reste du projet, cette passe n'a pas
pu être vérifiée visuellement (aucun rendu ni capture d'écran disponible ici) : le prochain build reste
le seul vrai test.

## Nouveautés v1.5 : décalage audio/vidéo

- **Cadence vidéo forcée à 30 im/s** (repli automatique si l'appareil ne le permet pas). Sans cadence
  imposée, si la caméra ralentit (faible luminosité, chauffe du téléphone), l'image prend du retard
  sur le son — c'est la cause la plus courante d'un décalage qui grandit pendant le direct.
- **Bouton « Resynchroniser »** dans le tableau de bord En direct : coupe puis relance la connexion
  vers le même serveur (quelques secondes de coupure côté spectateurs), pour repartir sur une horloge
  audio/vidéo propre sans devoir tout réarmer depuis Configuration.

**À distinguer** : un direct « en retard de quelques secondes » pour les spectateurs par rapport à ce
qui se passe en vrai est normal — YouTube/Twitch retardent volontairement le flux (souvent 5 à 15 s
en mode normal, réglable dans YouTube Studio → réglages avancés → latence). Ce n'est pas un bug de
l'app. Seul un décalage entre le son et l'image qui grandit dans le temps (visible même dans un
enregistrement local) est le problème que cette version cible.

## Nouveautés v1.4 : rotation de l'écran et mises en page paysage

- **Rotation libre** : le verrou portrait a été retiré. Tourner le téléphone bascule chaque écran
  (Configuration, Éléments, En direct) sur une mise en page dédiée en paysage, inspirée de tes
  maquettes — aperçu à gauche, formulaire/calques/statistiques à droite.
- **L'encodage suit l'orientation** : en paysage, le direct s'encode en 16:9 ; en portrait, en 9:16.
  Les 3 qualités (480p/720p/1080p) gardent leur netteté dans les deux sens.
- **Orientation verrouillée pendant un direct ou un enregistrement** : changer les dimensions de
  l'encodeur en cours de diffusion la couperait, donc l'app garde l'orientation choisie au démarrage
  jusqu'à l'arrêt (tourner le téléphone reste possible visuellement, mais l'encodage ne change pas
  tant que le direct est en cours — point à confirmer au test, cette limite est volontaire).

## Nouveautés v1.3 : refonte complète de l'interface

- **VDO.Ninja et OBS retirés** : plus d'onglets, plus de navigation — un seul flux Configuration → Éléments → En direct.
- **Format portrait natif** (9:16) : la vidéo remplit l'écran, sans bandes noires. Les 3 qualités
  (480p/720p/1080p) sont maintenant en dimensions portrait.
- **Manipulation tactile des sources** : glisser un logo/texte pour le déplacer, pincer à deux doigts
  pour le redimensionner, directement sur l'aperçu. Remplace l'ancien réglage par menu (9 ancrages fixes).
  **Point d'incertitude** : la position libre utilise une méthode de RootEncoder appelée par réflexion
  (car son nom exact varie selon la version de la librairie) ; si elle n'existe pas dans la version
  installée, l'app se replie automatiquement sur l'ancrage fixe le plus proche du point choisi — le
  glisser fonctionne toujours, mais peut « accrocher » à 9 positions plutôt que rester libre. À vérifier
  après ce build.
- **Réduction de bruit / écho** : interrupteur dans Configuration. Désactivée par défaut, car ce
  traitement ajoute un léger délai entre le son et l'image sur beaucoup de téléphones — piste pour
  atténuer le décalage audio/vidéo que tu avais remarqué. **Important à savoir** : le délai total
  jusqu'aux spectateurs (souvent plusieurs secondes sur YouTube/Twitch) est ajouté par la plateforme
  elle-même et ne peut pas être supprimé par l'app ; ce réglage agit uniquement sur un éventuel
  désynchronisation de l'app entre son propre son et sa propre image.

## Depuis v1.1 / v1.2 (toujours présent)

- YouTube en RTMPS par défaut (port 443), bascule RTMP possible.
- Journal (dans Configuration → réglages avancés) : raison exacte d'un échec de connexion, sans jamais
  afficher la clé de stream. Reconnexion automatique (3 essais).
- Enregistrement local MP4 dans `Android/data/com.livebridge/files/Movies/`.
- Serveurs open source : dossier `server/` (MediaMTX, SRS, FFmpeg multistream, notes nginx-rtmp / Red5).

## Limites connues

- Pas de niveaux audio (VU-mètres) : RootEncoder n'expose pas de mesure fiable.
- Pas de nombre de spectateurs ni d'alertes (abonnés, dons) : ça demande de connecter les API officielles
  de YouTube/Twitch (identification du compte, webhooks) — hors du périmètre de cette version.
- Non compilé ni testé ici (pas de SDK Android disponible) : le prochain build GitHub Actions reste le
  vrai test.
