package fr.personne.batch.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "adresse")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Adresse {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "adresse_seq")
	@SequenceGenerator(name = "adresse_seq", sequenceName = "adresse_seq", allocationSize = 1)
	private Long id;

	@Column(nullable = false, length = 10)
	private String numero;

	@Column(nullable = false, length = 150)
	private String rue;

	@Column(name = "code_postal", nullable = false, length = 10)
	private String codePostal;

	@Column(nullable = false, length = 100)
	private String ville;

}
