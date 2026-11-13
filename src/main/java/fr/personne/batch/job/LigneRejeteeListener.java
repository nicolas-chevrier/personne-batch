package fr.personne.batch.job;

import org.springframework.batch.core.listener.SkipListener;
import org.springframework.stereotype.Component;

import fr.personne.batch.domain.LignePersonne;
import fr.personne.batch.domain.PersonneImport;
import lombok.extern.slf4j.Slf4j;

/** Journalise chaque ligne rejetée, avec la raison du rejet. */
@Slf4j
@Component
public class LigneRejeteeListener implements SkipListener<LignePersonne, PersonneImport> {

	/** Ligne illisible : mauvais nombre de colonnes. */
	@Override
	public void onSkipInRead(Throwable cause) {
		log.warn("Ligne rejetée à la lecture : {}", cause.getMessage());
	}

	/** Ligne lue mais refusée par le contrôle de format. */
	@Override
	public void onSkipInProcess(LignePersonne ligne, Throwable cause) {
		log.warn("Ligne rejetée au contrôle : {}", cause.getMessage());
	}

}
