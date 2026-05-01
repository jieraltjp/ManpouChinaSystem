package com.manpou.allinone.qc.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "qc_image", indexes = {
        @Index(name = "idx_qc_record_id", columnList = "qc_record_id"),
        @Index(name = "idx_filename", columnList = "filename"),
        @Index(name = "idx_create_time", columnList = "create_time")
})
@Getter
@NoArgsConstructor
public class QcImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "qc_record_id")
    @Setter
    private Long qcRecordId;

    @Column(name = "filename", nullable = false)
    private String filename;

    @Column(name = "original_name", nullable = false)
    private String originalName;

    @Column(name = "url", nullable = false, length = 512)
    private String url;

    @Column(name = "size", nullable = false)
    private Long size;

    @Column(name = "mime_type", nullable = false, length = 64)
    private String mimeType;

    @Column(name = "uploaded_by")
    @Setter
    private Long uploadedBy;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by")
    private Long deletedBy;

    @PrePersist
    protected void onCreate() {
        this.createTime = LocalDateTime.now();
        this.isDeleted = false;
    }

    public QcImage(String filename, String originalName, String url, Long size, String mimeType) {
        this.filename = filename;
        this.originalName = originalName;
        this.url = url;
        this.size = size;
        this.mimeType = mimeType;
    }

    public void softDelete(Long deletedBy) {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedBy;
    }
}
