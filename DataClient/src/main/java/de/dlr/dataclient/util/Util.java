package de.dlr.dataclient.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class Util {
    public static final Gson GSON = new GsonBuilder().serializeNulls()
            .setPrettyPrinting().disableHtmlEscaping().create();

    private Util() {
    }
}
