import genius.core.Bid;


public class BidWrapper implements Comparable<BidWrapper>{

    private Bid bid;
    private Double utility;

    public BidWrapper(Bid b, double u) {
        bid = b;
        utility = u;
    }

    public Bid getBid() {
        return bid;
    }

    public double getUtility() {
        return utility;
    }

    @Override
    public int compareTo(BidWrapper o) {
        // compareTo should return < 0 if this is supposed to be
        // less than other, > 0 if this is supposed to be greater than
        // other and 0 if they are supposed to be equal
        return this.utility.compareTo(o.utility);
    }
}
