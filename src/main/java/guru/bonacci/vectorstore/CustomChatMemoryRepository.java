package guru.bonacci.vectorstore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CustomChatMemoryRepository {

	record DocRating(List<String> similarRecords, int rating) {}; 
	
	Map<String, List<DocRating>> chatMemoryStore = new ConcurrentHashMap<>();

	
	public void add(String vectorStoreName, List<String> similarDocuments, int rating) {
		chatMemoryStore.putIfAbsent(vectorStoreName, new ArrayList<DocRating>());
		chatMemoryStore.get(vectorStoreName).add(new DocRating(similarDocuments, rating));
	}

	public String asString() {
		String oneliner = chatMemoryStore.entrySet()
				.stream()
				.map(kv -> {
					var sbuilder = new StringBuilder();
					String vStore = kv.getKey();
					sbuilder.append(vStore);
					
					kv.getValue().forEach(docRating -> {
						sbuilder.append(" had rating " + docRating.rating());
						sbuilder.append(" for the following documents \n" + String.join("\n\n", docRating.similarRecords()));
					});
					
					return sbuilder.toString();
				})
				.collect(Collectors.joining("\n\n"));
		
		log.debug("yep, incredibly bored indeed {}", oneliner);
		return oneliner;
	}
}
