package org.recap.model.jpa;

import lombok.Data;
import lombok.EqualsAndHashCode;


import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Created by pvsubrah on 6/11/16.
 */
@Data
@EqualsAndHashCode(callSuper=false)
@Entity
@Table(name = "request_item_t", catalog = "")
@AttributeOverride(name = "id", column = @Column(name = "REQUEST_ID"))
public class RequestItemEntity extends RequestItemAbstractEntity {

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "ITEM_ID", referencedColumnName = "ITEM_ID", insertable = false, updatable = false)
    private ItemEntity itemEntity;

    @Column(name = "EMAIL_ID")
    private String emailId;

    @ManyToOne(cascade = CascadeType.MERGE)
    @JoinTable(name = "bulk_request_t",
            joinColumns = @JoinColumn(name = "REQUEST_ID"),
            inverseJoinColumns = @JoinColumn(name = "BULK_REQUEST_ID"))
    private BulkRequestItemEntity bulkRequestItemEntity;

}
