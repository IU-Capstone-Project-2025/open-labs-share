package olsh.backend.api_gateway.service;

import com.google.protobuf.Timestamp;

public class TimestampConverter {

    /**
     * Converts a protobuf Timestamp to an ISO 8601 formatted string.
     *
     * @param timestamp The protobuf Timestamp to convert
     * @return ISO 8601 formatted string or null if the input is null
     */
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
