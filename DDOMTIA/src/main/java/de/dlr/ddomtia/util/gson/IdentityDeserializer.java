package de.dlr.ddomtia.util.gson;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.reflect.TypeToken;

import de.dlr.ddomtia.identity.Identity;
import de.dlr.ddomtia.identity.Register;
import de.dlr.ddomtia.identity.TSAEntry;
import de.dlr.ddomtia.util.Util;

@JsonAdapter(Identity.class)
public final class IdentityDeserializer implements JsonDeserializer<Identity> {
	private static final Type token = new TypeToken<Map<Integer, Set<TSAEntry>>>() {}.getType();
	@Override
	public Identity deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
		final JsonObject object = (JsonObject) jsonElement;
		final String identityString = object.get("identityName").getAsString();
		final Register register = Util.gson.fromJson(object.get("register"), Register.class);
		final int iterations;
		if (object.has("iterations")) {
			iterations = object.get("iterations").getAsInt();
		} else {
			iterations = 0;
		}
		if (object.has("iterationMap")) {
			final JsonObject iterationMapString = object.get("iterationMap").getAsJsonObject();
			final Map<Integer, Set<TSAEntry>> iterationMap = Util.gson.fromJson(iterationMapString, IdentityDeserializer.token);
			return new Identity(identityString, register, iterationMap, iterations);
		}
		return new Identity(identityString, register, new HashMap<>(), iterations);
	}
}
