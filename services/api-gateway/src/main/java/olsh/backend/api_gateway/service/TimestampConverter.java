package olsh.backend.api_gateway.service;

import com.google.protobuf.Timestamp;

public class TimestampConverter {

    public static String convertTimestampToIso(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }

        try {
            java.time.Instant instant = java.time.Instant.ofEpochSecond(
                    timestamp.getSeconds(), timestamp.getNanos());
            return instant.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
