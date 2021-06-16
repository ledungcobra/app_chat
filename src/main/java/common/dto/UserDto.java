package common.dto;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserDto implements Serializable
{
        @EqualsAndHashCode.Include
        private Long id;

        private String userName;
        private String displayName;

        public UserDto() {
        }

        public UserDto(Long id) {
                this.id = id;
        }

        public UserDto(Long id, String userName, String displayName) {
                this.id = id;
                this.userName = userName;
                this.displayName = displayName;
        }
}
