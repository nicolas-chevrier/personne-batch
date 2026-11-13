package fr.personne.batch.job;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.ChunkListener;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.stereotype.Component;

import fr.personne.batch.domain.LignePersonne;
import fr.personne.batch.domain.PersonneImport;
import lombok.extern.slf4j.Slf4j;

/**
 * Mesure le temps d'exécution à trois niveaux : le job, chaque step et, en niveau DEBUG,
 * chaque chunk. Les durées du job et des steps viennent des dates que Spring Batch
 * enregistre aussi dans BATCH_JOB_EXECUTION et BATCH_STEP_EXECUTION.
 */
@Slf4j
@Component
public class MesureTempsListener
		implements JobExecutionListener, StepExecutionListener, ChunkListener<LignePersonne, PersonneImport> {

	/** Le step tourne sur un seul thread : un champ suffit pour le chunk en cours. */
	private long debutChunk;

	@Override
	public void afterJob(JobExecution execution) {
		log.info("Job {} : {} en {}", execution.getJobInstance().getJobName(), execution.getStatus(),
				formater(duree(execution.getStartTime(), execution.getEndTime())));
	}

	@Override
	public ExitStatus afterStep(StepExecution execution) {
		Duration duree = duree(execution.getStartTime(), execution.getEndTime());
		if (execution.getReadCount() == 0 && execution.getSkipCount() == 0) {
			// Step sans lecture : la publication.
			log.info("Step {} : {} en {}", execution.getStepName(), execution.getExitStatus().getExitCode(),
					formater(duree));
		}
		else {
			log.info("Step {} : {} lignes lues, {} écrites, {} rejetées, {} commits en {} ({} lignes/s)",
					execution.getStepName(), execution.getReadCount(), execution.getWriteCount(),
					execution.getSkipCount(), execution.getCommitCount(), formater(duree),
					debit(execution.getReadCount(), duree));
		}
		return execution.getExitStatus();
	}

	@Override
	public void beforeChunk(Chunk<LignePersonne> chunk) {
		debutChunk = System.nanoTime();
	}

	@Override
	public void afterChunk(Chunk<PersonneImport> chunk) {
		if (log.isDebugEnabled()) {
			log.debug("Chunk de {} lignes écrit en {} ms", chunk.size(), (System.nanoTime() - debutChunk) / 1_000_000);
		}
	}

	/** La date de fin n'est pas toujours renseignée au moment où le listener est appelé. */
	static Duration duree(LocalDateTime debut, LocalDateTime fin) {
		return Duration.between(debut, (fin != null) ? fin : LocalDateTime.now());
	}

	static String formater(Duration duree) {
		return String.format(Locale.FRANCE, "%.3f s", duree.toMillis() / 1000.0);
	}

	static long debit(long lignes, Duration duree) {
		return lignes * 1000 / Math.max(duree.toMillis(), 1);
	}

}
