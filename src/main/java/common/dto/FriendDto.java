package common.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Data
@EqualsAndHashCode
public class FriendDto implements Serializable
{
        @EqualsAndHashCode.Include
        private Long id;

        private String displayName;
}
