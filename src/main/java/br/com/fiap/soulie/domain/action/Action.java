package br.com.fiap.soulie.domain.action;


import br.com.fiap.soulie.domain.action.dto.ActionRequest;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "Action")
@Table(name = "actions")
public class Action {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;
    @Column(nullable = false, length = 255)
    private String description;
    @Column(nullable = false)
    private int points;

    public Action(ActionRequest actionRequest){
        this.name = actionRequest.name();
        this.description = actionRequest.description();
        this.points = actionRequest.points();
    }
}
