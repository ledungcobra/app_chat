package common.dto;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserPendingDto implements Serializable {
    @EqualsAndHashCode.Include
    private Long id;
    private Long userId;
    private String displayName;
    private Long groupId;

    @Override
    public String toString() {
        return displayName;
    }

    public UserPendingDto(Long id, Long userId, String displayName, Long groupId) {
        this.id = id;
        this.userId = userId;
        this.displayName = displayName;
        this.groupId = groupId;
    }

    public UserPendingDto() {
    }
}
