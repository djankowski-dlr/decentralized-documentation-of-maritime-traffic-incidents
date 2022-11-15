package de.dlr.dataclient.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import de.dlr.dataclient.dto.AISDataDTO;
import de.dlr.dataclient.repository.AISDataRepository;
import de.dlr.dataclient.util.Util;

import lombok.extern.log4j.Log4j2;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public final class TimeRunnable {

	private final AISDataRepository aisDataRepository;
	private final String url;
	private final CloseableHttpClient httpClient;
//	private final List<Long> times;

	public TimeRunnable(AISDataRepository aisDataRepository, @Value("${ddomti.server.url}") String url) {
		this.httpClient = HttpClients.createDefault();
		this.aisDataRepository = aisDataRepository;
		this.url = url;
//		this.times = new ArrayList<>();
	}

	@Scheduled(fixedDelay = 1, timeUnit = TimeUnit.SECONDS)
	public void run() {
		log.debug("Start sending data from database.");

		final int offset = new Random(10).nextInt(2_000_000);
		final List<AISDataDTO> dataDTOList = this.aisDataRepository.getData(10, offset);
		final String tmp = Util.GSON.toJson(dataDTOList);

		final Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		final HttpEntity httpEntity = MultipartEntityBuilder.create().addBinaryBody("file", tmp.getBytes(StandardCharsets.UTF_8), ContentType.DEFAULT_BINARY, "data-" + timestamp).addTextBody("name", "data-" + timestamp, ContentType.TEXT_PLAIN).build();

		final HttpPost httppost = new HttpPost(this.url);
		httppost.setEntity(httpEntity);
		final long start = System.currentTimeMillis();
		try (InputStream inputStream = this.httpClient.execute(httppost).getEntity().getContent()) {
			log.debug("Receive response from server: {}", new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
//			this.times.add(System.currentTimeMillis() - start);
//			log.info("Average: {} Count: {}",this.times.stream().mapToDouble(Long::doubleValue).average().getAsDouble(), this.times.size());
		} catch (IOException e) {
			log.error(e);
			throw new RuntimeException(e);
		}
	}
}
