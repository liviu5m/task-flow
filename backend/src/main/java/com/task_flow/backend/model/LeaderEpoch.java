package com.task_flow.backend.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "leader_epoch")
@Data
public class LeaderEpoch {
    @Id
    private Long id = 1L; 

    private long epoch;
}
