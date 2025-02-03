import java.util.ArrayList;
/*
    congressperson object is used to store an individual congressperson and there transactions
*/
public class congressperson {

    // last name of congressperson
    String name;

    // used for gathering pdfs with transaction info
    //year represents the year a filling was made
    public ArrayList<String> year = new ArrayList<>();
    // DocId represents the fillingID that identifies the PDF with the transaction info
    public ArrayList<String> docID = new ArrayList<>();

    //object used to store information about a congressperson stock transactions
    public  ArrayList<Transaction> transactions = new ArrayList<>();


}
