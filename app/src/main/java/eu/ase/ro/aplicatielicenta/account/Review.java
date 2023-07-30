package eu.ase.ro.aplicatielicenta.account;

public class Review {
    private float rating;
    private String startPoint;
    private String destinationPoint;
    private String id;

    public Review() {
    }

    public Review(float rating, String startPoint, String destinationPoint) {
        this.rating = rating;
        this.startPoint = startPoint;
        this.destinationPoint = destinationPoint;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public String getStartPoint() {
        return startPoint;
    }

    public void setStartPoint(String startPoint) {
        this.startPoint = startPoint;
    }

    public String getDestinationPoint() {
        return destinationPoint;
    }

    public void setDestinationPoint(String destinationPoint) {
        this.destinationPoint = destinationPoint;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
