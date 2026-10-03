package nz.ac.auckland.grocerfy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling 
public class GrocerfyApplication {

	public static void main(String[] args) {
		SpringApplication.run(GrocerfyApplication.class, args);
	}

}
