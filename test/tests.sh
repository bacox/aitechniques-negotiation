#!/bin/bash

agents=(
    'negotiator.parties.BoulwareNegotiationParty'
    'negotiator.parties.ConcederNegotiationParty'
    'agents.TimeDependentAgentHardliner'
    'agents.anac.y2011.TheNegotiator.TheNegotiator'
    'agents.anac.y2011.HardHeaded.KLH'
)

our_agent='E:\UNI\Master\AIT\aitechniques-negotiation\out\production\aitechniques-negotiation\group44\Group44_Party.class'

declare -A domains
declare -A names
# Party domain
domains[0,0]='etc/templates/partydomain/party1_utility.xml'
domains[0,1]='etc/templates/partydomain/party2_utility.xml'
names[0]='party'

# Laptop domain
domains[1,0]='etc/templates/anac/y2011/Laptop/laptop_buyer_utility.xml'
domains[1,1]='etc/templates/anac/y2011/Laptop/laptop_seller_utility.xml'
names[1]='laptop'

# Energy domain
domains[2,0]='etc/templates/anac/y2011/Energy/energy_consumer.xml'
domains[2,1]='etc/templates/anac/y2011/Energy/energy_distributor.xml'
names[2]='energy'

# Jobs domain
domains[3,0]='etc/templates/Jobs/Jobs_util1.xml'
domains[3,1]='etc/templates/Jobs/Jobs_util2.xml'
names[3]='jobs'


for ((i=0;i<4;i=i+1)); do
    PART=""
    PREF=""
    for j in ${agents[@]}; do
        PART="${PART} $j"
    done
    PART="${PART} $our_agent"
    for ((j=0;j<2;j=j+1)); do
        PREF="${PREF} ${domains[$i,$j]}"
    done
    python session_xml.py --parties $PART --preferences $PREF -dt ROUND --repeat 4
    cd ../dependency
    java.exe -cp ../dependency/genius-9.1.11.jar genius.cli.Runner ../test/test_tournament.xml ../test/results
    cd ../test
    cat results.csv | sed '1d' > results_${names[$i]}.csv
    rm results.csv

    python session_xml.py --parties $our_agent $our_agent --preferences $PREF -dt ROUND --repeat 10
    cd ../dependency
    java.exe -cp ../dependency/genius-9.1.11.jar genius.cli.Runner ../test/test_tournament.xml ../test/results
    cd ../test
    cat results.csv | sed '1d' > results_our_${names[$i]}.csv
    rm results.csv
done