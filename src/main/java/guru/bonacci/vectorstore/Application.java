package guru.bonacci.vectorstore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

	public static final int NUM_RUNS = 1;


	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
