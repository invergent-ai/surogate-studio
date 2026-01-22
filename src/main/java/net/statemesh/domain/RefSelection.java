package net.statemesh.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

@Embeddable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefSelection implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "ref_id")
    private String id;

    @Column(name = "ref_type")
    private String type;


}
