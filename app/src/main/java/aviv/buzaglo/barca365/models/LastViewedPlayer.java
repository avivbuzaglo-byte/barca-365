package aviv.buzaglo.barca365.models;

public class LastViewedPlayer {
    private int id;
    private int shirtNumber;
    private String name;
    private String position;
    private String imageUrl; // נשמור גם את ה-URL כדי לחסוך חישובים בשליפה

    // חובה: בנאי ריק עבור Firebase
    public LastViewedPlayer() { }

    public LastViewedPlayer(int id, String name, String position, int shirtNumber, String imageUrl) {
        this.id = id;
        this.name = name;
        this.position = position;
        this.shirtNumber = shirtNumber;
        this.imageUrl = imageUrl;
    }

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getPosition() { return position; }
    public String getImageUrl() { return imageUrl; }
    public int getShirtNumber() {return shirtNumber; }
}