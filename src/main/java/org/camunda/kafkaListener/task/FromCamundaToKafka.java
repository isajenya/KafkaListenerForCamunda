package org.camunda.kafkaListener.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.admin.NewTopic;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.camunda.kafkaListener.ProcessEnv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@ExternalTaskSubscription("sendKafkaRecord")
public class FromCamundaToKafka implements ExternalTaskHandler {

	private final static Logger LOG = LoggerFactory.getLogger(FromCamundaToKafka.class);

	@Autowired
	private NewTopic kafkaTopic;

	@Autowired
	private KafkaTemplate<String, String> kafka;

	@Autowired
	private ObjectMapper objectMapper;

	public void sendRecordToKafka(Map<String, Object> variables) throws JsonProcessingException {
		String dataAsJson = objectMapper.writeValueAsString(variables);

		kafka.send(kafkaTopic.name(), dataAsJson).addCallback(
				result -> {
					if (result != null) {
						LOG.info("Produced record: " + result.getRecordMetadata());
					}
				},
				exception -> LOG.error("Failed to produce to kafka", exception));
	}

	@Override
	public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		ProcessEnv processEnv = new ProcessEnv(externalTask, externalTaskService);

		String correlationIdInKafkaRecord = UUID.randomUUID().toString();
		processEnv.setCorrelationId(correlationIdInKafkaRecord);

		try {
			sendRecordToKafka(externalTask.getAllVariables());
		} catch (JsonProcessingException exception) {
			processEnv.handleTechnicalError(exception);
		}
	}
}
