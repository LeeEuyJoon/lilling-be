package luti.server.infrastructure.batch.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import luti.server.infrastructure.batch.ClickCountDatabaseWriter;
import luti.server.infrastructure.batch.ClickCountRedisReader;
import luti.server.infrastructure.batch.dto.ClickCountData;

@Configuration
public class ClickCountBatchConfig {

	@Bean
	public Job clickCountSyncJob(
		JobRepository jobRepository,
		Step clickCountSyncStep
	) {
		return new JobBuilder("clickCountSyncJob", jobRepository)
			.start(clickCountSyncStep)
			.build();
	}

	@Bean
	public Step clickCountSyncStep(
		JobRepository jobRepository,
		PlatformTransactionManager transactionManager,
		ClickCountRedisReader reader,
		ClickCountDatabaseWriter writer
	) {
		return new StepBuilder("clickCountSyncStep", jobRepository)
			.<ClickCountData, ClickCountData>chunk(100, transactionManager)
			.reader(reader)
			.writer(writer)
			.build();
	}
}
