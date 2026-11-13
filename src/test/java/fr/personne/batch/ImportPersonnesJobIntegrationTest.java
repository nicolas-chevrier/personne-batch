package fr.personne.batch;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.test.JobOperatorTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.springframework.batch.test.StepScopeTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import fr.personne.batch.GenerateurFichierPersonnes.Bilan;
import fr.personne.batch.domain.LignePersonne;
import fr.personne.batch.job.ImportPersonnesJobRunner;
import fr.personne.batch.repository.AdresseRepository;
import fr.personne.batch.repository.PersonneImportRepository;
import fr.personne.batch.repository.PersonneRepository;

/**
 * Lance le vrai job sur une base PostgreSQL. Avant chaque test, la base est remise dans
 * l'état de data.sql : 4 adresses et 5 personnes, dont certaines sont aussi dans les fichiers.
 */
@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
@Sql(statements = "truncate table personne, adresse, personne_import")
@Sql(scripts = "classpath:data.sql")
class ImportPersonnesJobIntegrationTest {

	private static final int ADRESSES_INITIALES = 4;

	private static final int PERSONNES_INITIALES = 5;

	@Autowired
	private JobOperatorTestUtils jobOperatorTestUtils;

	@Autowired
	private JobRepositoryTestUtils jobRepositoryTestUtils;

	@Autowired
	private Job importPersonnesJob;

	@Autowired
	private FlatFileItemReader<LignePersonne> personneReader;

	@Autowired
	private PersonneRepository personnes;

	@Autowired
	private AdresseRepository adresses;

	@Autowired
	private PersonneImportRepository tableIntermediaire;

	@Autowired
	private JdbcTemplate jdbc;

	@BeforeEach
	void preparer() {
		jobRepositoryTestUtils.removeJobExecutions();
		jobOperatorTestUtils.setJob(importPersonnesJob);
	}

	@Test
	void leReaderLitChaqueLigneApresLEnTete() throws Exception {
		StepExecution stepExecution = MetaDataInstanceFactory
			.createStepExecution(parametres(ressource("personnes-invalides.csv")));

		List<LignePersonne> lignes = StepScopeTestUtils.doInStepScope(stepExecution, () -> {
			List<LignePersonne> lues = new ArrayList<>();
			personneReader.open(new ExecutionContext());
			try {
				// La 7e ligne de données, incomplète, arrêterait la lecture : on s'arrête avant.
				for (int i = 0; i < 6; i++) {
					lues.add(personneReader.read());
				}
			}
			finally {
				personneReader.close();
			}
			return lues;
		});

		assertThat(lignes).hasSize(6);
		assertThat(lignes.get(0)).isEqualTo(
				new LignePersonne(2, "Dupont", "Marie", "35", "Infirmière", "12", "rue de la Paix", "75002", "Paris"));
		assertThat(lignes.get(3).age()).isEqualTo("abc");
	}

	@Test
	void importeLeFichierSansDupliquerCeQuiEstDejaEnBase() throws Exception {
		JobExecution execution = jobOperatorTestUtils.startJob(parametres(ressource("personnes-valides.csv")));

		assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		assertThat(Steps.de(execution, "importFichierStep").getWriteCount()).isEqualTo(7);
		assertThat(tableIntermediaire.count()).isZero();

		// Nouvelles : Nina, Léo et Zoé. Nouvelle adresse : celle des Roux, partagée.
		assertThat(personnes.count()).isEqualTo(PERSONNES_INITIALES + 3);
		assertThat(adresses.count()).isEqualTo(ADRESSES_INITIALES + 1);
		assertThat(compter("select count(*) from adresse where rue = 'rue Neuve'")).isEqualTo(1);
		assertThat(compter("""
				select count(*) from personne p join adresse a on a.id = p.adresse_id
				where p.nom = 'Roux' and a.rue = 'rue Neuve'
				""")).isEqualTo(2);

		// Déjà en base : mise à jour de Marie, Paul inchangé, chacun toujours en un seul exemplaire.
		assertThat(age("Dupont", "Marie")).isEqualTo(35);
		assertThat(age("Dupont", "Paul")).isEqualTo(36);
		// Déjà en base à Lyon, à Toulouse dans le fichier : même personne, mise à jour avec sa nouvelle adresse.
		assertThat(age("Durand", "Luc")).isEqualTo(53);
		assertThat(jdbc.queryForObject("""
				select a.ville from personne p join adresse a on a.id = p.adresse_id
				where p.nom = 'Durand' and p.prenom = 'Luc'
				""", String.class)).isEqualTo("Toulouse");
		// Son ancienne adresse reste en base, avec son autre habitant.
		assertThat(compter("""
				select count(*) from personne p join adresse a on a.id = p.adresse_id where a.ville = 'Lyon'
				""")).isEqualTo(1);
		// Présent deux fois dans le fichier : la dernière ligne l'emporte.
		assertThat(age("Roux", "Léo")).isEqualTo(45);
		// Nouvelle personne rattachée à une adresse déjà en base.
		assertThat(compter("""
				select count(*) from personne p join adresse a on a.id = p.adresse_id
				where p.nom = 'Lefèvre' and a.rue = 'place du Capitole'
				""")).isEqualTo(1);
		// En base mais absents du fichier : conservés.
		assertThat(age("Moreau", "Jeanne")).isEqualTo(67);
		assertThat(age("Petit", "Hugo")).isEqualTo(28);
	}

	@Test
	void unSecondImportDuMemeFichierNeChangeRien() throws Exception {
		jobOperatorTestUtils.startJob(parametres(ressource("personnes-valides.csv")));
		long personnesApresPremierImport = personnes.count();
		long adressesApresPremierImport = adresses.count();

		JobExecution execution = jobOperatorTestUtils.startJob(parametres(ressource("personnes-valides.csv")));

		assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		assertThat(personnes.count()).isEqualTo(personnesApresPremierImport);
		assertThat(adresses.count()).isEqualTo(adressesApresPremierImport);
	}

	@Test
	void uneLigneInvalideAnnuleToutLImport() throws Exception {
		JobExecution execution = jobOperatorTestUtils.startJob(parametres(ressource("personnes-invalides.csv")));

		assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
		// Les deux lignes fautives sont signalées dans le même passage : colonnes manquantes, âge invalide.
		StepExecution importFichier = Steps.de(execution, "importFichierStep");
		assertThat(importFichier.getReadSkipCount()).isEqualTo(1);
		assertThat(importFichier.getProcessSkipCount()).isEqualTo(1);
		assertThat(Steps.de(execution, "publicationStep").getStatus()).isEqualTo(BatchStatus.FAILED);

		// Les six lignes valides du fichier n'ont rien changé.
		assertBaseInchangee();
	}

	@Test
	void uneErreurDEcritureDansUnChunkAnnuleLesChunksPrecedents(@TempDir Path dossier) throws Exception {
		// 1 200 lignes valides, soit deux chunks complets, puis un nom trop long pour la base.
		Path fichier = dossier.resolve("personnes.csv");
		GenerateurFichierPersonnes.generer(fichier, 1_200, false);
		Files.writeString(fichier, "N".repeat(150) + ";Marie;35;Infirmière;12;rue de la Paix;75002;Paris\n",
				StandardCharsets.UTF_8, StandardOpenOption.APPEND);

		JobExecution execution = jobOperatorTestUtils.startJob(parametres(fichier.toString()));

		assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
		StepExecution importFichier = Steps.de(execution, "importFichierStep");
		assertThat(importFichier.getStatus()).isEqualTo(BatchStatus.FAILED);
		// Les premiers chunks ont bien été validés, mais seulement dans la table intermédiaire.
		assertThat(importFichier.getCommitCount()).isGreaterThanOrEqualTo(2);
		assertThat(tableIntermediaire.count()).isGreaterThanOrEqualTo(1_000);
		assertBaseInchangee();
	}

	@Test
	void uneErreurPendantLaPublicationAnnuleToutLImport() throws Exception {
		// Contrainte posée pour le test : elle refuse une personne du fichier, après la création des adresses.
		jdbc.execute("alter table personne add constraint refus_test check (nom <> 'Lefèvre')");
		try {
			JobExecution execution = jobOperatorTestUtils.startJob(parametres(ressource("personnes-valides.csv")));

			assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
			assertThat(Steps.de(execution, "importFichierStep").getStatus()).isEqualTo(BatchStatus.COMPLETED);
			assertThat(Steps.de(execution, "publicationStep").getStatus()).isEqualTo(BatchStatus.FAILED);
			// L'adresse des Roux, créée dans la même transaction, a disparu avec le reste.
			assertBaseInchangee();
		}
		finally {
			jdbc.execute("alter table personne drop constraint refus_test");
		}
	}

	@Test
	void echoueQuandLeFichierEstIntrouvable() throws Exception {
		JobExecution execution = jobOperatorTestUtils.startJob(parametres("fichier-absent.csv"));

		assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
		assertBaseInchangee();
	}

	@Test
	void importeVingtMilleLignes(@TempDir Path dossier) throws Exception {
		Path fichier = dossier.resolve("personnes.csv");
		Bilan bilan = GenerateurFichierPersonnes.generer(fichier, 20_000, false);

		JobExecution execution = jobOperatorTestUtils.startJob(parametres(fichier.toString()));

		assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		StepExecution importFichier = Steps.de(execution, "importFichierStep");
		assertThat(importFichier.getReadCount()).isEqualTo(20_000);
		assertThat(importFichier.getWriteCount()).isEqualTo(20_000);
		// 500 lignes par chunk : au moins 40 transactions.
		assertThat(importFichier.getCommitCount()).isGreaterThanOrEqualTo(40);

		// En base avant l'import et absents du fichier : Hugo, Jeanne et l'adresse de Jeanne.
		assertThat(personnes.count()).isEqualTo(bilan.personnesDistinctes() + 2);
		assertThat(adresses.count()).isEqualTo(bilan.adressesDistinctes() + 1);
		assertThat(age("Dupont", "Marie")).isEqualTo(35);

		// Les dates enregistrées par Spring Batch donnent la durée du job.
		Duration duree = Duration.between(execution.getStartTime(), execution.getEndTime());
		System.out.printf("Import de 20 000 lignes : %d ms%n", duree.toMillis());
		assertThat(duree).isPositive();
	}

	@Test
	void vingtMilleLignesDontQuelquesAgesInvalidesNeChangentRien(@TempDir Path dossier) throws Exception {
		Path fichier = dossier.resolve("personnes.csv");
		Bilan bilan = GenerateurFichierPersonnes.generer(fichier, 20_000, true);

		JobExecution execution = jobOperatorTestUtils.startJob(parametres(fichier.toString()));

		assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
		assertThat(bilan.lignesInvalides()).isEqualTo(14);
		assertThat(Steps.de(execution, "importFichierStep").getProcessSkipCount()).isEqualTo(14);
		assertBaseInchangee();
	}

	/** Les tables finales sont exactement dans l'état de data.sql. */
	private void assertBaseInchangee() {
		assertThat(personnes.count()).isEqualTo(PERSONNES_INITIALES);
		assertThat(adresses.count()).isEqualTo(ADRESSES_INITIALES);
		assertThat(age("Dupont", "Marie")).isEqualTo(34);
	}

	/**
	 * Dans une classe à part : @SpringBatchTest prend pour une fabrique toute méthode
	 * de la classe de test qui renvoie un StepExecution.
	 */
	private static final class Steps {

		static StepExecution de(JobExecution execution, String nom) {
			return execution.getStepExecutions()
				.stream()
				.filter(step -> step.getStepName().equals(nom))
				.findFirst()
				.orElseThrow();
		}

	}

	private static String ressource(String nom) throws Exception {
		return new ClassPathResource(nom).getFile().getAbsolutePath();
	}

	/** idImport différent à chaque appel : chaque lancement est un nouvel import. */
	private JobParameters parametres(String fichier) {
		return new JobParametersBuilder().addString(ImportPersonnesJobRunner.FICHIER, fichier)
			.addString(ImportPersonnesJobRunner.ID_IMPORT, String.valueOf(System.nanoTime()))
			.toJobParameters();
	}

	private int age(String nom, String prenom) {
		return jdbc.queryForObject("select age from personne where nom = ? and prenom = ?", Integer.class, nom,
				prenom);
	}

	private long compter(String requete) {
		return jdbc.queryForObject(requete, Long.class);
	}

}
