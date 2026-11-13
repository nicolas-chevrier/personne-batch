package fr.personne.batch.job;

import java.util.regex.Pattern;

import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import fr.personne.batch.domain.LignePersonne;
import fr.personne.batch.domain.PersonneImport;

/** Contrôle le format d'une ligne puis la convertit en entité JPA de la table intermédiaire. */
@Component
public class PersonneItemProcessor implements ItemProcessor<LignePersonne, PersonneImport> {

	/** Un nombre entier positif, de trois chiffres au plus. */
	private static final Pattern AGE = Pattern.compile("\\d{1,3}");

	@Override
	public PersonneImport process(LignePersonne ligne) {
		return PersonneImport.builder()
			.numeroLigne(ligne.numeroLigne())
			.nom(ligne.nom())
			.prenom(ligne.prenom())
			.age(lireAge(ligne))
			.profession(ligne.profession())
			.numero(ligne.numero())
			.rue(ligne.rue())
			.codePostal(ligne.codePostal())
			.ville(ligne.ville())
			.build();
	}

	private int lireAge(LignePersonne ligne) {
		String age = ligne.age();
		if (age == null || !AGE.matcher(age).matches()) {
			throw new LigneInvalideException(ligne.numeroLigne(), "l'âge « " + age + " » n'est pas un nombre");
		}
		return Integer.parseInt(age);
	}

}
