package team.suajung.ad.ress.auth.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "roles")
public class Role {

    @Id
    private String id;

    private ERole name;

    public enum ERole {
        ROLE_USER,
        ROLE_ADMIN
    }

    public String getRoleName() {
        return name != null ? name.name() : null;
    }
}

