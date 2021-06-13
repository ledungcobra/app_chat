package server.entities;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.*;

@Entity
@Table
@Getter
@Setter
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

    // Notification sent to another people
    @OneToMany(mappedBy = "fromUser", cascade = {CascadeType.ALL})
    private List<Notification> sentNotifications;

    // Notification receive from another people
    @OneToMany(mappedBy = "receiveUser", cascade = {CascadeType.ALL})
    private List<Notification> receiveNotifications;


    @OneToMany(mappedBy = "owner", cascade = {CascadeType.ALL})
    private Set<FriendShip> friendships;

    public List<Group> getGroups()
    {
        if (groups == null) this.groups = new ArrayList<>();
        return groups;
    }

    public void addFriendship(Collection<FriendShip> friendships)
    {
        this.friendships.addAll(friendships);
        friendships.forEach(f -> {
            f.setOwner(this);
        });
    }

    public void sendAnNotification(Notification notification)
    {
        notification.setFromUser(this);
        notification.setIsSeen(false);
    }

    @Override
    public String toString()
    {
        return "User{" +
                "id=" + id +
                '}';
    }
}
