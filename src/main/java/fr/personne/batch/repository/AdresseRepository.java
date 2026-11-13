package fr.personne.batch.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import fr.personne.batch.domain.Adresse;

public interface AdresseRepository extends JpaRepository<Adresse, Long> {
}
