package fr.personne.batch.config;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.database.JpaItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import fr.personne.batch.domain.LignePersonne;
import fr.personne.batch.domain.PersonneImport;
import fr.personne.batch.job.LigneInvalideException;
import fr.personne.batch.job.LignePersonneLineMapper;
import fr.personne.batch.job.LigneRejeteeListener;
import fr.personne.batch.job.MesureTempsListener;
import fr.personne.batch.job.PersonneItemProcessor;
import fr.personne.batch.job.PublicationTasklet;
import fr.personne.batch.repository.PersonneImportRepository;
import jakarta.persistence.EntityManagerFactory;

/**
 * Import tout ou rien, en trois steps :
 * <ol>
 * <li>purge de la table intermédiaire ;</li>
 * <li>lecture du fichier par chunks, contrôle, écriture dans la table intermédiaire ;</li>
 * <li>publication vers adresse et personne, en une seule transaction.</li>
 * </ol>
 * Les chunks ne touchent jamais aux tables finales. Si une ligne est rejetée ou si une
 * écriture échoue, la publication n'a pas lieu, ou elle est annulée en entier.
 */
@Configuration
public class ImportPersonnesJobConfiguration {

	@Bean
	Job importPersonnesJob(JobRepository jobRepository, Step purgeImportStep, Step importFichierStep,
			Step publicationStep, MesureTempsListener mesureTemps) {
		return new JobBuilder("importPersonnesJob", jobRepository).listener(mesureTemps)
			.start(purgeImportStep)
			.next(importFichierStep)
			.next(publicationStep)
			.build();
	}

	/** Rejoué même lors d'une reprise : un import repart toujours d'une table intermédiaire vide. */
	@Bean
	Step purgeImportStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
			PersonneImportRepository importRepository) {
		return new StepBuilder("purgeImportStep", jobRepository).tasklet((contribution, chunkContext) -> {
			importRepository.vider();
			return RepeatStatus.FINISHED;
		}, transactionManager).allowStartIfComplete(true).build();
	}

	@Bean
	Step importFichierStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
			FlatFileItemReader<LignePersonne> personneReader, PersonneItemProcessor processor,
			JpaItemWriter<PersonneImport> personneImportWriter, LigneRejeteeListener ligneRejetee,
			MesureTempsListener mesureTemps, PersonneBatchProperties properties) {
		return new StepBuilder("importFichierStep", jobRepository)
			.<LignePersonne, PersonneImport>chunk(properties.chunkSize())
			.transactionManager(transactionManager)
			.reader(personneReader)
			.processor(processor)
			.writer(personneImportWriter)
			// Une ligne mal formée est journalisée et la lecture continue, pour signaler toutes
			// les lignes fautives en un seul passage. PublicationTasklet refuse ensuite de publier.
			.faultTolerant()
			.skip(FlatFileParseException.class, LigneInvalideException.class)
			.skipLimit(properties.skipLimit())
			.skipListener(ligneRejetee)
			.listener(mesureTemps)
			// Comme la purge : une reprise relit le fichier depuis le début.
			.allowStartIfComplete(true)
			.build();
	}

	@Bean
	Step publicationStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
			PublicationTasklet publicationTasklet, MesureTempsListener mesureTemps) {
		return new StepBuilder("publicationStep", jobRepository).tasklet(publicationTasklet, transactionManager)
			.listener((StepExecutionListener) mesureTemps)
			.build();
	}

	@Bean
	@StepScope
	FlatFileItemReader<LignePersonne> personneReader(@Value("#{jobParameters['fichier']}") String fichier) {
		return new FlatFileItemReaderBuilder<LignePersonne>().name("personneReader")
			.resource(new FileSystemResource(fichier))
			.encoding("UTF-8")
			// Première ligne : les noms de colonnes.
			.linesToSkip(1)
			.lineMapper(new LignePersonneLineMapper())
			// Pas de position mémorisée : la purge vide la table, il faut donc tout relire.
			.saveState(false)
			.build();
	}

	/** persist plutôt que merge : pas de SELECT avant chaque INSERT, et les INSERT partent par lots. */
	@Bean
	JpaItemWriter<PersonneImport> personneImportWriter(EntityManagerFactory entityManagerFactory) {
		return new JpaItemWriterBuilder<PersonneImport>().entityManagerFactory(entityManagerFactory)
			.usePersist(true)
			.build();
	}

}
