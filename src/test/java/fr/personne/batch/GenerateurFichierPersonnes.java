package fr.personne.batch;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Génère un fichier CSV de personnes, toujours le même pour un nombre de lignes donné.
 * <p>
 * Les premières lignes reprennent des personnes et adresses de data.sql, le reste est
 * tiré au hasard parmi un nombre limité d'adresses, pour que plusieurs personnes
 * partagent la même. Sur demande, une ligne sur {@value #PERIODE_AGE_INVALIDE} a un
 * âge invalide.
 * <p>
 * Pour régénérer les fichiers de input/ après {@code ./mvnw test-compile} :
 *
 * <pre>
 * java -cp target/test-classes fr.personne.batch.GenerateurFichierPersonnes input/personnes.csv 20000
 * java -cp target/test-classes fr.personne.batch.GenerateurFichierPersonnes input/personnes-invalides.csv 20000 invalides
 * </pre>
 */
public final class GenerateurFichierPersonnes {

	public static final String EN_TETE = "nom;prenom;age;profession;numero;rue;codePostal;ville";

	/** Présentes dans data.sql, sauf la dernière qui arrive à une adresse déjà en base. */
	private static final List<String> LIGNES_CONNUES = List.of(
			"Dupont;Marie;35;Infirmière;12;rue de la Paix;75002;Paris",
			"Dupont;Paul;36;Enseignant;12;rue de la Paix;75002;Paris",
			"Durand;Luc;52;Pâtissier;5;avenue Jean Jaurès;69007;Lyon",
			"Lefèvre;Nina;19;Étudiante;8;place du Capitole;31000;Toulouse");

	private static final int PERIODE_AGE_INVALIDE = 1337;

	private static final int NOMBRE_ADRESSES = 6000;

	private static final String[] NOMS = { "Martin", "Bernard", "Thomas", "Robert", "Richard", "Simon", "Laurent",
			"Michel", "Garcia", "David", "Bertrand", "Roux", "Vincent", "Fournier", "Morel", "Girard", "André",
			"Mercier", "Blanc", "Guérin", "Boyer", "Garnier", "Chevalier", "François", "Legrand", "Gauthier",
			"Perrin", "Robin", "Clément", "Morin", "Nicolas", "Henry", "Roussel", "Mathieu", "Gautier", "Masson",
			"Marchand", "Duval", "Denis", "Lemaire" };

	private static final String[] PRENOMS = { "Emma", "Louise", "Jade", "Alice", "Chloé", "Lina", "Léa", "Rose",
			"Anna", "Mila", "Inès", "Ambre", "Julia", "Manon", "Camille", "Sarah", "Zoé", "Agathe", "Juliette",
			"Lucie", "Gabriel", "Léo", "Raphaël", "Arthur", "Louis", "Lucas", "Adam", "Jules", "Maël", "Noah",
			"Ethan", "Nathan", "Théo", "Sacha", "Tom", "Paul", "Victor", "Antoine", "Maxime", "Julien" };

	private static final String[] PROFESSIONS = { "Infirmier", "Enseignant", "Boulanger", "Développeur", "Comptable",
			"Plombier", "Médecin", "Avocat", "Architecte", "Électricien", "Journaliste", "Pharmacien", "Cuisinier",
			"Chauffeur", "Agriculteur", "Vendeur", "Ingénieur", "Coiffeur", "Menuisier", "Retraité" };

	private static final String[] RUES = { "rue Victor Hugo", "rue de la République", "rue Pasteur", "rue Gambetta",
			"rue du Moulin", "rue des Écoles", "rue de l'Église", "rue Nationale", "rue Voltaire", "rue Carnot",
			"avenue de la Gare", "avenue Foch", "avenue des Tilleuls", "avenue du Général Leclerc",
			"boulevard Saint-Michel", "boulevard de la Liberté", "boulevard Clemenceau", "place de la Mairie",
			"place du Marché", "place Bellecour", "chemin des Vignes", "chemin du Lavoir", "allée des Acacias",
			"allée des Cerisiers", "impasse du Château", "impasse des Roses", "quai de la Loire", "quai des Brumes",
			"cours Lafayette", "route de Paris" };

	/** Code postal puis ville. */
	private static final String[][] VILLES = { { "75011", "Paris" }, { "69003", "Lyon" }, { "13001", "Marseille" },
			{ "31400", "Toulouse" }, { "33000", "Bordeaux" }, { "44000", "Nantes" }, { "67000", "Strasbourg" },
			{ "59000", "Lille" }, { "35000", "Rennes" }, { "34000", "Montpellier" }, { "06000", "Nice" },
			{ "38000", "Grenoble" }, { "21000", "Dijon" }, { "49000", "Angers" }, { "51100", "Reims" } };

	/**
	 * Ce que la base doit contenir de plus après l'import du fichier.
	 *
	 * @param lignesValides lignes dont l'âge est un nombre
	 * @param lignesInvalides lignes que le batch doit rejeter
	 * @param personnesDistinctes couples nom et prénom différents parmi les lignes valides
	 * @param adressesDistinctes adresses différentes parmi les lignes valides
	 */
	public record Bilan(int lignesValides, int lignesInvalides, int personnesDistinctes, int adressesDistinctes) {
	}

	private GenerateurFichierPersonnes() {
	}

	public static void main(String[] args) throws IOException {
		Path fichier = Path.of(args.length > 0 ? args[0] : "input/personnes.csv");
		int lignes = args.length > 1 ? Integer.parseInt(args[1]) : 20_000;
		boolean avecAgesInvalides = args.length > 2 && args[2].equals("invalides");
		System.out.println(fichier.toAbsolutePath() + " : " + generer(fichier, lignes, avecAgesInvalides));
	}

	public static Bilan generer(Path fichier, int nombreLignes, boolean avecAgesInvalides) throws IOException {
		Random hasard = new Random(42);
		Set<String> personnes = new HashSet<>();
		Set<String> adresses = new HashSet<>();
		int invalides = 0;
		if (fichier.getParent() != null) {
			Files.createDirectories(fichier.getParent());
		}
		try (BufferedWriter sortie = Files.newBufferedWriter(fichier, StandardCharsets.UTF_8)) {
			sortie.write(EN_TETE);
			sortie.newLine();
			for (int i = 0; i < nombreLignes; i++) {
				String nom;
				String prenom;
				String age;
				String profession;
				String adresse;
				if (i < LIGNES_CONNUES.size()) {
					String[] champs = LIGNES_CONNUES.get(i).split(";", 5);
					nom = champs[0];
					prenom = champs[1];
					age = champs[2];
					profession = champs[3];
					adresse = champs[4];
				}
				else {
					nom = nom(hasard);
					prenom = PRENOMS[hasard.nextInt(PRENOMS.length)];
					age = String.valueOf(18 + hasard.nextInt(70));
					profession = PROFESSIONS[hasard.nextInt(PROFESSIONS.length)];
					adresse = adresse(hasard.nextInt(NOMBRE_ADRESSES));
					if (avecAgesInvalides && i % PERIODE_AGE_INVALIDE == 0) {
						age = (invalides % 2 == 0) ? "inconnu" : age + " ans";
					}
				}
				sortie.write(String.join(";", nom, prenom, age, profession, adresse));
				sortie.newLine();
				if (age.chars().allMatch(Character::isDigit)) {
					personnes.add(nom + ";" + prenom);
					adresses.add(adresse);
				}
				else {
					invalides++;
				}
			}
		}
		return new Bilan(nombreLignes - invalides, invalides, personnes.size(), adresses.size());
	}

	/**
	 * Nom simple ou composé (« Martin-Roux ») : assez de combinaisons pour que la plupart
	 * des lignes désignent des personnes différentes, tout en gardant quelques doublons.
	 */
	private static String nom(Random hasard) {
		String premier = NOMS[hasard.nextInt(NOMS.length)];
		String second = NOMS[hasard.nextInt(NOMS.length)];
		return premier.equals(second) ? premier : premier + "-" + second;
	}

	/** Chaque indice donne une adresse différente. */
	private static String adresse(int indice) {
		String[] ville = VILLES[indice % VILLES.length];
		String rue = RUES[(indice / VILLES.length) % RUES.length];
		int numero = 1 + indice / (VILLES.length * RUES.length);
		return String.join(";", String.valueOf(numero), rue, ville[0], ville[1]);
	}

}
