# DAI-MOM system
# System Overview

The DAI system developed with RMI (Java) technology is based on a MOM structure, featuring its own Broker along with multiple consumers and producers, while incorporating a series of AI agents that enable intelligent and automatic message filtering before they reach the consumers. Messages that do not meet the established criteria are discarded and are not published to the message queues of the designated channel, ensuring that only valid and secure messages reach the consumers.

The system allows the producer to send durable or non-durable messages after a Broker crash (non-durable messages being removed from the queue after 5 minutes of inactivity by the MOM), with messages being filtered by agents before consumers receive them or not, list queues, create new queues (with the option to decide whether they should be durable or not), delete queues, and view information about each of the available queues.

The consumer can choose which channel to consume messages from and therefore see what messages are sent by producers, with the option to perform message acknowledgment either automatically or manually (in which case they can reject it, returning it to its respective message queue). Additionally, if multiple consumers are subscribed to the same queue, messages will be distributed in round-robin fashion (a fair dispatch policy), meaning each message will be sequentially assigned to a different consumer in a cyclical manner, ensuring that each consumer receives approximately the same number of messages and that each message is processed by a single consumer.

Finally, the agents will filter the different messages sent by consumers and decide whether they should be discarded or not according to their respective discard criteria. Furthermore, the system is completely extensible and allows for the creation of custom agents for any specific business logic.

# Testing and Automatic Execution of the MOM

If we want to automatically execute only the Broker, we should run the script `./testinitBrokerAgents.sh`, which will execute the TestAgentsProducer.java file, attempting to send a series of messages, some of which will be rejected and others will not.

# Manual Compilation and Execution of the Project

1. Open a terminal and navigate to the project directory
2. Compile the .java files: `javac *.java`
3. From one terminal, run the MessageBroker:
   a) Start the remote registry for the MessageBroker in that terminal: `rmiregistry &`
   b) Execute the Broker: `java MessageBrokerImpl [urlMessageBroker]`
      - `[urlMessageBroker]` → `rmi://localhost/MessageBroker`
4. In another terminal, run the Consumer:
   a) Start the remote registry for the Consumer in that terminal: `rmiregistry &`
   b) Execute the Consumer: `java ConsumerImpl [urlMessageBroker] [urlConsumer]`
      - `[urlConsumer]` → `rmi://localhost/Consumer`
5. In the last terminal, run the Producer:
   a) Start the remote registry for the Producer in that terminal: `rmiregistry &`
   b) Execute the Producer: `java ProducerImpl [urlMessageBroker] [urlProducer]`
      - `[urlProducer]` → `rmi://localhost/Producer`

**Note:** If virtual machines were used, it would work the same way, except:
- URLs format: `rmi://[IP_address]:[portFile]/[file]`
- Required command on each machine: `rmiregistry [portFile] &`
