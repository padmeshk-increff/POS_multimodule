package com.increff.pos.entity;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Version;
import java.time.ZonedDateTime;

@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity {

    @CreationTimestamp
    @Column(nullable = false,updatable = false)
    protected ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    protected ZonedDateTime updatedAt;

    @Version
    @Column(nullable = false)
    protected Integer version;

}
