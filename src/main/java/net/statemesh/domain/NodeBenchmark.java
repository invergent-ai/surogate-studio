package net.statemesh.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "node_benchmark")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"nodeReservation"})
public class NodeBenchmark implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private String id;

    @NotNull
    @Column(name = "created", nullable = false)
    private Instant created;

    @Column(name = "updated")
    private Instant updated;

    @Column(name = "rxMbps")
    Integer rxMbps;

    @Column(name = "txMbps")
    Integer txMbps;

    @Column(name = "free_disk_bytes")
    Long freeDiskBytes;

    @Column(name = "available_mem_bytes")
    Long availableMemBytes;

    @Column(name = "public_ip")
    String publicIp;

    @Column(name = "cpu_ghz")
    Double cpuGhz;

    @Column(name = "cpu_brand_name")
    String cpuBrandName;

    @Column(name = "cpu_vendor_id")
    String cpuVendorId;

    @Column(name = "cpu_hypervisor_vendor_id")
    String cpuHypervisorVendorId;

    @Column(name = "cpu_physical_cores")
    Integer cpuPhysicalCores;

    @Column(name = "cpu_logical_cores")
    Integer cpuLogicalCores;

    @Column(name = "cpu_threads")
    Integer cpuThreadsPerCore;

    @Column(name = "cpu_family")
    Integer cpuFamily;

    @Column(name = "cpu_model")
    Integer cpuModel;

    @Column(name = "cpu_stepping")
    Integer cpuStepping;

    @Column(name = "cpu_features", length = 2048)
    String cpuFeatures;

    @Column(name = "bench_num_cores")
    Integer benchNumCores;

    @Column(name = "bench_real_cores")
    Integer benchRealCores;

    @Column(name = "bench_total_ops")
    Double benchTotalOps;

    @Column(name = "bench_core_ops", length = 4096)
    String benchCoreOps;

    @ManyToOne
    @JsonIgnoreProperties(value = { "user", "node" }, allowSetters = true)
    private NodeReservation nodeReservation;
}
