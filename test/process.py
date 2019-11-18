import argparse
import pandas as pd
import numpy as np
from os import path

def unique(data):
    values = []
    for i in data:
        values.append(i.split('@')[0])
    return np.unique(values).tolist()

def count(data, value):
    amount = 0
    for i in data:
        if i.split('@')[0] == value:
            amount += 1
    return amount

parser = argparse.ArgumentParser(description='Processes csv files created by genius.')
parser.add_argument('-f', required=True)
args = parser.parse_args()

if not path.exists(args.f):
    print("File does not exist")
    exit(0)

data = pd.read_csv(args.f, delimiter=';')
agent1 = data['Agent 1']
agent2 = data['Agent 2']

util1 = np.asarray(data['Utility 1'])
util2 = np.asarray(data['Utility 2'])

agents = unique(agent1)
utilities = [[] for _ in range(len(agents))]
for i in range(len(util1)):
    utilities[agents.index(agent1[i].split('@')[0])].append(util1[i])
    utilities[agents.index(agent2[i].split('@')[0])].append(util2[i])

mean_util = np.mean(np.asarray(utilities), axis=1)
std_util = np.std(np.asarray(utilities), axis=1)
print(agents)
print(mean_util)
print(std_util)
print("Negotiations per agent: ", count(agent1, agents[0])+count(agent2, agents[0]))