package guru.bonacci.vectorstore.stores;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import guru.bonacci.vectorstore.VectorStoreEvaluator;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(name = "runner.active", havingValue = "pgvector")
public class PgVector extends VectorStoreEvaluator {

  private final JdbcClient jdbcClient;

  public PgVector(ChatClient.Builder builder, JdbcClient jdbcClient, VectorStore vectorStore) {
  	super(builder, vectorStore);
  	this.jdbcClient = jdbcClient;
  }


  @Override
  public void load() {
  	Integer docCount = jdbcClient.sql("select count(*) from vector_store")
        .query(Integer.class)
        .single();

		if (docCount > 0) {
			log.info("current count vector store: {}", docCount);
			return;
		}
		
    log.info("loading pdf into vector store");
    var config = PdfDocumentReaderConfig.builder()
            .withPageExtractedTextFormatter(new ExtractedTextFormatter.Builder().withNumberOfBottomTextLinesToDelete(0)
                    .withNumberOfTopPagesToSkipBeforeDelete(0)
                    .build())
            .withPagesPerDocument(1)
            .build();

    var pdfReader = new PagePdfDocumentReader(getPdfResource(), config);
    var textSplitter = new TokenTextSplitter();
    getVectorStore().accept(textSplitter.apply(pdfReader.get()));
    log.info("docs loaded");
  }
}