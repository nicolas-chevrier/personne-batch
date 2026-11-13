package fr.personne.batch.job;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import fr.personne.batch.config.PersonneBatchProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Lance le job au démarrage de l'application.
 * <p>
 * --fichier désigne le CSV (par défaut personne.batch.fichier). --idImport identifie
 * l'import et vaut par défaut la date et l'heure du lancement : chaque lancement est
 * donc un nouvel import. Relancer avec l'idImport d'un import en échec le reprend
 * après son dernier chunk validé.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "personne.batch.runner.enabled", matchIfMissing = true)
public class ImportPersonnesJobRunner implements ApplicationRunner, ExitCodeGenerator {

	public static final String FICHIER = "fichier";

	public static final String ID_IMPORT = "idImport";

	private final JobOperator jobOperator;

	private final Job importPersonnesJob;

	private final PersonneBatchProperties properties;

	private int codeRetour;

	@Override
	public void run(ApplicationArguments args) throws Exception {
		JobParameters parametres = new JobParametersBuilder()
			.addString(FICHIER, option(args, FICHIER, properties.fichier()))
			.addString(ID_IMPORT, option(args, ID_IMPORT, LocalDateTime.now().toString()))
			.toJobParameters();

		JobExecution execution = jobOperator.start(importPersonnesJob, parametres);
		log.info("Job {} terminé : {}", importPersonnesJob.getName(), execution.getStatus());
		codeRetour = (execution.getStatus() == BatchStatus.COMPLETED) ? 0 : 1;
	}

	@Override
	public int getExitCode() {
		return codeRetour;
	}

	private static String option(ApplicationArguments args, String nom, String defaut) {
		List<String> valeurs = args.getOptionValues(nom);
		return (valeurs == null || valeurs.isEmpty()) ? defaut : valeurs.get(0);
	}

}
