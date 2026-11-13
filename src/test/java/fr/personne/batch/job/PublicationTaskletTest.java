package fr.personne.batch.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.batch.test.MetaDataInstanceFactory;

import fr.personne.batch.repository.PersonneImportRepository;

@ExtendWith(MockitoExtension.class)
class PublicationTaskletTest {

	@Mock
	private PersonneImportRepository importRepository;

	@InjectMocks
	private PublicationTasklet tasklet;

	private StepExecution importFichier;

	private StepContribution contribution;

	@BeforeEach
	void preparer() {
		JobExecution job = MetaDataInstanceFactory.createJobExecution();
		importFichier = new StepExecution(1L, "importFichierStep", job);
		StepExecution publication = new StepExecution(2L, "publicationStep", job);
		for (StepExecution step : List.of(importFichier, publication)) {
			if (!job.getStepExecutions().contains(step)) {
				job.addStepExecution(step);
			}
		}
		contribution = new StepContribution(publication);
	}

	@Test
	void publieLesAdressesPuisLesPersonnesPuisVideLaTableIntermediaire() {
		when(importRepository.publierAdresses()).thenReturn(2);
		when(importRepository.publierPersonnes()).thenReturn(5);

		RepeatStatus statut = tasklet.execute(contribution, null);

		assertThat(statut).isEqualTo(RepeatStatus.FINISHED);
		assertThat(contribution.getWriteCount()).isEqualTo(5);
		InOrder ordre = inOrder(importRepository);
		ordre.verify(importRepository).publierAdresses();
		ordre.verify(importRepository).publierPersonnes();
		ordre.verify(importRepository).vider();
	}

	@Test
	void nePublieRienSiUneLigneAEteRejeteeAuControle() {
		importFichier.setProcessSkipCount(1);

		assertThatThrownBy(() -> tasklet.execute(contribution, null)).isInstanceOf(ImportRejeteException.class)
			.hasMessageContaining("1 ligne(s) rejetée(s)");
		verifyNoInteractions(importRepository);
	}

	@Test
	void nePublieRienSiUneLigneAEteRejeteeALaLecture() {
		importFichier.setReadSkipCount(3);

		assertThatThrownBy(() -> tasklet.execute(contribution, null)).isInstanceOf(ImportRejeteException.class)
			.hasMessageContaining("3 ligne(s) rejetée(s)");
		verifyNoInteractions(importRepository);
	}

}
