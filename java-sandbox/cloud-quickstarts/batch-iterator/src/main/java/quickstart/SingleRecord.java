package quickstart;

/** One record in the dataset being processed. */
public class SingleRecord {
    private int id;

    public SingleRecord(int id) {
        this.id = id;
    }

    /** The JSON deserializer needs a no-arg constructor. */
    public SingleRecord() {}

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return "SingleRecord{id=" + id + "}";
    }
}
