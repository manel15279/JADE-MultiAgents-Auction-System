# Multi-Agent Auction Platform with JADE

## Overview
This repository contains the source code for a multi-agent auction platform implemented using JADE (Java Agent DEvelopment Framework). The platform simulates a decentralized auction environment where buyers and sellers interact autonomously to conduct auctions. Buyers participate in auctions by submitting proposals, while sellers coordinate the auction process by managing bids and selecting winning proposals.

## Features
- Implementation of buyer agents that autonomously participate in auctions.
- Implementation of seller agents that manage the auction process.
- Communication between agents using ACL messages.
- Decentralized decision-making using JADE's multi-agent framework.
- Simulated auction environment for testing and demonstration.

## Structure
- The `Buyer` class represents a buyer agent that participates in auctions. Each buyer agent has a budget and autonomously submits proposals to purchase items.
- The `Auctioner` class represents a seller agent that manages auctions. Each seller agent coordinates the auction process by receiving proposals from buyers, managing bids, and selecting winning proposals.

## Usage
To run the auction platform locally, follow these steps:

1. Clone the repository to your local machine.
2. Set up the JADE environment by downloading the JADE framework from the official website.
3. Import the project into your preferred Java IDE.
4. Build and run the project using this command : ` -gui -agents Seller:Auctioner("Watch",100,1000);Buyer1:Buyer;Buyer2:Buyer`, where "Watch" is the name of the product being auctioned, 100 is the initial price of the product and 1000 is the reserve price of the product.
5. Explore the simulation of decentralized auctions with multiple buyers and sellers.



