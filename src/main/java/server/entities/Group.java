package server.entities;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "GROUP_CHAT")
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class Group extends BaseEntity
{
    @Id
    @EqualsAndHashCode.Include
    private Long id;
    private String name;

    @ManyToMany
    @JoinTable(name = "USER_GROUP",
            joinColumns = @JoinColumn(name = "GROUP_ID"),
            inverseJoinColumns = @JoinColumn(name = "USER_ID"))
    private List<User> members;

}
