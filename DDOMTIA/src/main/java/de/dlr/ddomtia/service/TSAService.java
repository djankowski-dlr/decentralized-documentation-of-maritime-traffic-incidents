package de.dlr.ddomtia.service;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.sql.Timestamp;

import com.google.common.hash.Hashing;

import de.dlr.ddomtia.dto.TSADocument;
import de.dlr.ddomtia.event.TSAEvent;

import lombok.extern.log4j.Log4j2;

import org.bouncycastle.tsp.TSPAlgorithms;
import org.bouncycastle.tsp.TSPException;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.tsp.TimeStampResponse;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.InputStreamSource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Log4j2
@Service
public final class TSAService implements ApplicationListener<TSAEvent> {
	private static final String FILE_NAME_PREFIX = "plain-data-";
	private final EncryptionService encryptionService;
	private final RestTemplate restTemplate;
	private final MongoDBService mongoDBService;
	private String tsa;

	@Autowired
	public TSAService(EncryptionService encryptionService, RestTemplate restTemplate, MongoDBService mongoDBService, @Value("${tsa.fallback}") String tsa) {
		this.encryptionService = encryptionService;
		this.restTemplate = restTemplate;
		this.mongoDBService = mongoDBService;
		this.tsa = tsa;
	}

	public ResponseEntity<String> document(InputStreamSource file, String name) throws IOException {
		// Read in data file.
		final byte[] dataBytes = file.getInputStream().readAllBytes();
		log.debug("Input data was read. ({} bytes)", dataBytes.length);
		// Hash Message.
		final byte[] hashedDataBytes = Hashing.sha512().hashBytes(dataBytes).asBytes();
		log.debug("Input data has been hashed.");
		// Encrypt data from data file with my private key.
		final byte[] keyMessage;
		try {
			keyMessage = this.encryptionService.sign(hashedDataBytes);
			log.debug("Hash of Data was encrypted.");
		} catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
			log.error("Error with encrypting the hashed data.", e);
			return ResponseEntity.internalServerError().body("Error with encrypting the hashed data");
		}
		// Create TSQ hashed content.
		final byte[] tsaContent = Hashing.sha256().hashBytes(TSAService.createTSAContentRequest(hashedDataBytes, keyMessage)).asBytes();

		final TimeStampRequestGenerator gen = new TimeStampRequestGenerator();
		gen.setCertReq(true);

		final BigInteger nonce = BigInteger.valueOf(System.currentTimeMillis());
		// Create Timestamp Request.
		final TimeStampRequest timeStampRequest = gen.generate(TSPAlgorithms.SHA256, tsaContent, nonce);
		final byte[] tsq = timeStampRequest.getEncoded();
		// Create HTTP Client.
		final SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setBufferRequestBody(false);
		this.restTemplate.setRequestFactory(requestFactory);
		final HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.valueOf("application/timestamp-query"));
		final HttpEntity<byte[]> requestEntity = new HttpEntity<>(tsq, httpHeaders);
		// Send TSQ to TSA.
		final ResponseEntity<byte[]> response = this.restTemplate.exchange(this.tsa, HttpMethod.POST, requestEntity, byte[].class);
		log.debug("Send TSQ Request.");
		if (response.getBody() == null){
			log.warn("TSA response body is null.");
			return ResponseEntity.internalServerError().body("No TSR Received");
		}
		final byte[] tsr = response.getBody();
		// Add TSQ and TSR to MongoDB together with metadata.
		final Timestamp timestamp = new Timestamp(System.currentTimeMillis());

		this.mongoDBService.addTSADocument(new TSADocument(timestamp.toString(), tsq, tsr, ""));
		log.debug("Added Document with TSQ and TSR to MongoDB.");
		this.mongoDBService.addBytesToMongoDB(TSAService.FILE_NAME_PREFIX + name, dataBytes);
		log.info("Documentation completed");
		return ResponseEntity.ok("Documentation completed");
	}

	public ResponseEntity<String> validate(InputStreamSource tsq, InputStreamSource tsr) {
		try {
			final TimeStampRequest timeStampRequest = new TimeStampRequest(tsq.getInputStream());
			final TimeStampResponse timeStampResponse = new TimeStampResponse(tsr.getInputStream());
			timeStampResponse.validate(timeStampRequest);
			log.info("Data was not manipulated.");
			return ResponseEntity.ok("Data was not manipulated.");
		} catch (TSPException | IOException e) {
			log.info("Data seems to be manipulated.", e);
			return ResponseEntity.ok("Data seems to be manipulated.");
		}
	}

	private static byte[] createTSAContentRequest(byte[] hash, byte[] signature) {
		final JSONObject data = new JSONObject();
		data.append("hash", hash);
		data.append("signature", signature);
		return data.toString().getBytes(StandardCharsets.UTF_8);
	}

	@Override
	public void onApplicationEvent(TSAEvent event) {
		this.tsa = event.getTsaEntry().address();
	}
}
