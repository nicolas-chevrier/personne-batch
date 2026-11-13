package fr.personne.batch.job;

import org.springframework.batch.infrastructure.item.file.LineMapper;
import org.springframework.batch.infrastructure.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.infrastructure.item.file.transform.FieldSet;

import fr.personne.batch.domain.LignePersonne;

/**
 * Format d'une ligne, séparateur « ; » :
 *
 * <pre>
 * nom;prenom;age;profession;numero;rue;codePostal;ville
 * </pre>
 *
 * Une ligne qui n'a pas exactement ces huit colonnes lève une exception, que le
 * reader transforme en FlatFileParseException.
 */
public class LignePersonneLineMapper implements LineMapper<LignePersonne> {

	private final DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer(";");

	public LignePersonneLineMapper() {
		tokenizer.setNames("nom", "prenom", "age", "profession", "numero", "rue", "codePostal", "ville");
	}

	@Override
	public LignePersonne mapLine(String ligne, int numeroLigne) {
		// readString retire les espaces en début et fin de valeur.
		FieldSet champs = tokenizer.tokenize(ligne);
		return new LignePersonne(numeroLigne, champs.readString("nom"), champs.readString("prenom"),
				champs.readString("age"), champs.readString("profession"), champs.readString("numero"),
				champs.readString("rue"), champs.readString("codePostal"), champs.readString("ville"));
	}

}
