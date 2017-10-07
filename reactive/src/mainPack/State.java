package mainPack;

import logist.topology.Topology.City;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lorenzotara on 05/10/17.
 */
public class State {

    private City startingCity;
    private City destination;

    private List<Action> validActionList;


    public State(City startingCity, City possibleDestination) {
        this.startingCity = startingCity;
        this.destination = possibleDestination;

        initializeValidActionList();
    }

    private void initializeValidActionList() {

        validActionList = new ArrayList<>();

        for (City city : startingCity.neighbors()) {

            validActionList.add(new Action(city));
            
        }

        if(destination != null){

            validActionList.add(new Action(destination));
        }

        //TODO:elimina duplicati, i ottengono se destination è anche neighbourn di starting city
    }


    public City getStartingCity() {
        return startingCity;
    }

    public City getDestination() {
        return destination;
    }

    public List<Action> getValidActionList(){

        return new ArrayList<>(validActionList);
    }

    public boolean actionIsValid(Action action){

        for (Action action1 : validActionList) {

            if(action.getNextCity().id == action1.getNextCity().id){
                return true;
            }
        }

        return false;

    }

}
