# Decentralized Documentation of Maritime Traffic Incidents to support Conflict Resolution
For the investigation of major traffic incidents, like the stranding of the Ever Given in 2021, lager vessels are required to install a Voyage Data Recorder (VDR).
However, by far, not every ship is equipped with a VDR, and the readout is a manual procedure, 
which is why it is typically not used for the investigation of minor traffic incidents. \
Nevertheless, ships, ports and traffic services already dispose many sensors that generate valuable data for conflict resolution as well.
In practice, the records of foreign entities are usually not trusted, as they could be manipulated in favor of the respective recording party.
Therefore, in this paper, we present an approach for dynamically establishing a common basis of trust based on a Time Stamping Authority that signs
the recorded data of each party cryptographically. In this way, it is possible to verify af-terwards whether the recorded data have been manipulated.\
The approach was evaluated in a testbed during a real ship berthing maneuver. Overall, it was shown that with the presented system,
parties can dynamically agree on a mutually trusted TSA to have their data signed in a trustworthy way.

## Background
This repository contains the complete implementation of the paper "Decentralized Documentation of Maritime Traffic Incidents to support Conflict Resolution".
The implementation should serve as a basis for a better comprehensibility of the developed concept and the achieved results.
In the following, the implementation of the paper will be referred to as "DDOMTI" (Decentralized Documentation of Maritime Traffic Incidents).
 
## Quick Start
The application is divided into two parts. The P2P Network for realizing the communication between the participants and the DDOMTI with all other required components (Negotiation Protocol, Data Recorder, Recording Database).
1. Start the [P2P network](P2P/src/main/java/de/dlr/p2p/P2PApplication.java). Note, that it is important that each user has an individual peer name, 
which can be set in the [P2P application.properties](P2P/src/main/resources/application.properties).
2. Prepare the start of the [DDOMTI](DDOMTIA) by providing relationships which are defined in identity files. 
Those files can be found in the [identities directory](DDOMTIA/src/main/resources/identities).
The Identity files path has to be inserted into the [application.properties](DDOMTIA/src/main/resources/application.properties).
3. Configure the remaining components - the [P2P-Network](P2P/src/main/resources/application.properties), 
the [Private and Public Key Files](DDOMTIA/src/main/resources/application.properties), the [MongoDB](DDOMTIA/src/main/resources/application.properties), the to-be-documented [data source](DataClient/src/main/resources/application.properties) and the [P2P endpoint](Starter/src/main/resources/application.properties) of every participant.
4. Start the [DDOMTI](DDOMTIA/src/main/java/de/dlr/ddomtia/DDOMTI.java).

## TSA Negotiation
As describted in the paper an important part of the application is the TSA Negotiation.
To start the negotiation call the [start](DDOMTIA/src/main/java/de/dlr/ddomtia/controller/EventStarterController.java) 
method with a HTTP GET request.\
When all participants called their [start](DDOMTIA/src/main/java/de/dlr/ddomtia/controller/EventStarterController.java)
method the negotiation takes places. Note that the implementation 
waits for 3 participants by default (analogous to the evaluation scenario of the section "Application and Evaluation" in the paper).

## Decentralized Documentation
To document data the [document](DDOMTIA/src/main/java/de/dlr/ddomtia/controller/TSAController.java) 
method has to be called through a HTTP POST request. As body arguments, it takes the data itself and a name 
under which the data should be stored in the Mongo DB.

## Acknowledgement
This work has been funded by the German Federal Ministry of Transport and Digital Infrastructure (BMVI) within the funding
guideline “Innovative Hafentechnologien” (IHATEC) under the project SmartKai with the funding code 19H19008E
and the FuturePorts project of the German Aerospace Center (DLR). 