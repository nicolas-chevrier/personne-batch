package fr.personne.batch.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Une ligne du fichier, contrôlée, en attente dans la table intermédiaire. Elle ne
 * devient une {@link Personne} et une {@link Adresse} qu'à la publication.
 */
@Entity
@Table(name = "personne_import")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PersonneImport {

	/** Numéro de la ligne dans le fichier : unique, et il départage les doublons à la publication. */
	@Id
	@Column(name = "numero_ligne")
	private Integer numeroLigne;

	@Column(nullable = false, length = 100)
	private String nom;

	@Column(nullable = false, length = 100)
	private String prenom;

	@Column(nullable = false)
	private Integer age;

	@Column(nullable = false, length = 100)
	private String profession;

	@Column(nullable = false, length = 10)
	private String numero;

	@Column(nullable = false, length = 150)
	private String rue;

	@Column(name = "code_postal", nullable = false, length = 10)
	private String codePostal;

	@Column(nullable = false, length = 100)
	private String ville;

}
