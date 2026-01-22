package net.statemesh.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serial;
import java.io.Serializable;

/**
 * App Templates.
 */
@Entity
@Table(name = "app_template")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class AppTemplate implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private String id;

    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    @NotNull
    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "long_description" , columnDefinition = "text")
    private String longDescription;

    @Column(name = "icon")
    private String icon;

    @Column(name = "template", columnDefinition = "text")
    private String template;

    @NotNull
    @Size(max = 50)
    @Column(name = "category", nullable = false)
    private String category;

    @Column(name = "zorder")
    private Integer zorder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id")
    private Provider provider;

    @Column(name = "hashtags")
    private String hashtags;
}
