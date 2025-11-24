# Evaluation lab - Node-RED

## Group number: 28

## Group members

- Francesco Genovese
- Federico Grandi
- Jonatan Sciaky

## Description of message flows

We have a router node that handles all messages coming from the Telegram receiver, and that has two outputs: one for query and report responses, and one for tracking responses.

For query messages, we parse the city and country, and forward them to the OpenWeather node.

For track messages, we retrieve and update the user preferences from the flow context. The preferences are mapped to each user using their `userId` from Telegram, and are represented as a `Set` of strings formatted as `CITY-COUNTRY` (which also prevents duplicates). After updating the context, we send a simple response message to the user.

For report messages, we retrieve the sender's preferences and generate as many messages as the number of locations they're tracking, each with their respective location info. We then send those messages to the OpenWeather node re-using the part of the flow in charge of queries, since report responses are formatted as normal query responses.

All responses are generated using the text node, which handles chat references automatically.

We're storing the preferences in the flow context so that they can also be accessed for manual logging outside of the router node; if not for this reason, the node context would suffice.

## Extensions 

- node-red-contrib-chatbot
- node-red-node-openweathermap


