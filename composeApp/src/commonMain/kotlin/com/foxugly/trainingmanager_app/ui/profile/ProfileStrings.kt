package com.foxugly.trainingmanager_app.ui.profile

object ProfileStrings {
    const val title = "Profil"
    const val emailLabel = "E-mail"
    const val firstName = "Prénom"
    const val lastName = "Nom"
    const val language = "Langue"
    val languageNames = mapOf(
        "fr" to "Français",
        "nl" to "Nederlands",
        "en" to "English",
        "it" to "Italiano",
        "es" to "Español",
    )
    const val weeklyRecap = "Récapitulatif hebdomadaire par e-mail"
    const val digestEmail = "Résumé quotidien des notifications"
    const val save = "Enregistrer"
    const val saved = "Profil enregistré."
    const val loadFailed = "Impossible de charger le profil."
    const val saveFailed = "L'enregistrement a échoué. Réessayez."
    const val changePasswordCta = "Changer le mot de passe"
    const val back = "Retour"

    // Change-password
    const val cpTitle = "Changer le mot de passe"
    const val currentPassword = "Mot de passe actuel"
    const val newPassword = "Nouveau mot de passe"
    const val confirmPassword = "Confirmer le mot de passe"
    const val cpSubmit = "Valider"
    const val cpCurrentInvalid = "Le mot de passe actuel est incorrect."
    const val cpUnchanged = "Le nouveau mot de passe doit être différent de l'actuel."
    const val cpWeak = "Ce mot de passe est trop faible."
    const val mismatch = "Les mots de passe ne correspondent pas."
    const val tooShort = "Le mot de passe doit faire au moins 8 caractères."
    const val cpSuccess = "Mot de passe mis à jour."
    const val cpFailed = "La modification a échoué. Réessayez."
}
