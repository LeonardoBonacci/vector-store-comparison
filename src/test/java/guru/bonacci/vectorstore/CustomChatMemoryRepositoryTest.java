package guru.bonacci.vectorstore;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CustomChatMemoryRepositoryTest {

	private CustomChatMemoryRepository repository;

	@BeforeEach
	void setUp() {
		repository = new CustomChatMemoryRepository();
	}

	@Test
	void testAddSingleEntry() {
		String vectorStoreName = "storeA";
		List<String> documents = List.of("Doc 1", "Doc 2");
		int rating = 85;

		repository.add(vectorStoreName, documents, rating);

		assertDoesNotThrow(() -> repository.add(vectorStoreName, documents, rating));
	}

	@Test
	void testAsStringEmpty() {
		String result = repository.asString();
		assertEquals("", result);
	}

	@Test
	void testAsStringAfterAdd() {
		repository.add("storeA", List.of("Doc A", "Doc B"), 70);
		repository.add("storeB", List.of("Doc F", "Doc R"), 70);
		repository.add("storeC", List.of("Doc T", "Doc G"), 70);

		String result = repository.asString();

		assertNotNull(result);
		
		System.out.println(result);
	}
}
