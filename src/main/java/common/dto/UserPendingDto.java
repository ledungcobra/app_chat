package common.dto;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserPendingDto {
    @EqualsAndHashCode.Include
    private Long id;
    private Long userId;
    private String displayName;
    private Long groupId;

    @Override
    public String toString() {
        return displayName;
    }
}
