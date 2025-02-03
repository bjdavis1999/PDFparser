import java.util.ArrayList;
/*
    Transaction object Stores All stock purchase or sales in a set of parallel ArrayLists
*/
public class Transaction {

    // records if transaction is the selling or a purchase of stocks
    // true is purchase false is selling
    char saleType;
    // StockTicker array list of Strings that identifies the stock
    String StockTicker;
    // Date is arraylist of strings formated mm/dd/year
    String Date ;
    // PriceRange is an ArrayList of int arrays of size 2 that store the range of money the transaction was for
    // reporting guidelines does not require a specific number
    int[] PriceRange = new int[2];

}
