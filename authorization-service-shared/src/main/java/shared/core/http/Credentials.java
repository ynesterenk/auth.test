package shared.core.http;

import lombok.Data;

@Data
public class Credentials {
    // Constructor with username and password parameters
    public Credentials(String username, String password) {
        this.username = username;
        this.password = password;
    }
    private final String username;
    private final String password;

}
