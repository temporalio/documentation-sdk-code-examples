package helloworkflow;

public class GreetActivitiesImpl implements GreetActivities {

    @Override
    public String greet(String name) {
      return "Hello " + name;
    }

}
