package common.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserAuthDto implements Serializable
{



    @EqualsAndHashCode.Include
    private Long id;

    private String userName;
    private String displayName;
    private String password;

    public UserAuthDto() {
    }

}
