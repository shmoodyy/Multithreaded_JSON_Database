package server;

import client.ClientRequest;
import com.google.gson.*;
import jsonDbUtil.ReadWriteUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Map;

class RequestHandler implements Runnable {

    private final Map<Object, Object> DATABASE;
//    final String DB_PATH = System.getProperty("user.dir") + "/JSON Database (Java)/task/src/server/data/db.json"; // for local
    final String DB_PATH = System.getProperty("user.dir") + "/src/server/data/db.json"; // for testing
    private final int PORT = 58543;
    final Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().create();
    final Gson GSON = new GsonBuilder().create();

    public RequestHandler(Map<Object, Object> database) {
        this.DATABASE = database;
    }

    @Override
    public void run() {
        while (true) {
            try (ServerSocket server = new ServerSocket(PORT)) {
                try (
                        Socket socket = server.accept();
                        DataInputStream input = new DataInputStream(socket.getInputStream());
                        DataOutputStream output = new DataOutputStream(socket.getOutputStream())
                ) {
                    String requestJson = input.readUTF();
                    System.out.println("Received: " + requestJson);
                    ClientRequest clientRequest = null;
                    String responseJson;
                    if (isValidJson(requestJson)) {
                        clientRequest = GSON.fromJson(requestJson, ClientRequest.class);
                        JsonObject serverResponse = processDB(DATABASE, clientRequest);
                        responseJson = "\n" + GSON.toJson(serverResponse);
                    } else {
                        responseJson = "\n" + GSON.toJson(errorResponse());
                    }
                    output.writeUTF(responseJson);
                    System.out.println("Sent: " + responseJson);
                    if (clientRequest != null && clientRequest.getType().equalsIgnoreCase("exit")) {
                        socket.close(); // Close the socket for the "exit" request
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isValidJson(String jsonString) {
        try {
            JsonParser.parseString(jsonString);
            return true;
        } catch (JsonSyntaxException e) {
            return false;
        }
    }

    private JsonObject processDB(Map<Object, Object> database, ClientRequest clientRequest) {
        JsonObject jsonObject = GSON.fromJson(ReadWriteUtil.readFromFile(DB_PATH), JsonObject.class);
        String type = clientRequest.getType();
        Object key = null;
        Object value = null;
        if (clientRequest.getKey() != null) key = clientRequest.getKey();
        if (clientRequest.getValue() != null) value = clientRequest.getValue();
        JsonObject responseJson = new JsonObject();
        if (!type.equals("exit")) {
            return switch (type) {
                case "set"      -> set(database, jsonObject, key, value, responseJson);
                case "get"      -> get(jsonObject, key, responseJson);
                case "delete"   -> delete(database, jsonObject, key, responseJson);
                default         -> throw new IllegalStateException("Unexpected value: " + type);
            };

        } else {
            responseJson.addProperty("response", "OK");
            return responseJson;
        }
    }

    public JsonObject set(Map<Object, Object> database, JsonObject jsonObject, Object key, Object value,
                              JsonObject responseJson) {
        JsonArray keyArray = key instanceof ArrayList<?> ? GSON.toJsonTree(key).getAsJsonArray() : null;
        JsonElement noFakeKeys = null;
        if (keyArray != null) {
            noFakeKeys = doAllKeysExist(jsonObject, keyArray);
        }
        if (keyArray != null && noFakeKeys == null) {
            return errorResponse();
        }
        database.put(key, value);
        if (keyArray != null) {
            JsonElement updated = updateValue(jsonObject, keyArray, String.valueOf(value), 0);
            ReadWriteUtil.writeToFile(DB_PATH, PRETTY_GSON.toJson(updated));
        } else {
            ReadWriteUtil.writeToFile(DB_PATH, PRETTY_GSON.toJson(database));
        }
        responseJson.addProperty("response", "OK");
        return responseJson;
    }

    public JsonElement updateValue(JsonElement jsonElement, JsonArray path, String newValue, int depth) {
        if (jsonElement.isJsonObject() && depth < path.size()) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            JsonElement nextElement = jsonObject.get(path.get(depth).getAsString());

            if (nextElement != null) {
                if (depth == path.size() - 1 && nextElement.isJsonPrimitive()) {
                    jsonObject.addProperty(path.get(depth).getAsString(), newValue);
                } else {
                    JsonElement updatedElement = updateValue(nextElement, path, newValue, depth + 1);
                    jsonObject.add(path.get(depth).getAsString(), updatedElement);
                }
            }
        }
        return jsonElement;
    }

    public JsonObject get(JsonObject jsonObject, Object key,
                          JsonObject responseJson) {
        JsonArray keyArray = key instanceof ArrayList<?> ? GSON.toJsonTree(key).getAsJsonArray() : null;
        JsonElement noFakeKeys = null;
        if (keyArray != null) {
            noFakeKeys = doAllKeysExist(jsonObject, keyArray);
        }
        if (noFakeKeys == null && !jsonObject.has(String.valueOf(key))) {
            return errorResponse();
        }
        responseJson.addProperty("response", "OK");
        responseJson.add("value", keyArray == null ? jsonObject.get(String.valueOf(key)) : noFakeKeys);
        return responseJson;
    }

    public JsonObject delete(Map<Object, Object> database, JsonObject jsonObject , Object key,
                             JsonObject responseJson) {
        JsonArray keyArray = key instanceof ArrayList<?> ? GSON.toJsonTree(key).getAsJsonArray() : null;
        JsonElement noFakeKeys = null;
        if (keyArray != null) {
            noFakeKeys = doAllKeysExist(jsonObject, keyArray);
        }
        if (keyArray != null && noFakeKeys == null) {
            return errorResponse();
        }
        database.remove(key);
        if (keyArray != null) {
            JsonElement deleted = deleteValue(jsonObject, keyArray, 0);
            ReadWriteUtil.writeToFile(DB_PATH, PRETTY_GSON.toJson(deleted));
        } else {
            ReadWriteUtil.writeToFile(DB_PATH, PRETTY_GSON.toJson(database));
        }
        responseJson.addProperty("response", "OK");
        return responseJson;
    }

    public JsonElement deleteValue(JsonElement jsonElement, JsonArray path, int depth) {
        if (jsonElement.isJsonObject() && depth < path.size()) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            JsonElement nextElement = jsonObject.get(path.get(depth).getAsString());

            if (nextElement != null) {
                if (depth == path.size() - 1) {
                    jsonObject.remove(path.get(depth).getAsString());
                } else {
                    JsonElement updatedElement = deleteValue(nextElement, path, depth + 1);
                    jsonObject.add(path.get(depth).getAsString(), updatedElement);
                }
            }
        }
        return jsonElement;
    }

    public JsonElement doAllKeysExist(JsonObject jsonObject, JsonArray jsonArray) {
        JsonElement found = null;
        for (JsonElement element : jsonArray) {
            found = doesKeyExist(jsonObject, element.getAsString());
            if (found == null) break;
            var nestedJson = jsonObject.get(element.getAsString());
            if (nestedJson != null && nestedJson.isJsonObject()) jsonObject = jsonObject.get(element.getAsString()).getAsJsonObject();
        }
        return found;
    }

    public JsonElement doesKeyExist(JsonObject jsonObject, String key) {
        if (jsonObject.has(key)) {
            return jsonObject.get(key);
        }
        for (var value : jsonObject.entrySet()) {
            var valueK = value.getKey();
            var valueV = value.getValue();
            if (valueK.equals(key)) {
                return jsonObject.get(valueK);
            } else if (valueV.isJsonArray()) {
                for (JsonElement arrayElement : valueV.getAsJsonArray()) {
                    if (arrayElement.getAsString().equals(key)) {
                        return jsonObject.get(arrayElement.getAsString());
                    }
                }
            }
        }
        return null;
    }

    private static JsonObject errorResponse() {
        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("response", "ERROR");
        responseJson.addProperty("reason", "No such key");
        return responseJson;
    }
}