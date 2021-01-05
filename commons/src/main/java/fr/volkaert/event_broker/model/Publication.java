package fr.volkaert.event_broker.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Arrays;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
@Table(name="publication")
public class Publication {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    private String code;
    private String name;
    private String eventTypeCode;
    private boolean active;

    // Use either List<String> authClientIdsAsList = Arrays.asList(clientId1, clientId2, clientId3); String authClientIds = String.join(",", authClientIdsAsList);
    // or String[] authClientIdsAsArray = { clientId1, clientId2, clientId3 }; String authClientIds = String.join(",", authClientIdsAsArray);
    // In the opposite way, to convert the String to an Array, just do String[] authClientIdsAsArray = authClientIds.split(",");
    private String authClientIds; // Authent ClientIds (comma separated) that are allowed to publish using this Publication (if null, no check will be performed)
}
