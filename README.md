# Affiche Mod

Mod Minecraft Forge 1.20.1 qui ajoute un système d'affiches murales personnalisables avec image distante, customisation de taille/position, et un système RP de dégradation/vandalisme.

[![Latest release](https://img.shields.io/github/v/release/Akirabane/Affiche-Mod?label=release)](https://github.com/Akirabane/Affiche-Mod/releases/latest)
![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-62B47A)
![Forge](https://img.shields.io/badge/Forge-47.4.x-1F6FEB)

---

## Sommaire

- [Aperçu](#aperçu)
- [Fonctionnalités](#fonctionnalités)
- [Installation](#installation)
- [Utilisation](#utilisation)
- [Mécaniques détaillées](#mécaniques-détaillées)
- [Build depuis les sources](#build-depuis-les-sources)
- [Structure du projet](#structure-du-projet)
- [Dépendances](#dépendances)

---

## Aperçu

L'**Affiche Viking** est un bloc qui se pose contre un mur, propose une UI d'édition browser (via MCEF) pour coller une image depuis une URL, ajuster sa taille et sa position, puis l'affiche en jeu sur un fond en planches de sapin servant de support.

Le mod inclut un système de dégradation à 5 paliers pour des interactions RP : un autre joueur peut « vandaliser » une affiche via un mini-jeu QTE de plus en plus dur, et le propriétaire (ou n'importe qui d'autre) peut la nettoyer avec une éponge.

## Fonctionnalités

### Affiche
- Pose uniquement sur faces verticales (parois) — la base reste sur le mur cliqué.
- Image chargée depuis une URL HTTP(S), affichée en jeu sans téléchargement persistant côté serveur.
- Taille réglable de **0,5 à 16 blocs** sur chaque axe.
- Ratios rapides : 16:9, 9:16, 4:3, 1:1.
- Décalage X/Y pour positionner l'image hors de l'ancrage du bloc.
- Backdrop en `spruce_planks` dépassant l'image d'environ 2 px de chaque côté (effet panneau).
- Bloc vide = aspect de planche de sapin sur 1×1.
- Rendu visible des deux côtés (utile quand l'affiche dépasse du bloc d'ancrage).

### Système de dégradation
- **5 paliers** (1 à 5). À 5, l'affiche est "trop dégradée" et ne peut plus être ciblée.
- **QTE non-propriétaire** : difficulté croissante (5 → 10 touches, 8 → 5 s).
- **Lettres localisées** sur le QTE : la lettre affichée correspond à la touche physique du joueur (AZERTY → A, QWERTY → A à la même position physique).
- **Cooldown personnel 30 min** par joueur. Chaque joueur a son propre timer ; un autre joueur n'est pas bloqué.
- **Dégradation naturelle** : **+1 niveau / heure** automatique tant que l'affiche existe et n'est pas au max.
- **Édition unique post-QTE** : un joueur qui réussit le QTE obtient un droit d'édition à usage unique (image/taille/position) — pas de transfert de propriété.
- **Éponge** : item qui retire **1 niveau** de dégradation par utilisation. Utilisable par tout joueur, consomme 1 unité de l'inventaire (sauf en créatif).

### Visuels de dégradation
- Voile noir progressif sur l'image (≈ 6 % → 41 % d'opacité selon le niveau).
- **N couches grunge stackées** (1 à 5 selon le niveau), chacune avec une rotation/flip distinct → le motif s'accumule, pas seulement l'opacité.
- Teinte marron foncé (60, 40, 15) sur le grunge.
- Tout rendu via un seul `RenderType` pour garantir un ordre de blend stable.

### Cassage du bloc
- **Vide** : vitesse normale de planche (~0,75 s à la main).
- **Avec image** : temps multiplié par `min(40, w × h)`. Une affiche 16×16 prend ~30 s à la main.
- **Avec hache** (n'importe quel niveau de hache) : divise le temps par 2.

## Installation

1. Installer **Minecraft Forge 1.20.1 (47.4.x)**.
2. Installer la dépendance **[MCEF](https://www.curseforge.com/minecraft/mc-mods/mcef)** (Minecraft Chromium Embedded Framework) — nécessaire pour l'UI d'édition browser.
3. Télécharger le JAR depuis la [dernière release](https://github.com/Akirabane/Affiche-Mod/releases/latest) et le placer dans `mods/`.
4. Lancer le jeu. Au premier démarrage, MCEF télécharge Chromium (~150 MB).

## Utilisation

### Poser une affiche
1. Récupère l'item **Affiche Viking** (créatif) ou via crafting custom de ton pack.
2. Clic droit sur une face verticale d'un bloc solide. L'affiche apparaît à plat contre le mur, vide.
3. Re-clic droit en tant que propriétaire → l'éditeur browser s'ouvre.
4. Colle une URL d'image, ajuste la taille/ratio/décalage, valide.

### Vandaliser une affiche (non-propriétaire)
1. Clic droit sur une affiche qui ne t'appartient pas.
2. Si tu n'es pas en cooldown et que l'affiche n'est pas au max : un **QTE** apparaît. Tape la séquence avant la fin du timer.
3. Succès → +1 niveau de dégradation + droit d'édition à usage unique (tu peux changer l'image une fois).
4. Échec ou ESC → rien ne se passe, pas de cooldown déclenché.

### Nettoyer une affiche
1. Récupère une **Éponge** (créatif).
2. Clic droit avec l'éponge en main sur une affiche dégradée.
3. -1 niveau de dégradation, -1 éponge dans l'inventaire (sauf créatif).

## Mécaniques détaillées

### Cooldown joueur
- Stocké par UUID dans la NBT du bloc (`playerCooldowns`).
- Comparaison sur `level.getGameTime()` (ticks monde).
- Persistant entre sessions et chargements de chunk.

### Dégradation naturelle
- Gérée par un `BlockEntityTicker` côté serveur uniquement.
- Compare `currentTick - lastNaturalTick >= 72000` (1 h = 72 000 ticks).
- Le timer s'initialise au premier tick du BE pour éviter un saut de niveau immédiat après chargement.

### Difficulté QTE par palier

| Niveau cible | Lettres | Temps |
|---|---|---|
| 1 | 5 | 8,0 s |
| 2 | 6 | 7,0 s |
| 3 | 7 | 6,0 s |
| 4 | 8 | 5,5 s |
| 5 | 10 | 5,0 s |

### Opacités cumulées d'overlay

| Niveau | Voile noir | Grunge stacké (1 − (1 − a)^N) |
|---|---|---|
| 1 | ~6 % | ~35 % |
| 2 | ~14 % | ~58 % |
| 3 | ~24 % | ~73 % |
| 4 | ~33 % | ~83 % |
| 5 | ~41 % | ~89 % |

### Compatibilité sauvegardes
- L'ancien champ NBT `degraded` (boolean) est lu en fallback → niveau 1 si présent.

## Build depuis les sources

```bash
git clone https://github.com/Akirabane/Affiche-Mod.git
cd Affiche-Mod
./gradlew build
```

JAR produit : `build/libs/vikingposter-1.0.0.jar`.

Prérequis : **JDK 17** (Minecraft 1.20.1 utilise Java 17).

## Structure du projet

```
src/main/
├── java/com/vikingposter/
│   ├── VikingPosterMod.java          # Entrée mod
│   ├── ClientSetup.java              # Hook client (BER, creative tab)
│   ├── blocks/PosterBlock.java       # Logique du bloc (use, ticker, cassage)
│   ├── blockentity/                  # BE (état, NBT, cooldowns, dégradation)
│   ├── client/web/                   # UI MCEF (PosterEditScreen/Bridge)
│   ├── qte/QTEScreen.java            # Mini-jeu QTE
│   ├── network/                      # Packets C2S/S2C
│   ├── renderer/                     # BER + ImageCache
│   ├── registration/                 # DeferredRegisters
│   └── items/SpongeEraserItem.java
└── resources/
    ├── assets/vikingposter/
    │   ├── blockstates/, models/, lang/
    │   ├── textures/                 # Overlay grunge + éponge
    │   └── web/                      # poster_edit.html/css/js (UI MCEF)
    └── META-INF/mods.toml
```

## Dépendances

| Dépendance | Rôle |
|---|---|
| **Forge 1.20.1-47.4.x** | API mod loader |
| **MCEF** | Navigateur Chromium embarqué pour l'UI d'édition |

## Crédits

Développé par **Akirabane**.

Texture grunge de dégradation fournie par l'auteur.

---

*Pour un retour de bug ou une demande de feature, ouvre une issue sur [le repo](https://github.com/Akirabane/Affiche-Mod/issues).*
