package group44;

import java.util.*;
import java.util.ArrayList;

import genius.core.AgentID;
import genius.core.Bid;
import genius.core.actions.Accept;
import genius.core.actions.Action;
import genius.core.actions.Offer;
import genius.core.parties.AbstractNegotiationParty;
import genius.core.parties.NegotiationInfo;
import genius.core.utility.AdditiveUtilitySpace;

public class Group44_Party extends AbstractNegotiationParty {

    private OpponentModel opponentModel;
    private ArrayList<BidWrapper> opponentBids = new ArrayList<BidWrapper>();

    private Bid lastReceivedBid = null;
    private BidSpace bidSpace = null;

    private int numberOfBids = 0;

    private double totalTimeGiven = 0;
    private double last15RoundsAvgTime = 0;
    private double lastBidTimestamp = 0;
    private double epsilon = 0.2;
    private double[] phaseFractions = new double[]{28.0 / 36.0, 7.0 / 36.0, 1.0 / 36.0};

    private boolean firstBidFetch = true;

    private enum PHASE {
        ONE,
        TWO,
        THREE;
    }

    private PHASE currentPhase;

    @Override
    public void init(NegotiationInfo info) {

        super.init(info);
        System.out.println("The user model is: " + userModel); // This is null
        System.out.println("Discount Factor is " + getUtilitySpace().getDiscountFactor());
        System.out.println("Reservation Value is " + getUtilitySpace().getReservationValueUndiscounted());
        currentPhase = PHASE.ONE;

        // If you need to initialize some variables, please initialize them below
        this.totalTimeGiven = info.getTimeline().getTotalTime();

        // Initialize opponent model
        this.opponentModel = new OpponentModel(((AdditiveUtilitySpace) info.getUtilitySpace()), this.epsilon);

        // Generate our bidspace with a time limit of 45 seconds
        this.bidSpace = new BidSpace(info.getUtilitySpace(), 45);
        System.out.println("Has finished bid generation ? " + this.bidSpace.isFinishedSort());
    }

    public double concessionValue(double start, double range, double fraction) {
        return start - (range * fraction);
    }

    public double concessionValueByPhase(double time) {
        switch (currentPhase) {
            case ONE:
                return concessionValue(1.0, (1.0 / 8.0), timeFractionByPhase(time));
            case TWO:
                return concessionValue((7.0 / 8.0), (1.0 / 8.0), timeFractionByPhase(time));
            case THREE:
                return concessionValue((5.0 / 8.0), (6.0 / 8.0), timeFractionByPhase(time));
            default:
                return 0;
        }
    }

    public double timeFractionByPhase(double time) {
        switch (currentPhase) {
            case ONE:
                return time / phaseFractions[0];
            case TWO:
                return (time - phaseFractions[0]) / (phaseFractions[1]);
            case THREE:
                return (time - phaseFractions[0] - phaseFractions[1]) / (phaseFractions[2]);
            default:
                return 0;
        }
    }

    public void updatePhase(double time) {
        switch (currentPhase) {
            case ONE:
                if (time > phaseFractions[0]) {
                    currentPhase = PHASE.TWO;
                }
                break;
            case TWO:
                if (time > phaseFractions[0] + phaseFractions[1]) {
                    currentPhase = PHASE.THREE;
                }
                break;
            case THREE:
                break;
            default:
        }
    }


    public Bid getBoundedBid(double l) {
        if (firstBidFetch) {
            Collections.sort(opponentBids);
            firstBidFetch = false;
        }
        for (BidWrapper b : opponentBids) {
            double utility = b.getUtility();
            if (utility >= l)
                return b.getBid();
        }
        return null;
    }

    public double upperBound(double l) {
        return l + (0.2 * (1 - l));
    }

    public Bid generateBid(double l, double u) {
        Bid bid = null;
        List<BidWrapper> possibilities = this.bidSpace.getBidsInRange(l, u);
        if(possibilities.size() == 0){
            try {
                // If the range is too small, just bid the max utility
                bid = getUtilitySpace().getMaxUtilityBid();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // Get random bid in our possible utility range
            // Bids are ordered by high to low
            // Maybe offer bid from the high side instead of random?
            bid = possibilities.get(new Random().nextInt(possibilities.size())).getBid();
        }
        return bid;
    }

    public Bid generateBid(double l) {
        return generateBid(l, 1.0);
    }

    public Bid generatePhaseOneBid(double l, double u) {
        if (Math.random() <= 0.7) {
            return generateBid(l, u);
        } else {
            return generateBid(l);
        }
    }

    public Bid generatePhaseTwoBid(double l, double u) {
//        Get bidlist
        if (Math.random() < 0.3) {
            u = 1.0;
        }
        List<BidWrapper> possibilities = this.bidSpace.getBidsInRange(l, u);
        for(BidWrapper possibility : possibilities) {
            double opponentUtility = opponentModel.estimateOpponentUtility(possibility.getBid());
            if(possibility.getUtility() > opponentUtility) {
                System.out.println("Found a bid conforming phase 2 " + possibility.getUtility());
                return possibility.getBid();

            }
        }
        if(possibilities.size() > 0) {
            return possibilities.get(0).getBid();
        }
        try {
            // If the range is too small, just bid the max utility
            return getUtilitySpace().getMaxUtilityBid();
        } catch (Exception e) {
            return bidSpace.maxUtilityBid.getBid();
        }
    }


    @Override
    public Action chooseAction(List<Class<? extends Action>> validActions) {
        // Continue bidspace generation if needed.
        this.bidSpace.continueGeneration();

        // Setup current actions time lower and upperbound
        double l = concessionValueByPhase(getTimeLine().getTime());
        double u = upperBound(l);
        Bid bid = null;
        if (currentPhase == PHASE.ONE) {
            bid = generatePhaseOneBid(l, u);
        } else if(currentPhase == PHASE.TWO){
            bid = generatePhaseTwoBid(l,u);
        } else {
//            Phase two and three
            bid = getBoundedBid(l);
            if (bid == null) {
                bid = generatePhaseOneBid(l, u);
            }
            System.out.println("In phase " + currentPhase + " sending offer " + getUtility(bid) + "with lower bound " + l);
        }

        double estimatedOpponentUtil = this.opponentModel.estimateOpponentUtility(bid);
//        System.out.println("estimated opponent utility: " + estimatedOpponentUtil);

        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            System.out.println("Not allowed to sleep");
        }

        //If the utility of the bid we were going to do is equal to or lower than
        // the utility of the bid just done by the other party, we accept it.
        if (lastReceivedBid != null && l <= this.getUtility(lastReceivedBid)) {
            System.out.println("Accepting bid #1 with phase " + currentPhase + " concession value: " + l + " and bid utility: " + this.getUtility(lastReceivedBid));
            return new Accept(getPartyId(), lastReceivedBid);
        }

        // In phase 3, we accept any bid if we estimate the number of rounds left to be less than 15
        if (currentPhase == PHASE.THREE && this.numRoundsLeft() <= 15) {
            System.out.println("Accepting bid #2 with phase " + currentPhase + " concession value: " + l + " and bid utility: " + this.getUtility(lastReceivedBid));
            return new Accept(getPartyId(), lastReceivedBid);
        }

        // Return a new offer
        System.out.println("New offer: " + getUtility(bid));
        return this.actionSetTimestamp(new Offer(getPartyId(), bid));
    }

    //Method to set the timestamp just before choosing an action, allowing us to monitor the time between
    // bids accurately
    public Action actionSetTimestamp(Action action) {
        this.updateLastBidTimestamp();
        return action;
    }

    @Override
    public void receiveMessage(AgentID sender, Action action) {
        super.receiveMessage(sender, action);
        double time = getTimeLine().getTime();
        updatePhase(time);
        if (action instanceof Offer) {
            Bid newBid = ((Offer) action).getBid();
            if (lastReceivedBid != null) {
                this.opponentModel.update(lastReceivedBid, newBid, time);
            }
            lastReceivedBid = newBid;
            this.updateLastBidTimestamp();
            if (currentPhase == PHASE.ONE) {
//                Save opponents bids for phase two and three
                Bid b = ((Offer) action).getBid();
                opponentBids.add(new BidWrapper(b, getUtility(b)));
//                opponentBids.add(((Offer) action).getBid());
            }
        }
    }

    @Override
    public String getDescription() {
        return "The negotiation agent of group 44";
    }

    public void updateLastBidTimestamp() {
        numberOfBids++;

        double currentTime = this.getTimeLine().getCurrentTime();
        this.updateLast15RoundsAvgTime(currentTime);

//    	System.out.println(("Last bid timestamp: " + this.lastBidTimestamp));
        this.lastBidTimestamp = currentTime;
//    	System.out.println(("New bid timestamp: " + this.lastBidTimestamp));

    }

    public void updateLast15RoundsAvgTime(double timestamp) {
        double timeSpentLastBid = timestamp - this.lastBidTimestamp;
        int n = Math.min(numberOfBids, 15);
        this.last15RoundsAvgTime =
                this.last15RoundsAvgTime + (timeSpentLastBid - this.last15RoundsAvgTime) / n;

//    	System.out.println(("last15RoundsAvgTime: " + this.last15RoundsAvgTime));
    }

    public double numRoundsLeft() {
        return (this.totalTimeGiven - this.getTimeLine().getCurrentTime()) / this.last15RoundsAvgTime;
    }
}
