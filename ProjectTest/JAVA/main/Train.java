// Train/Driver.java

package projecttest.Train;

public class Driver
{
    /**
     * This testdriver wil test the difrent methods from train and trainstation
     */
    public static void test() {
        Train t1 = new Train("Aarhus", "Berlin", 999);
        Train t2 = new Train("Herning", "Copenhagen", 42);
        Train t3 = new Train("Aarhus", "Herning", 66);
        Train t4 = new Train("Odense", "Herning", 177);
        Train t5 = new Train("Aarhus", "Copenhagen", 122);

        System.out.println("Opgave 3:");
        System.out.println("*******************");
        System.out.println(t1);
        System.out.println(t2);
        System.out.println(t3);
        System.out.println(t4);
        System.out.println(t5);
        System.out.println("*******************");
        System.out.println("");

        TrainStation ts = new TrainStation("Herning");
        ts.addTrain(t1);
        ts.addTrain(t2);
        ts.addTrain(t3);
        ts.addTrain(t4);
        ts.addTrain(t5);

        System.out.println("Opgave 8:");
        System.out.println("*******************");
        System.out.println("No. of trains going from or to Herning:");
        System.out.println(ts.connectingTrains());
        System.out.println("*******************");
        System.out.println("");

        System.out.println("Opgave 9:");
        System.out.println("*******************");
        System.out.println("The cheapest train going to Copenhagen is going:");
        System.out.println(ts.cheapTrainTo("Copenhagen"));
        System.out.println("*******************");
        System.out.println("");

        System.out.println("Opgave 10:");
        System.out.println("*******************");
        ts.printTrainStation();
        System.out.println("*******************");
        System.out.println("");

        System.out.println("Opgave 11:");
        System.out.println("*******************");
        System.out.println("Trains going from Aarhus to Herning:");
        for(Train t : ts.trainsFrom("Aarhus")) {
            System.out.println(t);
        }
        System.out.println("*******************");
        System.out.println("");

        System.out.println("Opgave 12:");
        System.out.println("*******************");
        System.out.println("The cheapest train going from herning to Copenhagen:");
        System.out.println(ts.cheapTrain("Copenhagen"));
        System.out.println("*******************");
        System.out.println("");
    }
}


// Train/TrainStation.java

package projecttest.Train;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Comparator;

public class TrainStation
{
    private String city;
    private ArrayList<Train> trains;

    public TrainStation(String city)
    {
        this.city = city;
        trains = new ArrayList<Train>();
    }

    /**
     * this method adds trains tot the ArrayList trains
     */
    public void addTrain(Train t) {
        trains.add(t);
    }

    /**
     * Method will return the number of trains that starts or ends in the
     * city of the Trainstation
     */
    public int connectingTrains() {
        int result = 0;
        for(Train t : trains) {
            if(city.equals(t.getDeparture()) || city.equals(t.getDestiantion())) {
                result ++;
            }
        }
        return result;
    }

    /**
     * This method returns the chepest train go to a spesific destination
     */
    public Train cheapTrainTo(String destination) {
        Train result = null;
        for(Train t : trains) {
            if(destination.equals(t.getDestiantion())) {
                if(result == null || result.getPrice() > t.getPrice()) {
                    result = t;
                }
            }
        }
        return result;
    }

    /**
     * This method prints out all trains in the ArrayList trains
     * in sorted order after the departurs in alphabeticly order
     * if they have the same departures then it wil sort after price 
     * from lovest til highest
     */
    public void printTrainStation() {
        Collections.sort(trains);
        System.out.println("The trainstaion in " + city + " has following trains:");
        for(Train t : trains) {
            System.out.println(t);
        }
    }

    /**
     * This method will return all trains that starts in a given place
     * going to the Trainstations city.
     */
    public List<Train> trainsFrom(String departure) {
        return trains.stream()
        .filter(t -> t.getDeparture().equals(departure) && t.getDestiantion().equals(city))
        .collect(Collectors.toList());
    }

    /**
     * This method returns the cheapest train strating in the Trainstations 
     * city, and ends in a given destination
     */
    public Train cheapTrain(String destination) {
        return trains.stream()
        .filter(t -> t.getDeparture().equals(city) && t.getDestiantion().equals(destination))
        .min(Comparator.comparing(t -> t.getPrice()))
        .orElse(null);
    }
}


// Train/Train.java

package projecttest.Train;

import java.util.Collections;

public class Train implements Comparable<Train>
{
    private String departure;
    private String destination;
    private int price;

    public Train(String departure, String destination, int price)
    {
        this.departure = departure;
        this.destination = destination;
        this.price = price;
    }

    /**
     * A method to get the departure
     */
    public String getDeparture() {
        return departure;
    }

    /**
     * A method to get the destiantion
     */
    public String getDestiantion() {
        return destination;
    }

    /**
     * A method to get grice
     */
    public int getPrice() {
        return price;
    }

    /**
     * This method will format a String of the given way
     */
    public String toString() {
        return "From " + departure + " to " + destination + " for " + price + " DKK";
    }

    /**
     * This method sorts departures alphabeticly, if they have the same 
     * departures then it wil sort after price from lovest til highest
     */
    public int compareTo(Train other) {
        if(!departure.equals(other.departure)) {
            return departure.compareTo(other.departure);
        } else 
        {
            return price - other.price;
        }
    }
}


