package tecnico.encryption;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class MessageSerializer {

    private static final Gson gson = new GsonBuilder().create();

    private MessageSerializer() {}

    public static String toJson(Object object) {
        return gson.toJson(object);
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        return gson.fromJson(json, clazz);
    }

    public static <T> T cloneObject(T object, Class<T> clazz) {
        String json = toJson(object);
        return fromJson(json, clazz);
    }
}