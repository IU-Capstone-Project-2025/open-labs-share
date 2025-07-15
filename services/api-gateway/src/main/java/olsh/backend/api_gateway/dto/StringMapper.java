package olsh.backend.api_gateway.dto;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class StringMapper {

    public static <T> List<T> mapToList(String str, Function<String, T> parser) {
        if (str == null || str.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String cleanStr = str.replaceAll("[\\[\\]]", "").trim();
        if (cleanStr.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(cleanStr.split(","))
                .map(String::trim)
                .map(parser)
                .toList();
    }

    public static List<Long> mapToLongList(String str) {
        return mapToList(str, Long::parseLong);
    }

    public static List<Integer> mapToIntegerList(String str) {
        return mapToList(str, Integer::parseInt);
    }


}
