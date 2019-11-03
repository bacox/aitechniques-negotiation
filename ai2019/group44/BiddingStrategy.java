package group44;

import genius.core.Bid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class BiddingStrategy {

    public class HistoryItem {
        public double time;
        public BidWrapper bidWrapper;
        public HistoryItem(double t, BidWrapper bw) {
            this.time = t;
            this.bidWrapper = bw;
        }
    }

    private OpponentModel opponentModel;
    private BidSpace bidSpace;
    private ArrayList<BidWrapper> opponentsBids;
    private boolean firstBidFetch = true;


    ArrayList<HistoryItem> bidHistory = new ArrayList<HistoryItem>();

    public BiddingStrategy(OpponentModel model, BidSpace space, ArrayList<BidWrapper> oBids) {
        // Save the reference of the models so they will be updated automatically
        opponentModel = model;
        bidSpace = space;
        opponentsBids = oBids;
    }

    private BidWrapper createBid(double l, double u) {
        BidWrapper bid = null;
        List<BidWrapper> possibilities = this.bidSpace.getBidsInRange(l, u);
        if(possibilities.size() == 0){
           bid = this.bidSpace.maxUtilityBid;
        } else {
            // Get random bid in our possible utility range
            // Bids are ordered by high to low
            // Maybe offer bid from the high side instead of random?
            bid = possibilities.get(new Random().nextInt(possibilities.size()));
        }
        return bid;
    }

    public Bid generateBid(double l, double u, Group44_Party.PHASE phase, double time) {
        BidWrapper generatedBid;
        switch (phase){
            case ONE:
                generatedBid = generatePhaseOneBid(l,u);
                break;
            case TWO:
                generatedBid = generatePhaseTwoBid(l, u);
                break;
            case THREE:
                generatedBid = generatePhaseThreeBid(l, u);
                break;
            default:
                generatedBid = generatePhaseOneBid(l,u);

        }
        bidHistory.add(new HistoryItem(time, generatedBid));
        return generatedBid.getBid();
    }

    private BidWrapper generatePhaseOneBid(double l, double u){
        if (Math.random() <= 0.7) {
            return createBid(l, u);
        } else {
            return createBid(l, 1.0);
        }
    }
    private BidWrapper generatePhaseTwoBid(double l, double u){
        if (Math.random() < 0.3) {
            u = 1.0;
        }
        List<BidWrapper> possibilities = this.bidSpace.getBidsInRange(l, u);
        for(BidWrapper possibility : possibilities) {
            double opponentUtility = opponentModel.estimateOpponentUtility(possibility.getBid());
            if(possibility.getUtility() > opponentUtility) {
                System.out.println("Found a bid conforming phase 2 " + possibility.getUtility());
                return possibility;

            }
        }
        if(possibilities.size() > 0) {
            return possibilities.get(0);
        }

        return bidSpace.maxUtilityBid;
    }

    private BidWrapper generatePhaseThreeBid(double l, double u){

        if (firstBidFetch) {
            Collections.sort(opponentsBids);
            firstBidFetch = false;
        }
        for (BidWrapper b : opponentsBids) {
            double utility = b.getUtility();
            if (utility >= l)
                return b;
        }
        return generatePhaseOneBid(l, u);
    }

    public void printBidHistory() {
        for(HistoryItem h : bidHistory) {
            System.out.println("Time: " + h.time + "\t\t utility of bid: " + h.bidWrapper.getUtility());
        }
    }

}
