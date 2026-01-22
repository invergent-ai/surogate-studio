package net.statemesh.service.criteria;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.IntegerFilter;
import tech.jhipster.service.filter.StringFilter;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Setter
@Getter
@ParameterObject
@NoArgsConstructor
public class ContainerCriteria implements Serializable, Criteria {
    @Serial
    private static final long serialVersionUID = 1L;

    private StringFilter imageName;
    private StringFilter type;
    private IntegerFilter cpuRequest;
    private IntegerFilter cpuLimit;
    private StringFilter memRequest;
    private StringFilter memLimit;
    private StringFilter pullImageMode;
    private StringFilter applicationId;

    // Your existing copy constructor
    public ContainerCriteria(ContainerCriteria other) {
        this.imageName = other.imageName == null ? null : other.imageName.copy();
        this.type = other.type == null ? null : other.type.copy();
        this.cpuRequest = other.cpuRequest == null ? null : other.cpuRequest.copy();
        this.cpuLimit = other.cpuLimit == null ? null : other.cpuLimit.copy();
        this.memRequest = other.memRequest == null ? null : other.memRequest.copy();
        this.memLimit = other.memLimit == null ? null : other.memLimit.copy();
        this.pullImageMode = other.pullImageMode == null ? null : other.pullImageMode.copy();
        this.applicationId = other.applicationId == null ? null : other.applicationId.copy();
    }

    @Override
    public ContainerCriteria copy() {
        return new ContainerCriteria(this);
    }

    public StringFilter imageName() {
        if (imageName == null) {
            imageName = new StringFilter();
        }
        return imageName;
    }

    public StringFilter type() {
        if (type == null) {
            type = new StringFilter();
        }
        return type;
    }


    public IntegerFilter cpuRequest() {
        if (cpuRequest == null) {
            cpuRequest = new IntegerFilter();
        }
        return cpuRequest;
    }

    public IntegerFilter cpuLimit() {
        if (cpuLimit == null) {
            cpuLimit = new IntegerFilter();
        }
        return cpuLimit;
    }

    public StringFilter memRequest() {
        if (memRequest == null) {
            memRequest = new StringFilter();
        }
        return memRequest;
    }

    public StringFilter memLimit() {
        if (memLimit == null) {
            memLimit = new StringFilter();
        }
        return memLimit;
    }


    public StringFilter pullImageMode() {
        if (pullImageMode == null) {
            pullImageMode = new StringFilter();
        }
        return pullImageMode;
    }

    public StringFilter applicationId() {
        if (applicationId == null) {
            applicationId = new StringFilter();
        }
        return applicationId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContainerCriteria that = (ContainerCriteria) o;
        return Objects.equals(imageName, that.imageName) &&
            Objects.equals(type, that.type) &&
            Objects.equals(cpuRequest, that.cpuRequest) &&
            Objects.equals(cpuLimit, that.cpuLimit) &&
            Objects.equals(memRequest, that.memRequest) &&
            Objects.equals(memLimit, that.memLimit) &&
            Objects.equals(pullImageMode, that.pullImageMode) &&
            Objects.equals(applicationId, that.applicationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            imageName,
            type,
            cpuRequest,
            cpuLimit,
            memRequest,
            memLimit,
            pullImageMode,
            applicationId
        );
    }

    @Override
    public String toString() {
        return "ContainerCriteria{" +
            "imageName=" + imageName +
            ", type=" + type +
            ", cpuRequest=" + cpuRequest +
            ", cpuLimit=" + cpuLimit +
            ", memRequest=" + memRequest +
            ", memLimit=" + memLimit +
            ", pullImageMode=" + pullImageMode +
            ", applicationId=" + applicationId +
            '}';
    }
}
