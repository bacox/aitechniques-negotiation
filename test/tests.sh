#!/bin/bash

agents=(
    'negotiator.parties.BoulwareNegotiationParty'
    'negotiator.parties.ConcederNegotiationParty'
    'agents.TimeDependentAgentHardliner'
    'agents.anac.y2011.TheNegotiator.TheNegotiator'
    'agents.anac.y2011.HardHeaded.KLH'
)

# Change this to the location of your compiled class files
our_agent='C:\Users\Daphne\Documents\TI\Master\TI 2019-2020\Q1\AI Techniques\Repo\aitechniques-negotiation\group44\Group44_Party.class'
our_old_agent='C:\Users\Daphne\Documents\TI\Master\TI 2019-2020\Q1\AI Techniques\Repo\aitechniques-negotiation\group44\Group44_Party_old.class'

declare -A domains
declare -A names

# Party domain
domains[0,0]='etc/templates/partydomain/party1_utility_u10.xml'
domains[0,1]='etc/templates/partydomain/party2_utility.xml'
names[0]='party'

# Laptop domain
domains[1,0]='etc/templates/anac/y2011/Laptop/laptop_buyer_utility.xml'
domains[1,1]='etc/templates/anac/y2011/Laptop/laptop_seller_utility_u10.xml'
names[1]='laptop'

# Jobs domain
domains[2,0]='etc/templates/Jobs/Jobs_util1.xml'
domains[2,1]='etc/templates/Jobs/Jobs_util2.xml'
names[2]='jobs'

# Loop over all 4 domains (or change to x domains if more are added/removed above)
for ((i=0;i<3;i=i+1)); do
    PART=""
    PREF=""
    for j in ${agents[@]}; do
        PART="${PART} $j"
    done
    for ((j=0;j<2;j=j+1)); do
        PREF="${PREF} ${domains[$i,$j]}"
    done

    python session_xml.py --agent $our_agent --parties $PART --preferences $PREF --repeat 5
    cd ../dependency
    java.exe -cp ../dependency/genius-9.1.11.jar genius.cli.Runner ../test/test_tournament.xml ../test/results
    cd ../test
    cat results.csv | sed '1d' > results_${names[$i]}.csv
    rm results.csv

    python session_xml.py --agent $our_agent --parties $our_agent $our_old_agent --preferences $PREF --repeat 5
    cd ../dependency
    java.exe -cp ../dependency/genius-9.1.11.jar genius.cli.Runner ../test/test_tournament.xml ../test/results
    cd ../test
    cat results.csv | sed '1d' > results_our_${names[$i]}.csv
    rm results.csv
done