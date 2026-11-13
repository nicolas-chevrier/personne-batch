package fr.personne.batch.job;

/** Au moins une ligne du fichier a été rejetée : l'import entier est abandonné. */
public class ImportRejeteException extends RuntimeException {

	public ImportRejeteException(long lignesRejetees) {
		super(lignesRejetees + " ligne(s) rejetée(s) : rien n'est publié, les tables personne et adresse sont inchangées");
	}

}
