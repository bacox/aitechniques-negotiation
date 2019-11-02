package group44;

import genius.core.Bid;
import genius.core.BidIterator;
import genius.core.utility.AbstractUtilitySpace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class BidSpace {
    // Time limit for enumeration and sorting
    private double limit;

    public boolean isFinishedEnumeration() {
        return finishedEnumeration;
    }

    public boolean isFinishedSort() {
        return finishedSort;
    }

    protected BidWrapper maxUtilityBid;

    // Keep track of being finished with enumeration and sorting
    private boolean finishedEnumeration = false;
    private boolean finishedSort = false;

    // Iterator as class variable to continue later in case of timeout
    protected BidIterator iter = null;

    // Holds all generated bids
    protected List<BidWrapper> allBids = new ArrayList<BidWrapper>();

    // General utility space information
    protected AbstractUtilitySpace utilitySpace;

    /**
     * Sets the utility space and timeout and starts bid generation
     *
     * @param UtilitySpace
     * @param timeLimit
     */
    public BidSpace(AbstractUtilitySpace UtilitySpace, double timeLimit) {
        this.limit = timeLimit * 1000000000; // Convert to nanoseconds
        this.utilitySpace = UtilitySpace;
        this.iter = new BidIterator(this.utilitySpace.getDomain());

        generateAllBids();
    }

    /**
     * Continues bid generation in case it has not finished yet
     */
    public void continueGeneration(){
        if (!this.finishedEnumeration){
            generateAllBids();
        } else if(!this.finishedSort){
            sort(this.limit);
        }
    }

    protected void sort(double timeLeft){
        double start = System.nanoTime();
        long size = this.allBids.size();
        double time = size * Math.log(size) * 100; // Sort has n log n complexity, assume 10 million elems / s sorting
        if (timeLeft - (System.nanoTime() - start) > time ){
            Collections.sort(this.allBids, Collections.reverseOrder());
            this.finishedSort = true;
        }
        maxUtilityBid = this.allBids.get(0);
    }

    /**
     * Generates all the possible bids in the domain
     */
    public void generateAllBids() {
        double start = System.nanoTime();
        while (this.iter.hasNext() && (System.nanoTime() - start) < this.limit) {
            Bid bid = this.iter.next();
            try {
                BidWrapper bidWrapper = new BidWrapper(bid, this.utilitySpace.getUtility(bid));
                this.allBids.add(bidWrapper);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if(!this.iter.hasNext()){
            this.finishedEnumeration = true;
            sort(this.limit - (System.nanoTime() - start)); // Start sorting with timeleft
        }


    }

    /**
     * Returns all bids with utility higher than lowerbound and lower or equal to upperbound
     *
     * @param lowerbound
     * @param upperbound
     *
     * @return List<BidWrapper>
     */
    public List<BidWrapper> getBidsInRange(double lowerbound, double upperbound){
        List<BidWrapper> bidsInRange = new ArrayList<BidWrapper>();

//        System.out.println("Getting bids in utility range : " + lowerbound + " to : " + upperbound);

        for (BidWrapper bid : this.allBids){
            double util = bid.getUtility();
            // skip rest whilst above upperbound (allBids list is sorted already)
            if (util > upperbound) continue;
            if(util <= upperbound && util > lowerbound){
                bidsInRange.add(bid);
            }
            // Stop looking further, rest will all be below lowerbound
            if (util < lowerbound) break;
        }

//        System.out.println("Found number of possible bids: "+bidsInRange.size());

        return bidsInRange;
    }
}
