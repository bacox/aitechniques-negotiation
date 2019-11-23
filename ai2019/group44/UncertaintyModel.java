package group44;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import genius.core.Bid;
import genius.core.issue.Issue;
import genius.core.issue.ValueDiscrete;
import genius.core.uncertainty.UserModel;

// Class to represent the Uncertainty Model
public class UncertaintyModel {
	
	private Map<Issue, Double> weights;
	private Map<Issue, HashMap<ValueDiscrete, Double>> issueValues;
	private List<Bid> bidOrder;
	
	public UncertaintyModel() {
	}
	
	// Estimate our preferences by reading out the Bid order received in the UserModel
	public void initEstimation(UserModel userModel) {
		// Map to contain the estimated utility of an (issue, value)-pair
		this.issueValues = new HashMap<Issue, HashMap<ValueDiscrete, Double>>();
		// Map to contain the estimated weight of an (issue, value)-pair
		this.weights = new HashMap<Issue, Double>();
		// Bid order as received by the userModel
		this.bidOrder = userModel.getBidRanking().getBidOrder();		
		double currentWeight = 0.0;
		
		for (Bid bid : bidOrder) {
			// Give more weight as a bid occurs more towards the end of the bid order, 
			// this works because the bid order is in ascending order
			currentWeight = currentWeight + 1.0;
			List<Issue> issues = bid.getIssues();

			for (Issue issue : issues) {
				// In the first loop, add the issues to the map
				if (!this.issueValues.containsKey(issue)) {
					HashMap<ValueDiscrete, Double> temp = new HashMap<ValueDiscrete, Double>();
					temp.put((ValueDiscrete) bid.getValue(issue), 0.0);
					this.issueValues.put(issue, temp);
				}
				
				ValueDiscrete currentValue = (ValueDiscrete) bid.getValue(issue);
				if (!this.issueValues.get(issue).containsKey(currentValue)) {
					// Add a value with the current weight when we see it for the first time
					this.issueValues.get(issue).put(currentValue, currentWeight);
				} else {
					// Increment the weight of the current value if we have seen it before
					this.issueValues.get(issue).replace(currentValue, this.issueValues.get(issue).get(currentValue) + currentWeight);
				}
			}
		}
		
		// Normalize the utilities
		normalizeValues();
		
		// To estimate the weight, we take into account the standard deviation in the bids for each issue
		//  A high standard deviation will result in a high weight for the corresponding issue
		Map<Issue, Double> stDevs = new HashMap<Issue, Double>();
		double stDevSum = 0.0;
		for (Issue issue : issueValues.keySet() ) {
			// Compute the standard deviation
			double stDev = computeStDev(issue);
			// Maintain a sum to later normalize the weights
			stDevSum += stDev;
			stDevs.put(issue, stDev);
		}
		
		// Normalize the weights 
		for (Issue issue: stDevs.keySet()) {
			double stDev = stDevs.get(issue);
			weights.put(issue, stDev / stDevSum);			
		}

	}
	
	// Function to compute the standard deviation using variance
	private double computeStDev(Issue issue) {
		double stDev = 0.0;
		Collection<Double> values = issueValues.get(issue).values();
		
		// Sum all values
        double sum = 0;
        for (double v : values) {
        	sum += v;
        }
        
        // Determine the average
        double avg = sum/values.size();
        
        // Determine the variance
        double variance = 0;
        for (double v : values) {
        	variance += Math.pow(v - avg, 2);
        }
        variance /= values.size();
        
        // Determine the standard deviation
        stDev = Math.sqrt(variance);
        
        return stDev;
 
    }

	private void normalizeValues() {
		// Normalize values by dividing each value with the maximum observed value		
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
	
	// Getter for the weights
	public Map<Issue, Double> getWeights() {
		return weights;
	}

	// Getter for the issueValues
	public Map<Issue, HashMap<ValueDiscrete, Double>> getIssueValues() {
		return issueValues;
	}
}
