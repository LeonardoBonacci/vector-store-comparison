package guru.bonacci.vectorstore;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.stream.IntStream;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;

import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class VectorStoreEvaluator implements CommandLineRunner {

  private final CustomChatMemoryRepository memory;
  private final ChatClient chatModel;
  @Getter private final VectorStore vectorStore;
  
  @Value("classpath:/docs/Nietzsche.pdf")
  @Getter private Resource pdfResource;

  @Value("classpath:/prompts/please-rate.st")
  private Resource prPromptTemplate;

  @Value("classpath:/questions.txt")
  private Resource questions;

  public VectorStoreEvaluator(CustomChatMemoryRepository memory, ChatClient.Builder builder, VectorStore vectorStore) {
  	this.memory = memory;
  	this.chatModel = builder.build();
    this.vectorStore = vectorStore;
  }

  public abstract void load();

  
  @Override
  public void run(String... args) {
  	load();
    double finalScore = 
    		readQuestions().stream()
    		.<Double>mapMulti((question, consumer) -> {
          try {
              consumer.accept(evaluate(question));
          } catch (RuntimeException e) {
              log.warn(e.getMessage());
          }
    		})
    		.mapToDouble(Double::doubleValue)
    		.average().getAsDouble();
    log.info("overall score {}", String.format("%.2f%n", finalScore));
  }

  double evaluate(final String question) {
  	log.info(question);
  	
    // Retrieve documents similar to a query
    final var vectorResultsAsText = similarDocuments(question);
    if (vectorResultsAsText.isEmpty()) {
    	log.error("no search results, try harder");
    	System.exit(0);
    	return 0;
    }
    
    log.debug("I am bored");
    vectorResultsAsText.forEach(vr -> log.debug(vr));

    String fullText = extractContent(pdfResource);
    
    var mapper = new ObjectMapper();
    JsonMapper.builder()
	    	.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
	    	.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
	    	.enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
	    	.build();
    var parser = new BeanOutputConverter<>(Response.class, mapper);
    
		PromptTemplate promptTemplate = new PromptTemplate(prPromptTemplate);
    var promptParams = new HashMap<String, Object>();
    promptParams.put("question", question);
    promptParams.put("sourceDocument", fullText);
    promptParams.put("vectorStoreSearchResult", String.join("\n", vectorResultsAsText));
    promptParams.put("memory", memory.asString());
    promptParams.put("format", parser.getFormat());
    
    double avg = IntStream.range(0, Application.NUM_RUNS)
        .map(i -> {
          int rating = rate(promptTemplate.create(promptParams), parser);
          memory.add("pgvector", vectorResultsAsText, rating);
          log.info("run {} - rating: {}", i + 1, rating);
          return rating;
        })
        .average()
        .getAsDouble();

    log.info("on average {}", String.format("%.2f%n", avg));
    return avg;
  }
  
  List<String> readQuestions() {
    try {
        Path path = questions.getFile().toPath();
        return Files.readAllLines(path);
    } catch (Exception e) {
        throw new RuntimeException("failed to read file " + questions.getFilename(), e);
    }
}
  
  
  List<String> similarDocuments(final String query) {
    return 
    		this.vectorStore.similaritySearch(SearchRequest.builder().query(query).topK(3).build())
    		.stream().map(Document::getText).toList();
  }
  
  int rate(final Prompt prompt, final BeanOutputConverter<Response> parser) {
    Response response =
    		chatModel
    			.prompt(prompt)
					.call()
					.entity(parser);
    
    log.debug("I am really bored");
    log.info(response.explanation());
    return response.rating();
  }
  
  String extractContent(final Resource loadMe) {
  	String text = "";
    try (final PDDocument document = Loader.loadPDF(loadMe.getFile())) {
        text = new PDFTextStripper().getText(document);
        log.debug("I am truly very bored");
        log.debug(text);
    } catch (final Exception ex) {
        log.error("error parsing pdf", ex);
        System.exit(0);
    }
    
    return text;
  }
}