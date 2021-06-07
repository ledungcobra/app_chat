package common.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Id;

@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Data
public class GroupDto
{
    @Id
    @EqualsAndHashCode.Include
    private Long id;

    private String name;

}
