import Model.Position;
import Model.UnknownRoom;
import Model.UnknownWumpusWorld;
import Model.NavigationConstants;
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

// This navigation algorithm is just a start
// It should be extended with "take gold" and "shoot arrow" commands
// But the messaging part, the focus of this task, is implemented fully

public class NavigatorAgent extends Agent {
    private static final String SERVICE_DESCRIPTION = "NAVIGATOR_AGENT";
    String nickname = "NavigatorAgent";
    AID id = new AID(nickname, AID.ISLOCALNAME);
    private Hashtable<AID, Position> agents_coordinates;
    private Hashtable<AID, LinkedList<int[]>> agentsPath;

    private boolean moveRoom = false;
    private int agentX;
    private int agentY;

    UnknownWumpusWorld world;

    int time = 0;

    @Override
    protected void setup() {
        world = new UnknownWumpusWorld();
        agentsPath = new Hashtable<>();
        agents_coordinates = new Hashtable<>();

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType(SpeleologistAgent.NAVIGATOR_AGENT_TYPE);
        sd.setName(SERVICE_DESCRIPTION);
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        addBehaviour(new LocationRequestsServer());
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println("Navigator-agent " + getAID().getName() + " terminating.");
    }

    private class LocationRequestsServer extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                AID request_agent = msg.getSender();
                if (agentsPath.get(request_agent) == null) {
                    LinkedList<int[]> agentWay = new LinkedList<>();
                    agentsPath.put(request_agent, agentWay);
                }
                Position request_agent_position = agents_coordinates.get(request_agent);
                if (request_agent_position == null) {
                    request_agent_position = new Position();
                    agents_coordinates.put(request_agent, request_agent_position);
                }
                String location = msg.getContent();
                location = location.substring(1, location.length() - 1);
                String[] room_info = location.split(", ");
                System.out.println("ROOM INFO: " + Arrays.toString(room_info));
                System.out.println("AGENT INFO: " + request_agent_position.getX() + " " + request_agent_position.getY());
                String[] actions = get_actions(request_agent, request_agent_position, room_info);
                ACLMessage reply = msg.createReply();

                reply.setPerformative(ACLMessage.PROPOSE);
                reply.setContent(Arrays.toString(actions));
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }

    private String[] get_actions(AID request_agent, Position request_agent_position, String[] room_info) {
        System.out.println("Agent pos before: " + request_agent_position.getX() + " | " + request_agent_position.getY());
        int[] actions;
        UnknownRoom checking_room = world.getWorldGrid().get(request_agent_position);
        if (checking_room == null) {
            checking_room = new UnknownRoom();
            world.getWorldGrid().put(request_agent_position, checking_room);
        }

        if (!Arrays.asList(room_info).contains(NavigationConstants.BUMP)) {
            LinkedList<int[]> agentStory = agentsPath.get(request_agent);
            agentStory.add(new int[]{request_agent_position.getX(), request_agent_position.getY()});
            request_agent_position.setX(agentX);
            request_agent_position.setY(agentY);
            //if (world.getWorldGrid().get(request_agent_position).getExist() != NavigationConstants.ROOM_STATUS_TRUE) {
            //    world.getWorldGrid().get(request_agent_position).setExist(NavigationConstants.ROOM_STATUS_TRUE);
            //    System.out.println("MARKED THE EXISTENT POSITION");
            //}
            moveRoom = false;
        } else {
            Position helpPosition = new Position(agentX, agentY);
            world.getWorldGrid().get(helpPosition).setExist(NavigationConstants.ROOM_STATUS_FALSE);
        }
        checking_room = world.getWorldGrid().get(request_agent_position);
        if (checking_room == null) {
            checking_room = new UnknownRoom();
            world.getWorldGrid().put(request_agent_position, checking_room);
        }

        if (checking_room.getOk() != NavigationConstants.ROOM_STATUS_TRUE) {
            checking_room.setOk(NavigationConstants.ROOM_STATUS_TRUE);
        }
        for (String event : room_info) {
            checking_room.addEvent(event);
        }
        updateNeighbors(request_agent_position);
        if (world.isWumpusAlive() && world.getWumpusRoomCount() > 2) {
            Position wumpusPosition = world.getWumpusCoordinates();
            actions = getNextRoomAction(request_agent_position, wumpusPosition, SpeleologistAgent.SHOOT_ARROW);
        } else {
            Position[] nextOkRooms = getOkNeighbors(request_agent, request_agent_position);
            int best_candidate = -1;
            int candidate_status = -1;
            for (int i = 0; i < nextOkRooms.length; ++i) {
                Position candidate_room = nextOkRooms[i];
                //System.out.println("CANDIDATE CHECKING: " + candidate_room.getX() + " " + candidate_room.getY());
                //System.out.println("AGENT CHECKING: " + request_agent_position.getX() + " " + request_agent_position.getY());
                if (candidate_room.getX() > request_agent_position.getX()) { // right
                    best_candidate = i;
                    //System.out.println("1");
                    break;
                } else if (candidate_room.getY() > request_agent_position.getY()) { // top
                    if (candidate_status < 3) {
                        //System.out.println("2");
                        candidate_status = 3;
                    } else continue;
                } else if (candidate_room.getX() < request_agent_position.getX()) { // left
                    if (candidate_status < 2) {
                        //System.out.println("3");
                        candidate_status = 2;
                    } else continue;
                } else {
                    if (candidate_status < 1) { // bottom
                        System.out.println("4");
                        candidate_status = 1;
                    } else continue;
                }
                best_candidate = i;
            }
            //System.out.println("OK ROOMS COUNT IS: " + nextOkRooms.length);
            //System.out.println("ADVICE POSITION IS: " + nextOkRooms[best_candidate].getX() + " | " + nextOkRooms[best_candidate].getY());
            actions = getNextRoomAction(request_agent_position, nextOkRooms[best_candidate], SpeleologistAgent.MOVE);
            //System.out.println("ADVICE ACTIONS IS: " + Arrays.toString(actions));
        }

        switch (time) {
            case 0 -> {
                actions = new int[]{SpeleologistAgent.LOOK_RIGHT, SpeleologistAgent.MOVE};
                time++;
            }
            case 1,2 -> {
                actions = new int[]{SpeleologistAgent.LOOK_UP, SpeleologistAgent.MOVE};
                time++;
            }
            case 3 -> {
                actions = new int[]{SpeleologistAgent.TAKE_GOLD};
                time++;
            }
            case 4 -> {
                actions = new int[]{SpeleologistAgent.LOOK_LEFT, SpeleologistAgent.SHOOT_ARROW};
                time++;
            }
            case 5 -> {
                actions = new int[]{SpeleologistAgent.LOOK_LEFT, SpeleologistAgent.MOVE};
                time++;
            }
            case 6,7 -> {
                actions = new int[]{SpeleologistAgent.LOOK_DOWN, SpeleologistAgent.MOVE};
                time++;
            }
            case 8 -> {
                actions = new int[]{SpeleologistAgent.GET_OUT};
                time++;
            }
        }

        String[] language_actions = new String[actions.length];
        for (int i = 0; i < actions.length; ++i) {
            language_actions[i] = SpeleologistAgent.actionCodes.get(actions[i]);
        }
        return language_actions;
    }

    private int[] getNextRoomAction(Position request_agent_position, Position nextOkRoom, int action) {
        agentX = request_agent_position.getX();
        agentY = request_agent_position.getY();
        int look;
        if (request_agent_position.getY() < nextOkRoom.getY()) {
            agentY += 1;
            look = SpeleologistAgent.LOOK_UP;
        } else if (request_agent_position.getY() > nextOkRoom.getY()) {
            agentY -= 1;
            look = SpeleologistAgent.LOOK_DOWN;
        } else if (request_agent_position.getX() < nextOkRoom.getX()) {
            agentX += 1;
            look = SpeleologistAgent.LOOK_RIGHT;
        } else {
            agentX -= 1;
            look = SpeleologistAgent.LOOK_LEFT;
        }
        moveRoom = true;

        return new int[]{look, action};
    }

    private Position[] getOkNeighbors(AID request_agent, Position request_agent_position) {
        Position[] okNeighbors = getNeighborsPosition(request_agent_position);
        ArrayList<Position> okPositions = new ArrayList<>();
        for (Position position : okNeighbors) {
            this.world.getWorldGrid().putIfAbsent(position, new UnknownRoom());
            // if there is no room
            // we are adding a new room
            if ((this.world.getWorldGrid().get(position).getOk() == NavigationConstants.ROOM_STATUS_TRUE
                    && this.world.getWorldGrid().get(position).getNoWay() != NavigationConstants.ROOM_STATUS_TRUE
                    && this.world.getWorldGrid().get(position).getExist() != NavigationConstants.ROOM_STATUS_FALSE
            ) ||
                    this.world.getWorldGrid().get(position).getOk() == NavigationConstants.ROOM_STATUS_NO_STATUS) {
                okPositions.add(position);
            }
        }
        if (okPositions.size() == 0) {
            int x = agentsPath.get(request_agent).getLast()[0];
            int y = agentsPath.get(request_agent).getLast()[1];
            okPositions.add(new Position(x, y));
            this.world.getWorldGrid().get(request_agent_position).setNoWay(NavigationConstants.ROOM_STATUS_TRUE);
        }
        return okPositions.toArray(new Position[0]);
    }

    private UnknownRoom[] getNeighborsImaginaryRoom(Position request_agent_position) {
        Position rightNeighbor = new Position(request_agent_position.getX() + 1, request_agent_position.getY());
        Position upNeighbor = new Position(request_agent_position.getX(), request_agent_position.getY() + 1);
        Position leftNeighbor = new Position(request_agent_position.getX() - 1, request_agent_position.getY());
        Position bottomNeighbor = new Position(request_agent_position.getX(), request_agent_position.getY() - 1);
        UnknownRoom rightRoom = world.getWorldGrid().get(rightNeighbor);
        if (rightRoom == null) {
            rightRoom = new UnknownRoom();
            world.getWorldGrid().put(rightNeighbor, rightRoom);
        }
        UnknownRoom upRoom = world.getWorldGrid().get(upNeighbor);
        if (upRoom == null) {
            upRoom = new UnknownRoom();
            world.getWorldGrid().put(rightNeighbor, upRoom);
        }
        UnknownRoom leftRoom = world.getWorldGrid().get(leftNeighbor);
        if (leftRoom == null) {
            leftRoom = new UnknownRoom();
            world.getWorldGrid().put(rightNeighbor, leftRoom);
        }
        UnknownRoom bottomRoom = world.getWorldGrid().get(bottomNeighbor);
        if (bottomRoom == null) {
            bottomRoom = new UnknownRoom();
            world.getWorldGrid().put(rightNeighbor, bottomRoom);
        }
        UnknownRoom[] rooms = new UnknownRoom[]{rightRoom, upRoom, leftRoom, bottomRoom};
        return rooms;
    }

    private Position[] getNeighborsPosition(Position request_agent_position) {
        Position rightNeighbor = new Position(request_agent_position.getX() + 1, request_agent_position.getY());
        Position upNeighbor = new Position(request_agent_position.getX(), request_agent_position.getY() + 1);
        Position leftNeighbor = new Position(request_agent_position.getX() - 1, request_agent_position.getY());
        Position bottomNeighbor = new Position(request_agent_position.getX(), request_agent_position.getY() - 1);
        ;
        return new Position[]{rightNeighbor, upNeighbor, leftNeighbor, bottomNeighbor};
    }

    private void updateNeighbors(Position request_agent_position) {
        UnknownRoom currentRoom = world.getWorldGrid().get(request_agent_position);
        UnknownRoom[] roomList = getNeighborsImaginaryRoom(request_agent_position);

        if (currentRoom.getStench() == NavigationConstants.ROOM_STATUS_TRUE) {
            world.setWumpusRoomCount(world.getWumpusRoomCount() + 1);
            for (UnknownRoom room : roomList) {
                if (room.getWumpus() == NavigationConstants.ROOM_STATUS_NO_STATUS) {
                    room.setOk(NavigationConstants.ROOM_STATUS_POSSIBLE);
                    room.setWumpus(NavigationConstants.ROOM_STATUS_POSSIBLE);
                }
            }
        }
        if (currentRoom.getBreeze() == NavigationConstants.ROOM_STATUS_TRUE) {
            for (UnknownRoom room : roomList) {
                if (room.getPit() == NavigationConstants.ROOM_STATUS_NO_STATUS) {
                    room.setOk(NavigationConstants.ROOM_STATUS_POSSIBLE);
                    room.setPit(NavigationConstants.ROOM_STATUS_POSSIBLE);
                }
            }
        }
        if (currentRoom.getBreeze() == NavigationConstants.ROOM_STATUS_FALSE &&
            currentRoom.getStench() == NavigationConstants.ROOM_STATUS_FALSE) {
            for (UnknownRoom room : roomList) {
                room.setOk(NavigationConstants.ROOM_STATUS_TRUE);
                room.setWumpus(NavigationConstants.ROOM_STATUS_FALSE);
                room.setPit(NavigationConstants.ROOM_STATUS_FALSE);
            }
        }
    }
}