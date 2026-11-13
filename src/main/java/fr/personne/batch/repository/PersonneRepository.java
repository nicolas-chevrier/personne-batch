package fr.personne.batch.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import fr.personne.batch.domain.Personne;

public interface PersonneRepository extends JpaRepository<Personne, Long> {
}
