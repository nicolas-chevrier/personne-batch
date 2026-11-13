package fr.personne.batch.job;

import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import fr.personne.batch.repository.PersonneImportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Recopie la table intermédiaire vers adresse et personne. Le step exécute ce tasklet
 * dans une seule transaction : si une requête échoue, aucune ligne de l'import ne reste
 * dans les tables finales.
 * <p>
 * Rien n'est publié si une ligne du fichier a été rejetée.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PublicationTasklet implements Tasklet {

	private final PersonneImportRepository importRepository;

	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
		long rejets = contribution.getStepExecution()
			.getJobExecution()
			.getStepExecutions()
			.stream()
			.mapToLong(StepExecution::getSkipCount)
			.sum();
		if (rejets > 0) {
			throw new ImportRejeteException(rejets);
		}

		// Les adresses d'abord : les personnes les référencent par clé étrangère.
		int adresses = importRepository.publierAdresses();
		int personnes = importRepository.publierPersonnes();
		importRepository.vider();
		log.info("Publication : {} adresses créées, {} personnes créées ou mises à jour", adresses, personnes);
		contribution.incrementWriteCount(personnes);
		return RepeatStatus.FINISHED;
	}

}
