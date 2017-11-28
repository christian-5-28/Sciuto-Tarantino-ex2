package template;

/**
 * ActionTimes class: it models the pickUp and delivery times.
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
