package fr.personne.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PersonneBatchApplication {

	public static void main(String[] args) {
		// Code retour du processus : 0 si le job a réussi, 1 sinon (voir ImportPersonnesJobRunner).
		System.exit(SpringApplication.exit(SpringApplication.run(PersonneBatchApplication.class, args)));
	}

}
