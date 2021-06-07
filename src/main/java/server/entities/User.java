package server.entities;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table
@Data
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class User extends BaseEntity
{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "USER_NAME", unique = true, nullable = false)
    private String userName;


    @Column(name = "PASSWORD", nullable = false)
    private String password;

    @Column(name = "DISPLAY_NAME")
    private String displayName;


    @OneToMany(mappedBy = "receiver")
    private List<PrivateMessage> messages;

    @ManyToMany
    @JoinTable(name = "USER_GROUP",
            joinColumns = @JoinColumn(name = "GROUP_ID"),
            inverseJoinColumns = @JoinColumn(name = "USER_ID"))
    private List<Group> groups;



    @OneToMany(mappedBy = "owner")
    private List<FriendShip> friendships;

    public List<FriendShip> getFriendships()
    {
        if (friendships == null)
            this.friendships = new ArrayList<>();
        return friendships;
    }

    public List<Group> getGroups()
    {
        if(groups == null)  this.groups = new ArrayList<>();
        return groups;
    }
}
