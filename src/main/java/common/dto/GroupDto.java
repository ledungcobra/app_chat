package common.dto;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Id;
import java.io.Serializable;

@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Getter
@Setter
public class GroupDto implements Serializable {
    @Id
    @EqualsAndHashCode.Include
    private Long id;

    private String name;

    @Override
    public String toString() {
        return name;
    }

    public GroupDto() {
    }

    public GroupDto(String name) {
        this.name = name;
    }
}
