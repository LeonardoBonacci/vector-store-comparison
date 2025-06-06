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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class VectorStoreEvaluator implements CommandLineRunner {

  private final ChatClient chatModel;
  @Getter private final VectorStore vectorStore;
  
  @Value("classpath:/docs/Nietzsche.pdf")
  @Getter private Resource pdfResource;

  @Value("classpath:/prompts/please-rate.st")
  private Resource prPromptTemplate;

  @Value("classpath:/questions.txt")
  private Resource questions;

  public VectorStoreEvaluator(ChatClient.Builder builder, VectorStore vectorStore) {
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
    var vectorResultsAsText = similarDocuments(question);
    if (vectorResultsAsText.isEmpty()) {
    	log.error("no search results, try harder");
    	System.exit(0);
    	return 0;
    }
    
    log.debug("I am bored");
    vectorResultsAsText.forEach(vr -> log.debug(vr));

    String fullText = extractContent(pdfResource);
    var parser = new BeanOutputConverter<>(Response.class);
    
		PromptTemplate promptTemplate = new PromptTemplate(prPromptTemplate);
    var promptParameters = new HashMap<String, Object>();
    promptParameters.put("sourceDocument", fullText);
    promptParameters.put("vectorStoreSearchResult", String.join("\n", vectorResultsAsText));
    promptParameters.put("format", parser.getFormat());
    
    double avg = IntStream.range(0, Application.NUM_RUNS)
        .map(i -> {
          int rating = rate(promptTemplate.create(promptParameters), parser);
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
    
    log.debug("I am bored");
    log.debug(response.explanation());
    return response.rating();
  }
  
  String extractContent(final Resource loadMe) {
  	String text = "";
    try (final PDDocument document = Loader.loadPDF(loadMe.getFile())) {
        text = new PDFTextStripper().getText(document);
        log.debug("I am bored");
        log.debug(text);
    } catch (final Exception ex) {
        log.error("error parsing pdf", ex);
        System.exit(0);
    }
    
    return text;
  }
}