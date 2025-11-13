package luti.server.client.dto;

public class KeyBlock {

    private long start;
    private long end;

    public KeyBlock() {
    }

    public KeyBlock(long start, long end) {
        this.start = start;
        this.end = end;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    @Override
    public String toString() {
        return "KeyBlock{" +
            "start=" + start +
            ", end=" + end +
            '}';
    }
}
