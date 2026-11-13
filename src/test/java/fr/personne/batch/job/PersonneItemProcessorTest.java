package fr.personne.batch.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import fr.personne.batch.domain.LignePersonne;
import fr.personne.batch.domain.PersonneImport;

class PersonneItemProcessorTest {

	private final PersonneItemProcessor processor = new PersonneItemProcessor();

	@Test
	void convertitUneLigneEnEntiteDeLaTableIntermediaire() {
		PersonneImport personne = processor.process(ligne("42"));

		assertThat(personne.getNumeroLigne()).isEqualTo(7);
		assertThat(personne.getNom()).isEqualTo("Dupont");
		assertThat(personne.getPrenom()).isEqualTo("Marie");
		assertThat(personne.getAge()).isEqualTo(42);
		assertThat(personne.getProfession()).isEqualTo("Infirmière");
		assertThat(personne.getNumero()).isEqualTo("12");
		assertThat(personne.getRue()).isEqualTo("rue de la Paix");
		assertThat(personne.getCodePostal()).isEqualTo("75002");
		assertThat(personne.getVille()).isEqualTo("Paris");
	}

	@ParameterizedTest
	@ValueSource(strings = { "0", "7", "100" })
	void accepteUnAgeEntier(String age) {
		assertThat(processor.process(ligne(age)).getAge()).isEqualTo(Integer.parseInt(age));
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "", "abc", "12.5", "12,5", "-3", "34 ans", "1e2", "1234" })
	void rejetteUnAgeQuiNEstPasUnNombre(String age) {
		assertThatThrownBy(() -> processor.process(ligne(age))).isInstanceOf(LigneInvalideException.class)
			.hasMessageContaining("Ligne 7")
			.hasMessageContaining("n'est pas un nombre");
	}

	private static LignePersonne ligne(String age) {
		return new LignePersonne(7, "Dupont", "Marie", age, "Infirmière", "12", "rue de la Paix", "75002", "Paris");
	}

}
