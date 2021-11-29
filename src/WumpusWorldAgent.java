import Model.Coordinates;
import Model.NavigationConstants;
import Model.Room;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.*;

public class WumpusWorldAgent extends Agent {

    private static final int NUMBER_OF_ROWS = 4;
    private static final int NUMBER_OF_COLUMNS = 4;

    public static String SERVICE_DESCRIPTION = "WUMPUS-WORLD";
    private static final int START = -1;
    private static final int WUMPUS = 1;
    private static final int PIT = 2;
    private static final int BREEZE = 3;
    private static final int STENCH = 4;
    private static final int SCREAM = 5;
    private static final int GOLD = 6;
    private static final int BUMP = 7;

    public static HashMap<Integer, String> roomCodes = new HashMap<>() {{
        put(START, NavigationConstants.START);
        put(WUMPUS, NavigationConstants.WUMPUS);
        put(PIT, NavigationConstants.PIT);
        put(BREEZE, NavigationConstants.BREEZE);
        put(STENCH, NavigationConstants.STENCH);
        put(SCREAM, NavigationConstants.SCREAM);
        put(GOLD, NavigationConstants.GOLD);
        put(BUMP, NavigationConstants.BUMP);
    }};

    private Room[][] wumpusMap;
    private HashMap<AID, Coordinates> Speleologists;

    String nickname = "WumpusWorld";
    AID id = new AID(nickname, AID.ISLOCALNAME);

    @Override
    protected void setup() {
        System.out.println("Hello! WumpusWorld-agent " + getAID().getName() + " is ready.");
        Speleologists = new HashMap<>();

        generateMap();
        showMap();

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());

        ServiceDescription sd = new ServiceDescription();
        sd.setType(SpeleologistAgent.WUMPUS_WORLD_TYPE);
        sd.setName(SERVICE_DESCRIPTION);

        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }

        addBehaviour(new SpeleologistConnectPerformer());
        addBehaviour(new SpeleologistArrowPerformer());
        addBehaviour(new SpeleologistGoldPerformer());
        addBehaviour(new SpeleologistMovePerformer());
    }

    private void generateMap() {
        // _  B   P  B
        // S  _   B  _
        // WS BSG P  B
        // S  _   B  P

        this.wumpusMap = new Room[NUMBER_OF_ROWS][NUMBER_OF_COLUMNS];
        this.wumpusMap[0][1] = new Room(WumpusWorldAgent.roomCodes, BREEZE);
        this.wumpusMap[0][2] = new Room(WumpusWorldAgent.roomCodes, PIT);
        this.wumpusMap[0][3] = new Room(WumpusWorldAgent.roomCodes, BREEZE);
        this.wumpusMap[1][0] = new Room(WumpusWorldAgent.roomCodes, STENCH);
        this.wumpusMap[1][2] = new Room(WumpusWorldAgent.roomCodes, BREEZE);
        this.wumpusMap[2][0] = new Room(WumpusWorldAgent.roomCodes, WUMPUS, STENCH);
        this.wumpusMap[2][1] = new Room(WumpusWorldAgent.roomCodes, BREEZE, STENCH, GOLD);
        this.wumpusMap[2][2] = new Room(WumpusWorldAgent.roomCodes, PIT);
        this.wumpusMap[2][3] = new Room(WumpusWorldAgent.roomCodes, BREEZE);
        this.wumpusMap[3][0] = new Room(WumpusWorldAgent.roomCodes, STENCH);
        this.wumpusMap[3][2] = new Room(WumpusWorldAgent.roomCodes, BREEZE);
        this.wumpusMap[3][3] = new Room(WumpusWorldAgent.roomCodes, PIT);

        // Set empty rooms
        for (int i = 0; i < this.wumpusMap.length; i++) {
            for (int j = 0; j < this.wumpusMap[i].length; j++) {
                if (this.wumpusMap[i][j] == null) {
                    this.wumpusMap[i][j] = new Room(WumpusWorldAgent.roomCodes);
                }
            }

        }

    }

    private void showMap() {
        for (int i = 0; i < this.wumpusMap.length; i++){
            for (int j = 0; j < this.wumpusMap[i].length; j++){
                System.out.println("POSITION: " + i + ", " + j + "; MARKERS: " + wumpusMap[i][j].Events);
            }

        }
    }

    private class SpeleologistConnectPerformer extends CyclicBehaviour {
        public void action() {
            // Call for proposal
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                String message = msg.getContent();
                if (Objects.equals(message, SpeleologistAgent.GO_INSIDE)){
                    AID current_Speleologist = msg.getSender();
                    Coordinates Speleologist_coords = Speleologists.get(current_Speleologist);

                    if (Speleologist_coords == null){
                        Speleologists.put(current_Speleologist, new Coordinates(0, 0));
                    }

                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.CONFIRM);
                    reply.setContent(wumpusMap[0][0].Events.toString());
                    myAgent.send(reply);
                }
            }
            else {
                block();
            }
        }
    }

    private class SpeleologistArrowPerformer extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(SpeleologistAgent.SHOOT_ARROW);
            ACLMessage msg = myAgent.receive(mt);

            if (msg != null) {
                ACLMessage reply = msg.createReply();
                reply.setPerformative(SpeleologistAgent.SHOOT_ARROW);

                String message = msg.getContent();
                AID current_Speleologist = msg.getSender();
                Coordinates Speleologist_coords = Speleologists.get(current_Speleologist);

                int row = Speleologist_coords.row;
                int column = Speleologist_coords.column;
                String answer = "";
                if (message.equals(SpeleologistAgent.actionCodes.get(SpeleologistAgent.LOOK_DOWN))){
                    for (int i = 0; i < row; ++i){
                        if (wumpusMap[i][column].Events.contains(WumpusWorldAgent.roomCodes.get(WUMPUS))){
                            answer = NavigationConstants.SCREAM;
                        }
                    }
                }
                else if(message.equals(SpeleologistAgent.actionCodes.get(SpeleologistAgent.LOOK_UP))){
                    for (int i = row+1; i < NUMBER_OF_ROWS; ++i){
                        if (wumpusMap[i][column].Events.contains(WumpusWorldAgent.roomCodes.get(WUMPUS))){
                            answer = NavigationConstants.SCREAM;
                        }
                    }
                }
                else if(message.equals(SpeleologistAgent.actionCodes.get(SpeleologistAgent.LOOK_LEFT))){
                    for (int i = 0; i < column; ++i){
                        if (wumpusMap[row][i].Events.contains(WumpusWorldAgent.roomCodes.get(WUMPUS))){
                            answer = NavigationConstants.SCREAM;
                        }
                    }
                }
                else if (message.equals(SpeleologistAgent.actionCodes.get(SpeleologistAgent.LOOK_RIGHT))){
                    for (int i = column+1; i < NUMBER_OF_COLUMNS; ++i){
                        if (wumpusMap[row][i].Events.contains(WumpusWorldAgent.roomCodes.get(WUMPUS))){
                            answer = NavigationConstants.SCREAM;
                        }
                    }
                }

                reply.setContent(answer);

                myAgent.send(reply);
            }
            else {
                block();
            }
        }
    }

    private class SpeleologistMovePerformer extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(SpeleologistAgent.MOVE);
            ACLMessage msg = myAgent.receive(mt);

            if (msg != null) {
                ACLMessage reply = msg.createReply();
                reply.setPerformative(SpeleologistAgent.MOVE);

                String message = msg.getContent();
                AID current_Speleologist = msg.getSender();
                Coordinates Speleologist_coords = Speleologists.get(current_Speleologist);

                if (Speleologist_coords == null){
                    Speleologists.put(current_Speleologist, new Coordinates(0, 0));
                    Speleologist_coords = Speleologists.get(current_Speleologist);
                }

                System.out.println("World say: Current agent coordinates: " +
                        Speleologist_coords.row + " | " +
                        Speleologist_coords.column);

                int row = Speleologist_coords.row;
                int column = Speleologist_coords.column;
                if (message.equals(SpeleologistAgent.actionCodes.get(SpeleologistAgent.LOOK_DOWN))){
                    row -= 1;
                }
                else if(message.equals(SpeleologistAgent.actionCodes.get(SpeleologistAgent.LOOK_UP))){
                    row += 1;
                }
                else if(message.equals(SpeleologistAgent.actionCodes.get(SpeleologistAgent.LOOK_LEFT))){
                    column -=1;
                }
                else if (message.equals(SpeleologistAgent.actionCodes.get(SpeleologistAgent.LOOK_RIGHT))){
                    column += 1;
                }

                if (row > -1 && column > -1 && row < NUMBER_OF_ROWS && column < NUMBER_OF_COLUMNS){
                    Speleologist_coords.column = column;
                    Speleologist_coords.row = row;
                    reply.setContent(wumpusMap[row][column].Events.toString());
                }
                else {
                    System.out.println("World say: Out of bounds: row = " +
                            row + " | column: " +
                            column);
                    reply.setContent(String.valueOf(new ArrayList<String>(){{
                        add(NavigationConstants.BUMP);
                    }}));
                }
                myAgent.send(reply);
            }
            else {
                block();
            }
        }
    }

    private class SpeleologistGoldPerformer extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(SpeleologistAgent.TAKE_GOLD);
            ACLMessage msg = myAgent.receive(mt);

            if (msg != null) {
                AID current_Speleologist = msg.getSender();
                Coordinates Speleologist_coords = Speleologists.get(current_Speleologist);
                if (Speleologist_coords == null){
                    Speleologists.put(current_Speleologist, new Coordinates(0, 0));
                }
                else {
                    if (wumpusMap[Speleologist_coords.row][Speleologist_coords.column]
                            .Events.contains(WumpusWorldAgent.roomCodes.get(GOLD))){
                        ACLMessage reply = msg.createReply();
                        reply.setPerformative(SpeleologistAgent.TAKE_GOLD);
                        reply.setContent("GOLD");
                        myAgent.send(reply);
                    }
                }
            }
            else {
                block();
            }
        }
    }
}