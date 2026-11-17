package com.example.userUI;
import com.example.userUI.DistrictEntity;
import tools.jackson.databind.ObjectMapper;
import org.locationtech.jts.geom.Coordinate;
import java.util.List;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class GeoJsonConverter {

    private final ObjectMapper mapper;

    public List<CouriersEntity> parseCouriers(String json) throws Exception {
        return Arrays.asList(mapper.readValue(json, CouriersEntity[].class));
    }

    public List<Integer> parseCourierOrderIds(String json) throws Exception {
        return Arrays.asList(mapper.readValue(json, Integer[].class));
    }

    public GeoJsonConverter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public String convert(DistrictEntity district) throws Exception {

        List<double[]> points = new ArrayList<>();

        for (Coordinate coordinate : district.getPolygon().getExteriorRing().getCoordinates()) {
            points.add(new double[]{coordinate.x, coordinate.y});
        }

        Map<String, Object> geometry = Map.of("type", "Polygon", "coordinates", List.of(points));

        Map<String, Object> feature = Map.of("type", "Feature", "properties", Map.of("name", district.getName()),
                "geometry", geometry);

        return mapper.writeValueAsString(feature);
    }

    public String convert(OrderEntity order) throws Exception {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

        Map<String, Object> geometry = Map.of("type", "Point",
                "coordinates", new double[]{order.getCoords().getX(), order.getCoords().getY()
                }
        );

        Map<String, Object> properties = new HashMap<>();
        properties.put("id", order.getId());
        properties.put("urgency", order.getUrgency());
        properties.put("status", order.getStatus());
        properties.put("deadline", order.getDeadline() == null ? null : order.getDeadline().format(formatter));

        return mapper.writeValueAsString(Map.of("type", "Feature", "geometry", geometry, "properties", properties));
    }

    public String convertPath(String path) throws Exception {
        List<double[]> points = new ArrayList<>();

        String[] pairs = path.substring(2, path.length() - 2).split("\\],\\[");

        for (String pair : pairs) {
            String[] xy = pair.split(",");

            points.add(new double[]{
                    Double.parseDouble(xy[0]),
                    Double.parseDouble(xy[1])
            });
        }

        Map<String, Object> geometry = Map.of(
                "type", "LineString",
                "coordinates", points
        );

        return mapper.writeValueAsString(Map.of(
                "type", "Feature",
                "geometry", geometry,
                "properties", Map.of()
        ));
    }


}