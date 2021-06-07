package server.entities;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity(name = "VIDEO_CALL")
@Table
@Getter
@Setter
public class VideoCall extends Call
{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(name = "RECEIVER_ID")
    @ManyToOne
    private User videoReceiver;

}
