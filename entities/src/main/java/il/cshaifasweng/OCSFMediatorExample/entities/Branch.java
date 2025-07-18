package il.cshaifasweng.OCSFMediatorExample.entities;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Maps the `branches` table
 *   branch_id | name | manager_id
 */
@Entity
@Table(name = "branches")
public class Branch implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "branch_id")
    private int branch_id;

    @Column(name = "branch_name", nullable = false, unique = true, length = 64)
    private String branch_name;

    /**↳  optional – when null we simply do not have a manager yet  */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private transient User manager;


    /* ---------- bidirectional helper ↓ (optional) ---------- */

    @OneToMany(mappedBy = "branch", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Storage> stockLines = new ArrayList<>();

    /* ---------- ctors ---------- */

    public Branch() { }

    public Branch(String name) {
        this.branch_name = name;
    }

    /* ---------- getters / setters ---------- */

    public int getBranchId()        { return branch_id; }
    public String getName()         { return branch_name; }
    public void   setName(String n) { this.branch_name = n; }

    public void setBranchId(int id) { this.branch_id = id; }

    public User getManager()              { return manager; }
    public void setManager(User manager)  { this.manager = manager; }

    public List<Storage> getStockLines()  { return stockLines; }

    /* ---------- convenience ---------- */

    @Override
    public String toString() {
        return branch_name; // or getName()
    }



    public String getBranchName() {
        return branch_name;
    }
}
