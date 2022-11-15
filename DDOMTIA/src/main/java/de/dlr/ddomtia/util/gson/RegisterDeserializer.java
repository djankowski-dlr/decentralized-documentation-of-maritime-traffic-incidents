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

import de.dlr.ddomtia.identity.Extended;
import de.dlr.ddomtia.identity.Identity;
import de.dlr.ddomtia.identity.Register;
import de.dlr.ddomtia.identity.TSAEntry;
import de.dlr.ddomtia.util.Util;

@JsonAdapter(Register.class)
public final class RegisterDeserializer implements JsonDeserializer<Register> {
	@Override
	public Register deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
		final JsonObject jsonObject = (JsonObject) jsonElement;
		final Type tsaToken = new TypeToken<Set<TSAEntry>>() {}.getType();
		final Type identityToken = new TypeToken<Set<Identity>>() {}.getType();
		final Set<TSAEntry> basis = Util.gson.fromJson(jsonObject.get("basic"), tsaToken);
		final Set<TSAEntry> tsa = Util.gson.fromJson(jsonObject.get("extended").getAsJsonObject().get("tsa"), tsaToken);
		final Set<Identity> identity = Util.gson.fromJson(jsonObject.get("extended").getAsJsonObject().get("identities"), identityToken);
		return new Register(basis, new Extended(tsa, identity));
	}
}
