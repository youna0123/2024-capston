package team.suajung.ad.ress.auth.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashSet;
import java.util.Set;

@Data
@Document(collection = "users")
public class User {

    @Id
    private String id;

    private String username;
    private String email;
    private String password;
    private boolean enabled = false;
    private Set<Role> roles = new HashSet<>();

    private long createdAt;
    private long updatedAt;
}

