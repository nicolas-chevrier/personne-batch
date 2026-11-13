package fr.personne.batch.job;

/** Ligne du fichier dont le format est incorrect : elle est rejetée, sans arrêter le job. */
public class LigneInvalideException extends RuntimeException {

	public LigneInvalideException(int numeroLigne, String message) {
		super("Ligne " + numeroLigne + " : " + message);
	}

}
