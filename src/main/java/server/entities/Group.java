package server.entities;

import lombok.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "GROUP_CHAT")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class Group extends BaseEntity {
    @Id
    @EqualsAndHashCode.Include
    private Long id;
    private String name;

    @ManyToMany
    @JoinTable(name = "USER_GROUP",
            joinColumns = @JoinColumn(name = "GROUP_ID"),
            inverseJoinColumns = @JoinColumn(name = "USER_ID"))
    private List<User> members = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "USER_GROUP_ADMIN",
            joinColumns = @JoinColumn(name = "GROUP_ID"),
            inverseJoinColumns = @JoinColumn(name = "USER_ID"))
    private List<User> admins = new ArrayList<>();

}
