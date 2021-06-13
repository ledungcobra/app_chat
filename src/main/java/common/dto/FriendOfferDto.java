package common.dto;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Getter
@Setter
public class FriendOfferDto implements Serializable
{
    @EqualsAndHashCode.Include
    private Long id;

    private String displayName;

    private Boolean accepted;

    @Override
    public String toString()
    {
        return this.displayName;
    }
}
