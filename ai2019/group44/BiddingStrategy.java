package group44;

import genius.core.Bid;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class BiddingStrategy {

    public class HistoryItem {
        public double time;
        public double lowerBound;
        public double upperBound;
        public BidWrapper bidWrapper;
        public HistoryItem(double t, BidWrapper bw, double l, double u) {
            this.time = t;
            this.bidWrapper = bw;
            lowerBound = l;
            upperBound = u;
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
        bidHistory.add(new HistoryItem(time, generatedBid, l, u));
        return generatedBid.getBid();
    }

    private BidWrapper generatePhaseOneBid(double l, double u){
//        Was 0.7
        if (Math.random() <= 0.7) {
            return createBid(l, u);
        } else {
            return createBid(l, 1.0);
        }
    }
    private BidWrapper generatePhaseTwoBid(double l, double u){
        // Get the set bid valid bids from the bidspace given the concession graph
        List<BidWrapper> possibilities = this.bidSpace.getBidsInRange(l, u);
        BidWrapper bestForOpponents = null;
        double bestUtilOpponent = 0;
        for(BidWrapper possibility : possibilities) {
            // Use the opponent model to estimate the opponents utility for this bid.
            double opponentUtility = opponentModel.estimateOpponentUtility(possibility.getBid());
            // Try to find a bid that has a better utility for us than for them.
            // But from this set try to find to best bid for the opponent so he is more likely to accept.
            if(possibility.getUtility() > opponentUtility) {
                if(opponentUtility > bestUtilOpponent) {
                    // Update the best found bid
                    bestForOpponents = possibility;
                    bestUtilOpponent = opponentUtility;
                }
            }
        }
        // If the opponents estimated utility is still 0 means we did not find a bid within the constraints.
        // If the opponents estimated utility > 0, we have a valid bid.
        if(bestUtilOpponent > 0){
            // We found the best possible bid given the opponent model
            return bestForOpponents;
        }
        if(possibilities.size() > 0) {
            // Our best alternative is to return the best bid given the constraints of the concession graph
            return possibilities.get(0);
        }
        // If everything fails just return our best possible bid given the domain
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

    public void saveHistoryToCsv(String filename) {
        FileWriter pw = null;
        try {
            pw = new FileWriter(new File(filename), false);
            StringBuilder sb = new StringBuilder();
            sb.append("Time, Utility, Lower Bound, Upper Bound\n");
            for(BiddingStrategy.HistoryItem h: bidHistory) {
                sb.append(h.time)
                        .append(",")
                        .append(h.bidWrapper.getUtility())
                        .append(",")
                        .append(h.lowerBound)
                        .append(",")
                        .append(h.upperBound)
                        .append("\n");
            }
            pw.write(sb.toString());
            pw.close();
            System.out.println("Data saved to file");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println("Unable to save data to file");
        }
    }

}
