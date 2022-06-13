package au.twc.core.product.domain;

import lombok.Data;

@Data
public class AuthToken {
    private String access_token;
    private int expires_in;
    private int refresh_expires_in;
    private String token_type;

    private String scope;
}
