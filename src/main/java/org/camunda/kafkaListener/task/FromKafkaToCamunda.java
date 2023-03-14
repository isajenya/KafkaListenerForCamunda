package org.camunda.kafkaListener.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.kafkaListener.configuration.KafkaTopicConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class FromKafkaToCamunda {

	private final static Logger LOG = LoggerFactory.getLogger(FromKafkaToCamunda.class);

	@Autowired
	private RuntimeService runtimeService;

	@KafkaListener(topics = KafkaTopicConfiguration.TOPIC_NAME, groupId = "groupId")
	public void processMessage(String content) throws JsonProcessingException {
		/*LOG.info("Received record: " + content);

		JsonNode jsonContent = new ObjectMapper().readTree(content);
		if (!jsonContent.hasNonNull("correlationId")) {
			throw new RuntimeException("Could not correlation record to process instance, field 'correlationId' is missing in record: " + content);
		}
		String correlationId = jsonContent.get("correlationId").asText();

		LOG.info("Correlate to process instance using correlationId: " + correlationId);*/
		runtimeService.createMessageCorrelation("MsgKafkaRecordReceived")
				//.correlationKey(correlationId)
				.correlate();
				/*.exceptionally(t -> {
					throw new RuntimeException("Could not hand over record to Zeebe: "+content+". check nested exception for details: " + t.getMessage());
				});*/

	}

}
