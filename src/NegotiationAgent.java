import java.util.ArrayList;
import java.util.List;
import java.util.ArrayList;

import genius.core.AgentID;
import genius.core.Bid;
import genius.core.actions.Accept;
import genius.core.actions.Action;
import genius.core.actions.Offer;
import genius.core.parties.AbstractNegotiationParty;
import genius.core.parties.NegotiationInfo;

public class NegotiationAgent extends AbstractNegotiationParty{

    private Bid lastReceivedBid = null;

    private int numberOfBids = 0;
    private double totalTimeGiven = 0;
    private double last15RoundsAvgTime = 0;
    private double lastBidTimestamp = 0;
//    private int currentPhase = 0;

    private enum PHASE {
        ONE,
        TWO,
        THREE;
    }
    private double[] phaseFractions = new double[] {28.0/36.0, 7.0/36.0, 1.0/36.0};
    private PHASE currentPhase;

    private ArrayList<Bid> opponentBids = new ArrayList<Bid>();

    @Override
    public void init(NegotiationInfo info) {

        super.init(info);

        System.out.println("Discount Factor is " + getUtilitySpace().getDiscountFactor());
        System.out.println("Reservation Value is " + getUtilitySpace().getReservationValueUndiscounted());
        currentPhase = PHASE.ONE;

        // if you need to initialize some variables, please initialize them
        // below
        this.totalTimeGiven = info.getTimeline().getTotalTime();
       
    }

    public double concessionValue(double start, double range, double fraction) {
        return start - (range * fraction);
    }

    public double concessionValueByPhase(double time) {
        switch (currentPhase) {
            case ONE:
                return concessionValue(1.0, (3.0/8.0),timeFractionByPhase(time));
            case TWO:
                return concessionValue((7.0/8.0), (2.0/8.0),timeFractionByPhase(time));
            case THREE:
                return concessionValue((5.0/8.0), (5.0/8.0),timeFractionByPhase(time));
            default:
                return 0;
        }
    }

    public double timeFractionByPhase(double time) {
        switch (currentPhase) {
            case ONE:
                return time / phaseFractions[0];
            case TWO:
                return (time- phaseFractions[0]) / (phaseFractions[1]);
            case THREE:
                return (time- phaseFractions[0] - phaseFractions[1]) / (phaseFractions[2]);
            default:
                return 0;
        }
    }

    public void updatePhase() {
        double time = getTimeLine().getTime();
        switch (currentPhase) {
            case ONE:
                if( time > phaseFractions[0]) {
                    currentPhase = PHASE.TWO;
                }
                break;
            case TWO:
                if( time > phaseFractions[0] + phaseFractions[1]) {
                    currentPhase = PHASE.THREE;
                }
                break;
            case THREE:
                break;
            default:
        }
    }

    public double upperBound(double l) {
        return l + (0.2 * (1 - l));
    }

    public Bid generateBid(double l, double u) {
        boolean bidInRange = false;
        Bid bid = generateRandomBid();
        while(!bidInRange) {
            double util = this.getUtility(bid);
            if(util >= l && util <= u) {
                bidInRange = true;
            }
            bid = generateRandomBid();
        }
        return bid;
    }

    public Bid generateBid(double l) {
        return generateBid(l, Integer.MAX_VALUE);
    }



    @Override
    public Action chooseAction(List<Class<? extends Action>> validActions) {

        double time = getTimeLine().getTime();
        double l = concessionValueByPhase(time);
        double u = upperBound(l);
        Bid bid = generateRandomBid();
        if (currentPhase == PHASE.ONE) {
            if( Math.random() > 0.7) {
                bid = generateBid(l, u);
            } else {
                bid = generateBid(l);
            }
        } else {
//            Phase two and three
            bid = opponentBids.get(1);
        }

        double bidUtil = this.getUtility(bid);
        System.out.println("Utility of our bid: " + bidUtil);
        System.out.println("Utility of last bid received: " + this.getUtility(lastReceivedBid));
        System.out.println("This is phase " + currentPhase + " " + concessionValueByPhase(time) + " \t\t " + timeFractionByPhase(time));

        try {
            Thread.sleep(250);
        } catch (InterruptedException e) {
            System.out.println("Not allowed to sleep");
        }
    	
    	//If the utility of the bid we were going to do is equal to or lower than
    	// the utility of the bid just done by the other party, we accept it.
    	if (lastReceivedBid != null && bidUtil <= this.getUtility(lastReceivedBid)) {
    		return new Accept(getPartyId(), lastReceivedBid); 
    	}
    	
    	// In phase 3, we accept any bid if we estimate the number of rounds left to be less than 15
    	if (currentPhase == PHASE.THREE && this.numRoundsLeft() <= 15) {
    		return new Accept(getPartyId(), lastReceivedBid); 
    	}
    	
    	// Return a new offer
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
        updatePhase();
        if (action instanceof Offer) {
            lastReceivedBid = ((Offer) action).getBid();
            this.updateLastBidTimestamp();
            if(currentPhase == PHASE.ONE) {
//                Save opponents bids for phase two and three
                opponentBids.add(((Offer) action).getBid());
            }
        }
    }

    @Override
    public String getDescription() {
        return "example party group N";
    }
    
    public void updateLastBidTimestamp() {
    	numberOfBids++;
    	
    	double currentTime = this.getTimeLine().getCurrentTime();
    	this.updateLast15RoundsAvgTime(currentTime);
    	
    	System.out.println(("Last bid timestamp: " + this.lastBidTimestamp));
    	this.lastBidTimestamp = currentTime;
    	System.out.println(("New bid timestamp: " + this.lastBidTimestamp));

    }
    
    public void updateLast15RoundsAvgTime(double timestamp) {
    	double timeSpentLastBid = timestamp - this.lastBidTimestamp;
    	int n = Math.min(numberOfBids, 15);
    	this.last15RoundsAvgTime = 
    			this.last15RoundsAvgTime + (timeSpentLastBid - this.last15RoundsAvgTime) / n;
    	
    	System.out.println(("last15RoundsAvgTime: " + this.last15RoundsAvgTime));
    }
    
    public double numRoundsLeft() {
    	return (this.totalTimeGiven - this.getTimeLine().getCurrentTime()) / this.last15RoundsAvgTime;
    }
}
