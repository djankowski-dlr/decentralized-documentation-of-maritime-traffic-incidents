package de.dlr.ddomtia.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import de.dlr.ddomtia.Message;
import de.dlr.ddomtia.identity.Identity;
import de.dlr.ddomtia.identity.Register;
import de.dlr.ddomtia.identity.TSAEntry;
import de.dlr.ddomtia.util.gson.IdentityDeserializer;
import de.dlr.ddomtia.util.gson.MessageDeserializer;
import de.dlr.ddomtia.util.gson.RegisterDeserializer;
import de.dlr.ddomtia.util.gson.EntryDeserializer;

import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;

@Log4j2
@UtilityClass
public final class Util {
    public static final Gson gson = new GsonBuilder().serializeNulls().disableHtmlEscaping().setPrettyPrinting()
            .registerTypeAdapter(Identity.class, new IdentityDeserializer())
            .registerTypeAdapter(Register.class, new RegisterDeserializer())
            .registerTypeAdapter(TSAEntry.class, new EntryDeserializer())
            .registerTypeAdapter(Message.class, new MessageDeserializer())
            .create();

    public static <T> T loadDataSources(String path, Class<T> clazz) {
        return Util.gson.fromJson(Util.loadAsString(path), clazz);
    }

    private static String loadAsString(final String path) {
        try {
            final File resource = new ClassPathResource(path).getFile();
            return Files.readString(resource.toPath());
        }catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void writeToFile(String path, String content) throws IOException {
        Files.writeString(new ClassPathResource(path).getFile().toPath(), content);
    }

    public static String sendHTTPRequest(RestOperations restTemplate, String url, String jsonString, HttpMethod httpMethod) throws RestClientException {
        final HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        final ResponseEntity<String> resp = restTemplate.exchange(url, httpMethod, new HttpEntity<>(jsonString, httpHeaders), String.class);
        return resp.getBody();
    }

    public static <T> Set<Set<T>> getPowerSetOfIteration(Collection<Set<T>> base, int size) {
        return base.stream().filter(set -> set.size() == size).collect(Collectors.toCollection(HashSet::new));
    }
}
