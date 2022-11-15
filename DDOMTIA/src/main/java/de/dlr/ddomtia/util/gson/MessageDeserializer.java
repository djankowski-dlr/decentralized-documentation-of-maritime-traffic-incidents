package de.dlr.ddomtia.util.gson;

import java.lang.reflect.Type;
import java.util.Set;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.reflect.TypeToken;

import de.dlr.ddomtia.Message;
import de.dlr.ddomtia.identity.TSAEntry;
import de.dlr.ddomtia.util.Util;

@JsonAdapter(Message.class)
public class MessageDeserializer implements JsonDeserializer<Message> {
	private static final Type token = new TypeToken<Set<TSAEntry>>() {}.getType();

	@Override
	public Message deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
		final JsonObject object = (JsonObject) jsonElement;
		final String identity = object.get("identity").getAsString();
		final int iteration = object.get("iteration").getAsInt();
		final int maxIteration = object.get("maxIteration").getAsInt();
		final Set<TSAEntry> entries = Util.gson.fromJson(object.get("entries").getAsJsonArray(), MessageDeserializer.token);
		return new Message(identity, iteration, maxIteration, entries);
	}
}
