package ibm.gse.eda.perf.consumer.app.dto;

public class Control {
    public String topic;
    public String offsetPolicy; 
    public boolean commit;
    public String timeStamps;

    public String order;  // START, STOP 
    public int numberOfConsumers;

    public Control(){}

}