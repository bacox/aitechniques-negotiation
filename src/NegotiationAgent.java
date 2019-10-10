import java.util.ArrayList;
import java.util.List;

import genius.core.AgentID;
import genius.core.Bid;
import genius.core.actions.Accept;
import genius.core.actions.Action;
import genius.core.actions.Offer;
import genius.core.parties.AbstractNegotiationParty;
import genius.core.parties.NegotiationInfo;

public class NegotiationAgent extends AbstractNegotiationParty{

    private Bid lastReceivedBid = null;
    private enum PHASE {
        ONE,
        TWO,
        THREE;
    }
    private double[] phaseFractions = new double[] {28.0/36.0, 7.0/36.0, 1.0/36.0};
    private PHASE phase;

    private ArrayList<Bid> opponentBids = new ArrayList<Bid>();

    @Override
    public void init(NegotiationInfo info) {

        super.init(info);

        System.out.println("Discount Factor is " + getUtilitySpace().getDiscountFactor());
        System.out.println("Reservation Value is " + getUtilitySpace().getReservationValueUndiscounted());
        phase = PHASE.ONE;

        // if you need to initialize some variables, please initialize them
        // below

    }

    public double concessionValue(double start, double range, double fraction) {
        return start - (range * fraction);
    }

    public double concessionValueByPhase(double time) {
        switch (phase) {
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
        switch (phase) {
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
        switch (phase) {
            case ONE:
                if( time > phaseFractions[0]) {
                    phase = PHASE.TWO;
                }
                break;
            case TWO:
                if( time > phaseFractions[0] + phaseFractions[1]) {
                    phase = PHASE.THREE;
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



    @Override
    public Action chooseAction(List<Class<? extends Action>> validActions) {

        double time = getTimeLine().getTime();
        System.out.println("This is phase " + phase + " " + concessionValueByPhase(time) + " \t\t " + timeFractionByPhase(time));

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            System.out.println("Not allowed to sleep");
        }
        if(lastReceivedBid != null) {
            if(getUtility(lastReceivedBid) >= 0.8) {
                return new Accept(getPartyId(), lastReceivedBid);
            }
        }
        if (lastReceivedBid == null || !validActions.contains(Accept.class) || Math.random() > 0 ) {
            try {
                return new Offer(getPartyId(), getUtilitySpace().getMaxUtilityBid());
            } catch (Exception e) {
                return new Offer(getPartyId(), lastReceivedBid);
            }
        } else {
            return new Accept(getPartyId(), lastReceivedBid);
        }
    }

    @Override
    public void receiveMessage(AgentID sender, Action action) {
        super.receiveMessage(sender, action);
        updatePhase();
        if (action instanceof Offer) {
            lastReceivedBid = ((Offer) action).getBid();
            if(phase == PHASE.ONE) {
//                Save opponents bids for phase two and three
                opponentBids.add(((Offer) action).getBid());
            }
        }
    }

    @Override
    public String getDescription() {
        return "example party group N";
    }
}
