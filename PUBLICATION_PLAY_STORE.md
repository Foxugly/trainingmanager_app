# Publication sur le Google Play Store — TrainingManager

> Checklist opérateur pour publier **TrainingManager** (`com.foxugly.trainingmanager_app`)
> sur le Play Store. Suit le modèle de `Foxugly/PushIT_app` (`PUBLICATION_PLAY_STORE.md`).
> iOS n'est **pas** couvert : pas de projet Xcode, pas de token APNs (audit 2026-10-18, A-B3).

## Déjà en place

- Build `release` : R8 + `shrinkResources`, API prod épinglée (`https://tm-api.foxugly.com/api/v1/`),
  logs HTTP en debug uniquement, HTTPS seulement, jetons en EncryptedSharedPreferences.
- **Suppression de compte dans l'app** : Profil → « Supprimer mon compte » → mot de passe actuel +
  confirmation → `POST /api/v1/auth/account/delete/`. Un propriétaire d'équipe reçoit un 409
  (`owns_teams`) : il doit d'abord transférer ou supprimer ses équipes depuis le site web.
- Liens de connexion (magic link, confirmation d'e-mail) : l'app affiche le compte visé, et le compte
  qui serait déconnecté, avant toute bascule de session. Le schéma `trainingmanager://` n'existe qu'en
  build **debug** (`androidApp/src/debug/AndroidManifest.xml`) ; la release n'accepte que les App Links HTTPS.
- CI (`ci.yml`) : tests, APK debug **et** APK release non signé (R8), pour détecter une régression de
  keep-rules avant la publication.

## A. Clés de signature et build signé

**1. Keystore d'upload** (une fois, à conserver à vie, hors du repo) :
```bash
keytool -genkeypair -v -keystore tm-release.jks -alias trainingmanager \
  -keyalg RSA -keysize 2048 -validity 10000
```

**2. Secrets GitHub** (`Foxugly/trainingmanager_app` → Settings → Secrets and variables → Actions).
`.github/workflows/release.yml` s'arrête dès le départ en listant ceux qui manquent :

| Secret | Contenu |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | `base64 -w0 tm-release.jks` (PowerShell : `[Convert]::ToBase64String([IO.File]::ReadAllBytes("tm-release.jks"))`) |
| `ANDROID_KEYSTORE_PASSWORD` | mot de passe du keystore |
| `ANDROID_KEY_ALIAS` | alias de la clé (ex. `trainingmanager`) |
| `ANDROID_KEY_PASSWORD` | mot de passe de la clé |
| `GOOGLE_SERVICES_JSON` | le vrai `google-services.json` du projet Firebase TrainingManager (JSON brut ou base64 ; la valeur du secret CI `GOOGLE_SERVICES_JSON_B64` convient) |
| `TM_SENTRY_DSN` | DSN du projet Sentry `tm-app` (org `foxugly-srl`, `de.sentry.io`) |

Le placeholder CI est refusé en release. Sans `TM_SENTRY_DSN` (builds locaux, PR), Sentry n'est pas
initialisé du tout.

**3. Version** : `tm.versionName` dans `gradle.properties` (source unique), à bumper avant une release.
`versionCode` n'est plus écrit à la main (`androidApp/build.gradle.kts`, « Versioning ») :
`TM_VERSION_CODE` (forçage) → `GITHUB_RUN_NUMBER` → nombre de commits → 1. En release, c'est le numéro
de run du workflow *Release*. **N'uploader sur Play que des AAB produits par ce workflow** : un build
local prend le nombre de commits et pourrait passer devant le compteur.

**4. Build signé** : Actions → *Release* → *Run workflow*, ou un tag `v*`
(`git tag v1.0.0 && git push origin v1.0.0`). Le job lance les tests, construit
`:androidApp:bundleRelease` signé, vérifie la signature (`jarsigner`) et publie l'artifact
`trainingmanager-<versionName>-<run>` : `androidApp-release.aab` + `mapping.txt` (à garder pour
désobfusquer les traces). **Aucun upload Play automatique.**

**5. Alternative locale** (test uniquement) : `keystore.properties` à la racine (git-ignored) avec
`RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`, le vrai
`androidApp/google-services.json`, puis `.\gradlew.bat :androidApp:bundleRelease`.

## B. App Links : `assetlinks.json` (bloquant pour les liens e-mail)

Le fichier est servi par le **frontend** : repo `Foxugly/trainingmanager_frontend`,
`public/.well-known/assetlinks.json` → `https://tm.foxugly.com/.well-known/assetlinks.json`
(détail : `docs/deep-links-assetlinks.md` de ce repo). Il ne contient aujourd'hui que l'empreinte
**debug** d'un poste de dev.

Après le premier upload sur Play :
1. Play Console → *Test and release* → *App integrity* → *App signing* : copier le **SHA-256 de la clé
   de signature d'application** (celle de Google) et celui de la **clé d'upload**.
2. Les ajouter à `sha256_cert_fingerprints` dans `trainingmanager_frontend/public/.well-known/assetlinks.json`,
   **retirer l'empreinte debug** (sinon tout APK signé avec ce keystore debug peut capter les liens
   magic / confirmation / reset), pousser : le frontend se redéploie.
3. Vérifier : `adb shell pm get-app-links com.foxugly.trainingmanager_app` → `verified` pour `tm.foxugly.com`.

Aucune empreinte n'est inventée ici : elle n'existe qu'une fois l'app créée dans Play Console.

## C. Fiche Play Console

- **Data safety** : données collectées = e-mail, nom, données d'entraînement saisies, journaux de plantage
  (Sentry, sans PII). Chiffrement en transit : oui. **Suppression de compte : oui**, dans l'app (Profil →
  Supprimer mon compte) et sur le web (`https://tm.foxugly.com`, page Profil) : déclarer cette URL.
- Politique de confidentialité, captures d'écran, icône 512×512 et bannière 1024×500 : à produire
  (pas encore de métadonnées fastlane dans ce repo).
- Premier envoi sur la piste **interne**, puis tests sur appareil réel : liens e-mail (après l'étape B),
  push FCM, suppression de compte.
