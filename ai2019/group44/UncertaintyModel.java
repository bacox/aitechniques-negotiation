package group44;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import genius.core.Bid;
import genius.core.issue.Issue;
import genius.core.issue.ValueDiscrete;
import genius.core.uncertainty.UserModel;

public class UncertaintyModel {
	
	private Map<Issue, Double> weights;
	private Map<Issue, HashMap<ValueDiscrete, Double>> issueValues;
	private List<Bid> bidOrder;
	
	public UncertaintyModel() {
	}
	
	public void initEstimation(UserModel userModel) {
		this.issueValues = new HashMap<Issue, HashMap<ValueDiscrete, Double>>();
		this.weights = new HashMap<Issue, Double>();
		this.bidOrder = userModel.getBidRanking().getBidOrder();		
		double currentWeight = 0.0;

		for (Bid bid : bidOrder) {
			currentWeight = currentWeight + 1.0;
			List<Issue> issues = bid.getIssues();

			for (Issue issue : issues) {				
				if (!this.issueValues.containsKey(issue)) {
					HashMap<ValueDiscrete, Double> temp = new HashMap<ValueDiscrete, Double>();
					temp.put((ValueDiscrete) bid.getValue(issue), 0.0);
					this.issueValues.put(issue, temp);
				}
				
				ValueDiscrete currentValue = (ValueDiscrete) bid.getValue(issue);
				
				if (!this.issueValues.get(issue).containsKey(currentValue)) {
					this.issueValues.get(issue).put(currentValue, currentWeight);
				} else {
					this.issueValues.get(issue).replace(currentValue, this.issueValues.get(issue).get(currentValue) + currentWeight);
				}
			}
		}
		
		normalizeValues();
		
		Map<Issue, Double> invertedStDevs = new HashMap<Issue, Double>();
		double invertedStDevSum = 0.0;
		for (Issue issue : issueValues.keySet() ) {
			double stDev = computeStDev(issue);
//			double invertedStDev = Math.pow(stDev, -1);
			double invertedStDev = stDev;
			invertedStDevSum += invertedStDev;
			invertedStDevs.put(issue, invertedStDev);
		}
		System.out.println("devs: " + invertedStDevs);
		System.out.println("sum: " + invertedStDevSum);
		for (Issue issue: invertedStDevs.keySet()) {
			double invertedStDev = invertedStDevs.get(issue);
			weights.put(issue, invertedStDev / invertedStDevSum);			
		}
		
		System.out.println("weights: "+ weights);
	}
	
	private double computeStDev(Issue issue) {
		double stDev = 0.0;
		Collection<Double> values = issueValues.get(issue).values();
		
        double sum = 0;
        
        for (double v : values) {
        	sum += v;
        }
        
        double avg = sum/values.size();
        double variance = 0;
        
        for (double v : values) {
        	variance += Math.pow(v - avg, 2);
        }
        
        variance /= values.size();
        stDev = Math.sqrt(variance);
        
        return stDev;
 
    }

	private void normalizeValues() {
		// Normalize values		
		for (Issue issue : issueValues.keySet()) {
			double maxValue = 0.0;
			HashMap<ValueDiscrete, Double> currentValues = issueValues.get(issue);
			
			for (ValueDiscrete value : currentValues.keySet()) {
				maxValue = Double.max(maxValue, currentValues.get(value));
			}
			
			for (ValueDiscrete value : currentValues.keySet()) {
				this.issueValues.get(issue).replace(value, this.issueValues.get(issue).get(value) / maxValue);
			}
		}
	}
	
	public Map<Issue, Double> getWeights() {
		return weights;
	}

	public Map<Issue, HashMap<ValueDiscrete, Double>> getIssueValues() {
		return issueValues;
	}
}
