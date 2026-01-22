package luti.server.infrastructure.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
public class ClickCountBatchScheduler {

	private static final Logger log = LoggerFactory.getLogger(ClickCountBatchScheduler.class);

	private final JobLauncher jobLauncher;
	private final JobRegistry jobRegistry;

	public ClickCountBatchScheduler(JobLauncher jobLauncher, JobRegistry jobRegistry) {
		this.jobLauncher = jobLauncher;
		this.jobRegistry = jobRegistry;
	}

	@Scheduled(fixedDelayString = "${batch.schedule.click-count-sync.fixed-delay}", initialDelayString = "${batch.schedule.click-count-sync.initial-delay}")
	public void runBatchJob() {
		try {
			log.info("배치 작업 실행 시작");
			JobParameters params = new JobParametersBuilder()
				.addLong("timestamp", System.currentTimeMillis())
				.toJobParameters();

			jobLauncher.run(jobRegistry.getJob("clickCountSyncJob"), params);
			log.info("배치 작업 실행 완료");

		} catch (Exception e) {
			log.error("배치 작업 실행 실패", e);
		}
	}
}
