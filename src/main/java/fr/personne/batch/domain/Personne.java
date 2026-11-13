package fr.personne.batch.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "personne")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Personne {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "personne_seq")
	@SequenceGenerator(name = "personne_seq", sequenceName = "personne_seq", allocationSize = 1)
	private Long id;

	@Column(nullable = false, length = 100)
	private String nom;

	@Column(nullable = false, length = 100)
	private String prenom;

	@Column(nullable = false)
	private Integer age;

	@Column(nullable = false, length = 100)
	private String profession;

	/** Plusieurs personnes peuvent partager la même adresse. */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "adresse_id", nullable = false)
	@ToString.Exclude
	private Adresse adresse;

}
