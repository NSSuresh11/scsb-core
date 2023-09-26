package org.recap.model.jpa;

import lombok.Data;


import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.Table;
import java.util.List;

/**
 * Created by pvsubrah on 6/11/16.
 */
@Data
@Entity
@Table(name = "holdings_t", catalog = "")
@AttributeOverride(name = "id", column = @Column(name = "HOLDINGS_ID"))
@NamedNativeQuery(
        name = "HoldingsEntity.getNonDeletedItemEntities",
        query = "SELECT ITEM_T.* FROM ITEM_T, ITEM_HOLDINGS_T, HOLDINGS_T WHERE " +
                "HOLDINGS_T.HOLDINGS_ID = ITEM_HOLDINGS_T.HOLDINGS_ID AND ITEM_T.ITEM_ID = ITEM_HOLDINGS_T.ITEM_ID " +
                "AND ITEM_T.IS_DELETED = 0 AND " +
                "HOLDINGS_T.OWNING_INST_HOLDINGS_ID = :owningInstitutionHoldingsId AND HOLDINGS_T.OWNING_INST_ID = :owningInstitutionId",
        resultClass = ItemEntity.class)

public class HoldingsEntity extends HoldingsAbstractEntity {


    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "OWNING_INST_ID", insertable = false, updatable = false)
    private InstitutionEntity institutionEntity;

    @ManyToMany(mappedBy = "holdingsEntities")
    private List<BibliographicEntity> bibliographicEntities;

    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "item_holdings_t", joinColumns = {
            @JoinColumn(name = "HOLDINGS_ID", referencedColumnName = "HOLDINGS_ID")},
            inverseJoinColumns = {
                    @JoinColumn(name = "ITEM_ID", referencedColumnName = "ITEM_ID")})
    private List<ItemEntity> itemEntities;

    /**
     * Instantiates a new Holdings entity.
     */
    public HoldingsEntity() {
        super();
    }
}
