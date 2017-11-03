package template;

/**
 * Created by lorenzotara on 03/11/17.
 */
public class ActionTimes {

    public int pickUpTime;
    public int deliveryTime;

    public ActionTimes(ActionTimes value) {

        this.pickUpTime = value.pickUpTime;
        this.deliveryTime = value.deliveryTime;
    }

    public ActionTimes(int pickUpTime, int deliveryTime) {
        this.pickUpTime = pickUpTime;
        this.deliveryTime = deliveryTime;
    }
}
