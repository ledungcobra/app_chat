package common.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Data
@EqualsAndHashCode
public class UserDto implements Serializable
{
        @EqualsAndHashCode.Include
        private Long id;

        private String userName;
        private String displayName;

}
