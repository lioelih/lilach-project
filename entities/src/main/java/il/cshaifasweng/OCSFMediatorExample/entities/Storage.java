package il.cshaifasweng.OCSFMediatorExample.entities;

import jakarta.persistence.*;

import java.io.Serializable;

/**
 * Maps the `storage` table
 *   storage_id | product_id | branch_id | quantity
 */
@Entity
@Table(name = "storage",
        uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "branch_id"}))
public class Storage implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "storage_id")
    private int storage_id;

    /* product side */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    /* branch side */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    /* ---------- ctors ---------- */

    public Storage() { }

    public Storage(Product product, Branch branch, int quantity) {
        this.product  = product;
        this.branch   = branch;
        this.quantity = quantity;
    }

    /* ---------- getters / setters ---------- */

    public int getStorageId()         { return storage_id; }

    public Product getProduct()       { return product; }
    public void    setProduct(Product p) { this.product = p; }

    public Branch  getBranch()        { return branch; }
    public void    setBranch(Branch b){ this.branch = b; }

    public int getQuantity()          { return quantity; }
    public void setQuantity(int q)    { this.quantity = q; }

    /* ---------- helpers ---------- */

    public void add(int delta) { this.quantity += delta; }
    public void subtract(int delta) { this.quantity -= delta; }
}
