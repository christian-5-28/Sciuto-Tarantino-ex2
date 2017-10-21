package mainPack;

import logist.topology.Topology.City;

import java.util.ArrayList;
import java.util.List;


public class State {

    private City currentCity;
    private City taskDestination;

    private List<Action> validActionList;


    public State(City currentCity, City possibleDestination) {
        this.currentCity = currentCity;
        this.taskDestination = possibleDestination;

        initializeValidActionList();
    }

    private void initializeValidActionList() {

        validActionList = new ArrayList<>();

        for (City city : currentCity.neighbors()) {

            validActionList.add(new Action(city));
            
        }

        if(taskDestination != null && !currentCity.hasNeighbor(taskDestination)){

            validActionList.add(new Action(taskDestination));
        }
    }


    public City getCurrentCity() {
        return currentCity;
    }

    public City getTaskDestination() {
        return taskDestination;
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
