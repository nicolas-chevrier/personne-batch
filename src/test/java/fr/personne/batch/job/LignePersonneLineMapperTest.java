package fr.personne.batch.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.file.transform.IncorrectTokenCountException;

import fr.personne.batch.domain.LignePersonne;

class LignePersonneLineMapperTest {

	private final LignePersonneLineMapper mapper = new LignePersonneLineMapper();

	@Test
	void decoupeLesHuitColonnes() {
		LignePersonne ligne = mapper.mapLine("Dupont;Marie;35;Infirmière;12;rue de la Paix;75002;Paris", 2);

		assertThat(ligne).isEqualTo(
				new LignePersonne(2, "Dupont", "Marie", "35", "Infirmière", "12", "rue de la Paix", "75002", "Paris"));
	}

	@Test
	void retireLesEspacesAutourDesValeurs() {
		LignePersonne ligne = mapper.mapLine(" Dupont ; Marie ;35 ;Infirmière;12;rue de la Paix;75002;Paris", 2);

		assertThat(ligne.nom()).isEqualTo("Dupont");
		assertThat(ligne.prenom()).isEqualTo("Marie");
		assertThat(ligne.age()).isEqualTo("35");
	}

	@Test
	void laisseLAgeTelQuelPourLeControleDeFormat() {
		assertThat(mapper.mapLine("Dupont;Marie;abc;Infirmière;12;rue de la Paix;75002;Paris", 2).age())
			.isEqualTo("abc");
	}

	@Test
	void refuseUneLigneQuiNAPasHuitColonnes() {
		assertThatThrownBy(() -> mapper.mapLine("Garcia;Tom;51;Plombier;7;rue Verte", 8))
			.isInstanceOf(IncorrectTokenCountException.class);
	}

}
