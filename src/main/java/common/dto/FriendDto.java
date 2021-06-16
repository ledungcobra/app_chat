package common.dto;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class FriendDto implements Serializable
{

    public FriendDto() {
    }

    @Builder
    public FriendDto(Long id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    @EqualsAndHashCode.Include
    private Long id;

    private String displayName;

    public String toString()
    {
        return this.displayName;
    }
}
