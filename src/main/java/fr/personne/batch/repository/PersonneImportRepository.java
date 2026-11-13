package fr.personne.batch.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import fr.personne.batch.domain.PersonneImport;

/** Table intermédiaire, et sa publication vers les tables finales. */
public interface PersonneImportRepository extends JpaRepository<PersonneImport, Integer> {

	@Modifying
	@Query(value = "truncate table personne_import", nativeQuery = true)
	void vider();

	/**
	 * Crée les adresses de l'import qui ne sont pas encore en base, une seule fois chacune.
	 * Une adresse déjà en base est réutilisée telle quelle : ses quatre colonnes forment
	 * sa clé, il n'y a donc rien d'autre à mettre à jour.
	 */
	@Modifying
	@Query(value = """
			insert into adresse (id, numero, rue, code_postal, ville)
			select nextval('adresse_seq'), d.numero, d.rue, d.code_postal, d.ville
			from (select distinct numero, rue, code_postal, ville from personne_import) d
			where not exists (
			    select 1 from adresse a
			    where a.numero = d.numero and a.rue = d.rue
			    and a.code_postal = d.code_postal and a.ville = d.ville
			)
			""", nativeQuery = true)
	int publierAdresses();

	/**
	 * Crée les personnes de l'import. Celles dont le nom et le prénom existent déjà ne sont
	 * pas recréées : leur âge, leur profession et leur adresse sont mis à jour. Une personne
	 * présente plusieurs fois dans le fichier garde les valeurs de sa dernière ligne.
	 * À appeler après {@link #publierAdresses()}.
	 */
	@Modifying
	@Query(value = """
			insert into personne (id, nom, prenom, age, profession, adresse_id)
			select nextval('personne_seq'), d.nom, d.prenom, d.age, d.profession, d.adresse_id
			from (
			    select distinct on (i.nom, i.prenom)
			        i.nom, i.prenom, i.age, i.profession, a.id as adresse_id
			    from personne_import i
			    join adresse a
			        on a.numero = i.numero and a.rue = i.rue
			        and a.code_postal = i.code_postal and a.ville = i.ville
			    order by i.nom, i.prenom, i.numero_ligne desc
			) d
			on conflict (nom, prenom)
			do update set age = excluded.age, profession = excluded.profession, adresse_id = excluded.adresse_id
			""", nativeQuery = true)
	int publierPersonnes();

}
