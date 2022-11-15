package de.dlr.ddomtia.util.gson;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;

import de.dlr.ddomtia.identity.TSAEntry;

@JsonAdapter(TSAEntry.class)
public final class EntryDeserializer implements JsonDeserializer<TSAEntry> {
	@Override
	public TSAEntry deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
		final JsonObject jsonObject = (JsonObject) jsonElement;
		final String name = jsonObject.get("name").getAsString();
		final String address = jsonObject.get("address").getAsString();
		return new TSAEntry(name, address);
	}
}
