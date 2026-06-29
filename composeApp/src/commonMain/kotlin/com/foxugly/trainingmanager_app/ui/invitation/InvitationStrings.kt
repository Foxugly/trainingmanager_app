package com.foxugly.trainingmanager_app.ui.invitation

object InvitationStrings {
    const val loadingTitle = "Invitation…"
    fun joinTitle(team: String) = "Rejoindre $team"
    const val password = "Mot de passe"
    const val confirmPassword = "Confirmer le mot de passe"
    const val join = "Rejoindre l'équipe"
    const val alreadyHandledTitle = "Invitation indisponible"
    const val alreadyHandledBody = "Cette invitation a déjà été utilisée, a expiré ou a été annulée."
    const val emailTaken = "Un compte existe déjà pour cette adresse. Connectez-vous."
    const val mismatch = "Les mots de passe ne correspondent pas."
    const val tooShort = "Le mot de passe doit faire au moins 8 caractères."
    const val lookupFailed = "Impossible de charger l'invitation. Le lien est peut-être invalide ou expiré."
    const val joinFailed = "L'adhésion a échoué. Réessayez."
    const val backToLogin = "Retour à la connexion"
}
