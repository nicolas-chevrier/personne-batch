package fr.personne.batch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Réglages du batch, préfixe {@code personne.batch} dans application.yml.
 *
 * @param fichier fichier CSV importé quand --fichier n'est pas fourni
 * @param chunkSize nombre de lignes traitées par transaction
 * @param skipLimit nombre de lignes rejetées signalées avant d'arrêter la lecture
 */
@ConfigurationProperties("personne.batch")
public record PersonneBatchProperties(String fichier, int chunkSize, int skipLimit) {
}
