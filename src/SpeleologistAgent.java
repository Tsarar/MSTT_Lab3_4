import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.Objects;

public class SpeleologistAgent extends Agent {

    public static int LOOK_RIGHT = 0;
    public static int LOOK_LEFT = 1;
    public static int LOOK_UP = 2;
    public static int LOOK_DOWN = 3;
    public static int MOVE = 4;
    public static int SHOOT_ARROW = 5;
    public static int TAKE_GOLD = 6;
    public static java.util.HashMap<Integer, String> actionCodes = new java.util.HashMap<>() {{
        put(LOOK_RIGHT, "right");
        put(LOOK_LEFT, "left");
        put(LOOK_UP, "up");
        put(LOOK_DOWN, "down");
        put(MOVE, "move");
        put(SHOOT_ARROW, "shoot");
        put(TAKE_GOLD, "take");
    }};

    public static String GO_INSIDE = "go_inside";
    public static String WUMPUS_WORLD_TYPE = "wumpus-world";
    public static String NAVIGATOR_AGENT_TYPE = "navigator-agent";

    public static String WORLD_DIGGER_CONVERSATION_ID = "digger-world";
    public static String NAVIGATOR_DIGGER_CONVERSATION_ID = "digger-navigator";

    private AID wumpusWorld;
    private AID navigationAgent;
    private String currentWorldState = "";

    @Override
    protected void setup() {
        addBehaviour(new WumpusWorldFinder());
    }

    private class WumpusWorldFinder extends Behaviour {
        private int step = 0;
        @Override
        public void action() {
            if (step == 0){
                // Find wumpus world agent
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType(WUMPUS_WORLD_TYPE);
                template.addServices(sd);
                try {
                    DFAgentDescription[] result = DFService.search(myAgent, template);
                    if (result != null && result.length > 0) {
                        wumpusWorld = result[0].getName();
                        myAgent.addBehaviour(new WumpusWorldPerformer());
                        step = 1;
                    } else {
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (FIPAException e) {
                    e.printStackTrace();
                }
            }
        }
        @Override
        public boolean done() {
            return step == 1;
        }
    }
    private class WumpusWorldPerformer extends Behaviour {
        private MessageTemplate mt;

        private int step = 0;
        @Override
        public void action() {
            switch (step) {
                // Send start message and go into program cycle
                case 0:
                    // Call for proposal
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);

                    cfp.addReceiver(wumpusWorld);
                    cfp.setContent(GO_INSIDE);
                    cfp.setConversationId(WORLD_DIGGER_CONVERSATION_ID);
                    cfp.setReplyWith("cfp"+System.currentTimeMillis());

                    myAgent.send(cfp);
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId(WORLD_DIGGER_CONVERSATION_ID),
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                    step = 1;
                    break;
                case 1:
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.CONFIRM) {
                            currentWorldState = reply.getContent();
                            myAgent.addBehaviour(new NavigatorAgentPerformer());
                            step = 2;
                        }
                    }
                    else {
                        block();
                    }
                    break;
            }
        }
        @Override
        public boolean done() {
            return step == 2;
        }
    }

    private class NavigatorAgentPerformer extends Behaviour {
        private int step = 0;
        private MessageTemplate mt;
        @Override
        public void action() {
            switch (step) {
                // Setup communication with navigation agent
                case 0: {
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType(NAVIGATOR_AGENT_TYPE);
                    template.addServices(sd);
                    try {
                        DFAgentDescription[] result = DFService.search(myAgent, template);
                        if (result != null && result.length > 0) {
                            navigationAgent = result[0].getName();
                            step = 1;
                        } else {
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    } catch (FIPAException e) {
                        e.printStackTrace();
                    }
                }
                case 1: {
                    // Send current world state to navigator agent
                    ACLMessage order = new ACLMessage(ACLMessage.INFORM);

                    order.addReceiver(navigationAgent);
                    order.setContent(currentWorldState);
                    order.setConversationId(NAVIGATOR_DIGGER_CONVERSATION_ID);
                    order.setReplyWith("order"+System.currentTimeMillis());

                    myAgent.send(order);

                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId(NAVIGATOR_DIGGER_CONVERSATION_ID),
                            MessageTemplate.MatchInReplyTo(order.getReplyWith()));

                    step = 2;
                }
                case 2: {
                    // Handle navigator response
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.PROPOSE) {
                            String actions = reply.getContent();
                            actions = actions.substring(1, actions.length()-1);
                            String[] instructions = actions.split(", ");
                            if (instructions.length == 1){
                                sendTakeGoldMessage();
                            }
                            else if (instructions.length == 2 && Objects.equals(instructions[1], actionCodes.get(SHOOT_ARROW))){
                                sendShootMessage(instructions[0]);
                            }
                            else if (instructions.length == 2 && Objects.equals(instructions[1], actionCodes.get(MOVE))){
                                sendMoveMessage(instructions[0]);
                            }
                            else {
                                System.out.println("ERROR ACTIONS");
                            }
                            step = 3;
                        }
                    }
                    else {
                        block();
                    }
                    break;

                }
                case 3:
                    // Get new world state and start again
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        currentWorldState = reply.getContent();
                        step = 1;
                    }
                    else {
                        block();
                    }
                    break;
            }
        }
        @Override
        public boolean done() {
            return step == 4;
        }

        private void sendShootMessage(String instruction) {
            ACLMessage order = new ACLMessage(SHOOT_ARROW);
            order.addReceiver(wumpusWorld);

            order.setContent(instruction);
            order.setConversationId(NAVIGATOR_DIGGER_CONVERSATION_ID);
            order.setReplyWith("order"+System.currentTimeMillis());

            myAgent.send(order);

            mt = MessageTemplate.and(MessageTemplate.MatchConversationId(NAVIGATOR_DIGGER_CONVERSATION_ID),
                    MessageTemplate.MatchInReplyTo(order.getReplyWith()));
        }

        private void sendTakeGoldMessage() {
            ACLMessage order = new ACLMessage(TAKE_GOLD);
            order.addReceiver(wumpusWorld);

            order.setContent("Take");
            order.setConversationId(NAVIGATOR_DIGGER_CONVERSATION_ID);
            order.setReplyWith("order"+System.currentTimeMillis());

            myAgent.send(order);
            mt = MessageTemplate.and(MessageTemplate.MatchConversationId(NAVIGATOR_DIGGER_CONVERSATION_ID),
                    MessageTemplate.MatchInReplyTo(order.getReplyWith()));
        }

        private void sendMoveMessage(String instruction) {
            ACLMessage order = new ACLMessage(MOVE);
            order.addReceiver(wumpusWorld);

            order.setContent(instruction);
            order.setConversationId(NAVIGATOR_DIGGER_CONVERSATION_ID);
            order.setReplyWith("order"+System.currentTimeMillis());

            myAgent.send(order);

            mt = MessageTemplate.and(MessageTemplate.MatchConversationId(NAVIGATOR_DIGGER_CONVERSATION_ID),
                    MessageTemplate.MatchInReplyTo(order.getReplyWith()));
        }
    }
}