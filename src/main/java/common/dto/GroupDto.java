package common.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Id;
import java.io.Serializable;

@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Data
public class GroupDto implements Serializable
{
    @Id
    @EqualsAndHashCode.Include
    private Long id;

    private String name;

}
