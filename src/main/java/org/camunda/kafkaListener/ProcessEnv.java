package org.camunda.kafkaListener;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ProcessEnv {
	private static final long ONE_MINUTE = 1000L * 60;
	private static final int MAX_RETRIES = 3;

	private static final String CORRELATION_ID = "correlationId";

	private final Map<String, Object> updatedVariables = new HashMap<>();
	private final ExternalTask externalTask;
	private final ExternalTaskService externalTaskService;

	public ProcessEnv(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		this.externalTask = externalTask;
		this.externalTaskService = externalTaskService;
	}

	public String getCorrelationId() {
		return (String) getVariable(CORRELATION_ID);
	}

	public void setCorrelationId(String correlationId) {
		updatedVariables.put(CORRELATION_ID, correlationId);
	}

	public void handleTechnicalError(Exception e) {
		log.error("{}: {}", e.getClass().getSimpleName(), e.getMessage(), e);
		Integer retries = this.getRetries(externalTask);
		Long timeout = this.getNextTimeout(retries);
		externalTaskService.handleFailure(
				externalTask, e.getMessage(),
				ExceptionUtils.getStackTrace(e),
				retries, timeout);
	}

	private Integer getRetries(ExternalTask task) {
		Integer retries = task.getRetries();
		if (retries == null) {
			retries = MAX_RETRIES;
		} else {
			retries = retries - 1;
		}
		return retries;
	}

	private Long getNextTimeout(Integer retries) {
		// increasing interval: 1 additional minute delay after each retry
		return ONE_MINUTE * (MAX_RETRIES - retries);
	}

	public void complete() {
		externalTaskService.complete(externalTask, updatedVariables);
	}

	private Object getVariable(String variableName) {
		if (updatedVariables.containsKey(variableName)) {
			return updatedVariables.get(variableName);
		} else {
			return externalTask.getVariable(variableName);
		}
	}
}
