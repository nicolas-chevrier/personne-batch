package fr.personne.batch.domain;

/**
 * Une ligne du fichier CSV, telle qu'elle a été lue : tout est encore du texte,
 * le contrôle de format se fait dans PersonneItemProcessor.
 */
public record LignePersonne(int numeroLigne, String nom, String prenom, String age, String profession,
		String numero, String rue, String codePostal, String ville) {
}
