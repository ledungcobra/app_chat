package server.entities;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Getter
@Setter
@Entity
@Table(name = "FRIEND_OFFER")
public class FriendOffer extends BaseEntity
{

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "OWNER_ID")
    private User owner;

    @ManyToOne
    @JoinColumn(name = "PARTNER")
    private User partner;

    @Column(name = "DISPLAY_NAME")
    private String displayName;

    @Column(name = "ACCEPTED")
    private Boolean accepted;
}
