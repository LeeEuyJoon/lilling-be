package luti.server.infrastructure.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ClickCountBatchScheduler {

	private static final Logger log = LoggerFactory.getLogger(ClickCountBatchScheduler.class);

	private final JobLauncher jobLauncher;
	private final Job clickCountSyncJob;

	public ClickCountBatchScheduler(JobLauncher jobLauncher, Job clickCountSyncJob) {
		this.jobLauncher = jobLauncher;
		this.clickCountSyncJob = clickCountSyncJob;
	}

	@Scheduled(fixedDelay = 300000, initialDelay = 60000)
	public void runBatchJob() {
		try {
			JobParameters params = new JobParametersBuilder()
				.addLong("timestamp", System.currentTimeMillis())
				.toJobParameters();

			jobLauncher.run(clickCountSyncJob, params);

		} catch (Exception e) {
			log.error("배치 작업 실행 실패", e);
		}
	}
}
