package persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * Created by andrey.sokolov on 20.10.2016.
 */
@Entity
public class Winner {
    @Id
    @GeneratedValue
    private Long id;

    @Column(unique = true)
    private String mention;

    @Column
    private long score;

    @Column
    private String rolled;

    public Winner() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMention() {
        return mention;
    }

    public void setMention(String mention) {
        this.mention = mention;
    }

    public long getScore() {
        return score;
    }

    public void setScore(long score) {
        this.score = score;
    }

    public String getRolled() {
        return rolled;
    }

    public void setRolled(String rolled) {
        this.rolled = rolled;
    }
}
