import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
/*

    this object is used to get data from files and sort through strings to get necessary info like transactions and filling ids

*/
public class DataParser {


    // adds all transactions made by any congress person 2 the congress data
    // made just in case and to make sure data parsing works with all documents
    // plan to only use the individual add when it's needed in final program

    public void  getAllCongressTransactions (ArrayList<congressperson> congress) throws IOException {

        //starts at beginning of congress
        int Index =0;
        // loops through all members of congress
        for (congressperson c:congress){
            // adds all transactions for current member to transactions list
            getCongresspeopleTransactions(Index,congress);
            Index ++;
        }

    }


    // gets the transactions for any congressperson by using the index they are stores in the congress object used to store all
    // congresspeople stores data in the transactions arraylist for given congressperson
    public void getCongresspeopleTransactions(int Index, ArrayList<congressperson> congress) throws IOException {


        // gets the name of the congressperson being worked on
        String Name = congress.get(Index).name;
        // gets the directory there pdfs are stored in
        File dir = new File("PDF/"+Name);
        // creates a list of file names for all pdfs in the directory
        File[] files = dir.listFiles();

        // try catch block catch null pointer exception if one happens
        try {

            int count =0;
            // loops through all pdf files in lastname directory
            for (File file : files) {

                // returns the string text of the pdf
                String pdfText = ReturnPDFText(file.toString());
                // used to get transactions from pdf string and add them to congresspersons transaction list
                System.out.println(files[count]);
                count++;
                getStockDataFromPDFText(pdfText, congress, Index);


            }

        }catch (NullPointerException n){
            System.out.println("nullPointerError while parsing pdf");
        }



    }

    // gets the transaction data from pdfs note a pdf can have more than one transaction likely many
    public void getStockDataFromPDFText(String text, ArrayList<congressperson> congress, int NameIndex){

        // used to store a string with an entire transaction stored on it
        String currentLine;
        // scanner used to load text and organize it
        Scanner scan = new Scanner(text);
        // stores the scanner lines to be added to current line or skipped if not needed
       // String priorLine = scan.nextLine();
        String line = scan.nextLine();
        // loops through all lines in a document
        while ((scan.hasNext())){
            // checks for a char that begins all transactions
            if (line.contains("sP")||line.contains("SP")||line.contains("(")) {
                // adds that line to the current transaction line
                currentLine = getNextLine(scan,line);
                if (!currentLine.toUpperCase().contains(": NEW")){

                    currentLine = "did not contain new filling tag";
                }

                // makes sure the transaction is a stock trade as other transactions can be in these reports


                    // gets needed info from current line and stores it to the congresspersons transactions
                    parseDataFromLine(currentLine,congress,NameIndex);


            }


                // prevents an error if code gets to last line in scanner before starting ending loop
            if (scan.hasNext()){
                //priorLine = line;
                line = scan.nextLine();
            }

        }



    }
    // checks if a line contains a trade type indicator returns true if it has one that indicates a stock or if it is missing one
    // because of how some of the pdfs are set up we will need to verify if tickers are a valid stock when getting price info
    // however this function lets me reduce the amount of invalid transactions needed to be sorted through when pricing
    private boolean ContainsStockTradeIndicator(String currentLine){

        // checks for ST the trade type indicator for stocks always contained within []
        if (currentLine.contains("[ST]")){
            return true;
        }
        // returns true if no indicator is present sense this may still be a stock trade
        return !currentLine.contains("[");
    }

    // used to get a price from the price range

    private int getPrice(String currentLine,int index){

        // makes sure price is a valid price there are cases were .01 is reported or an improper range is used for certain transaction
        // these cases are set to negative one and will be filtered out of the results later
        try {
            // stores minimum of price range when null is inputted goes until the character at index is not an int value 0-9
            return Integer.parseInt(getString(currentLine, null, index));
        } catch (NumberFormatException n){
            // invalid result indicator
            return  -1;
        }
    }

    // get transaction data from a string and store it to congress persons transactions
    public  void parseDataFromLine(String currentLine, ArrayList<congressperson> congress, int NameIndex){

        // creates a temporary transaction to be added to congress person at end
        Transaction Temp = new Transaction();
        // stores the price range in an array
        int[] purchaseRange = new int[2];
        // gets returns true if line has a st indicator or no indicator
        boolean StockIndicator = ContainsStockTradeIndicator(currentLine);
        // used to move around line char for parsing
        int index =0;
        // first check to make sure line is valid needs a ticker contained within (XXXX)
        // and an indicator [ST] for stock trade or no indicator to pass
        if (currentLine.contains("(")&&StockIndicator){
            // moves to the start of the ticker
            index = moveIndexToChar(currentLine,'(',0);
            // moves 1 more to be on actual ticker text
            index++;
            // stores the ticker name in a temp variable so we can determine if its valid
            String TempTicker = (getString(currentLine,')',index));
            // checks if ticker is longer then stock minimum
            if (TempTicker.length() <= 5) {
                // try to parse data from rest of line if fails skips line as it is not format correctly
                // and probably does not contain a trade
                // via debug there is 617 lines that produce a skip most of these are intended because formating
                // sometimes produces a valid line that does not contain a trade however if time allows going back over this to get the edge cass may
                // be beneficial
                try {
                    // stores the ticker in the temp transaction object
                    Temp.StockTicker = TempTicker;

                    // moves index to end of ticker
                    index = moveIndexToChar(currentLine, ')', index);
                    // moves index into the date
                    index = moveIndexToChar(currentLine, '/', index);
                    // the sale type is always 4 or 10 characters away from the date this depends on if it's a (partial) type
                    // not storing this info as im not sure if it would be usefull can come back and add it if needed
                    // sale type can be E S or P stands for Exchange Sale or Purchase
                    // not sure what to do with exchange types yet but there stored so we can pick how to process them later
                    Temp.saleType = getSaleType(currentLine, index);
                    // moves the index back to the start of the date
                    index -= 2;
                    // stores the date in the Temp Transaction
                    Temp.Date = (getString(currentLine, ' ', index));
                    // moves Index to start of the minmum price range
                    index = moveIndexToChar(currentLine, '$', index);
                    // moves index forward 1 space into the actual price
                    index++;
                    // stores minimum of price is -1 if invalid price
                    purchaseRange[0] = getPrice(currentLine,index);
                    // moves to next dollar value
                    index = moveIndexToChar(currentLine, '$', index);
                    // moves index forward 1 space
                    index++;
                    // stores maximum of price is -1 if invalid price
                    purchaseRange[1] = getPrice(currentLine,index);
                    // ads price range to temp transaction
                    Temp.PriceRange = (purchaseRange);
                    // stores transaction in congressperson transaction list
                    congress.get(NameIndex).transactions.add(Temp);
                } catch (StringIndexOutOfBoundsException e){
                    // catch all for lines that create an error because they don't contain the correct formating

                }
            }

        }

    }



    // returns the sales type purchase is true selling is false
    public char getSaleType(String currentLine,int index){
        // moves index to the correct space
        index -= 4;
        // if transaction is a partial one moves index further to get to the actual space
        if (currentLine.charAt(index) == ')'){
            index -= 10;
        }
        // returns the transaction type P S or E
        // purchase sale or exchange
        return  currentLine.charAt(index);
    }


    // checks if a line is a stock trade and not another asset or invalid line
    public Boolean isStockTrade(String currentLine){


        // stores index for were trade type ends
        int Index = 0;
        // moves to the start of the trade line characters
        if (currentLine.contains("[")){
            //moves to end of transactions
            while (currentLine.charAt(Index) != '['){
                Index++;
            }
            Index++;

            // checks if transaction matches stock trade id ST
            return currentLine.startsWith("ST", Index);

        }
        // returns false if transaction is note a stock trade
        return false;

    }

    // gets the next unprocessed transaction string
    // it's all added to one line because variations in the documents made it easier to parse on large string rather than several split up ones
    public  String getNextLine(Scanner scan,String line){

            // used to combine all lines were the transaction info is stores
            StringBuilder combinedLine = new StringBuilder();
            // appends first line that contained a valid transaction identifier normally () or SP
            combinedLine.append(line);
            // used to make sure loop does not try and create a mega line
            int count = 0;
            // loops through document until out of lines, or it completes a transaction line
            while (!line.contains("s: New")&&scan.hasNext()){
                // Edge case 2 common terms found in invalid lines that the parser logic try to run
                // believe I have fixed the need for this case however commited out so I can re add if removal proves to be a problem
                /*
                if(line.toUpperCase().contains("OWNER: JT")||line.toUpperCase().contains("OWNER: SP")){
                    combinedLine.append(line);
                    return "bad line";

                }
                */
                // edge case for invalid lines some congresspeople like giving descriptions about the transaction that sometimes includes
                // the ticker name resulting in an invalid duplicate of the transaction or an error when it attempts to parse data
                // from the resulting line
                if (line.toUpperCase().contains("DESCRIPTION:")||line.toUpperCase().contains("COMMENTS:")||line.toUpperCase().contains("FILING ID")){
                    // just a line that I know the parse won't attempt to get data from
                    return  "bad Line description";
                }
                // used to find end of transaction most end with new
                // deleted used for some of the weird documents may filter out those transactions
                // haven't decided yet
                if (line.contains(": Deleted")||line.contains(": New")){
                    combinedLine.append(line);
                    return combinedLine.toString();
                }
                // advances the loop to the next line and adds line to the combined string
                line = scan.nextLine();
                combinedLine.append(line);
                count++;
            }

            // returns the entire transaction line
            return combinedLine.toString();

    }


    // returns the plain text of the pdf for some reason this has some variability that I believe has been accounted for in the text parser
    public String ReturnPDFText(String Path) throws IOException {

        // loads path to pdf
        File pdfFile = new File(Path);

        // opens pdf
        PDDocument document = PDDocument.load(pdfFile);

        // strips text from pdf
        PDFTextStripper pdfStripper = new PDFTextStripper();
        // stores pdf as a string
        String text = pdfStripper.getText(document);

        // closes the pdf document for resource management
        document.close();

        // returns the pdf string
        return text;

    }

    // extracts the name of the pdf files from the unzipped xml files for all congresspeople Stores it as a DocID and stores the ear for later retrieval from website
    void extractPdfFileName(String fileName, ArrayList<congressperson> congress) throws FileNotFoundException {


        // stores the path to the xml document that has all fillings listed for a year
        File file = new File("Zips/unzipped/"+ fileName);
        // scanner used to go through document
        Scanner scan = new Scanner(file);
        //loops through file
        while (scan.hasNext()){

            // stores next line in file to string
            String line = scan.nextLine();
            // checks for the last name tag in the line if found starts creating or updating a congress person
            if (line.contains("Last")){

                // first makes a temp congress member
                congressperson Temp = new congressperson();

                // gets the last name stores on current line
                Temp.name = getString(line,'<',10);


                // moves scanner lines to the next line needed
                line = moveXLines(scan,2);

                // if the filling is the correct type continues process
                if (line.charAt(16) == 'P'){

                    // moves down a line
                    line = moveXLines(scan,1);
                    // gets the year of the filling
                    Temp.year.add(getString(line,'<',10));
                    // moves down another line
                    line   = moveXLines(scan,1);
                    // gets the document id of the filling
                    Temp.docID.add(getString(line,'<',11));
                    // updates the congress object
                    updateCongress(congress,Temp);


                }

            }
        }

    }

    // advances a counter to the correct index of a string takes a starting index and a char to find
    private int moveIndexToChar(String line,char character,int Index){

        // moves index tell it finds char value
        while(line.charAt(Index) != character){

            Index++;
        }
        // returns index
        return Index;
    }

    // moves a scanner object x number of lines in order to advance to required info
    private String moveXLines(Scanner scan,int index){

        // moves line x number of times
        for (int i = 0;i < index;i++){

            scan.nextLine();

        }
        // returns line that is after loop
        return scan.nextLine();
    }

    // gets all the char in a string from a given start index tell it finds an ending char
    // can take a null value to stop when char are no longer an int value will skip , to get a parsable value
    private String getString(String line,Character character,int Index){



        // used to build the string being returned
        StringBuilder newString = new StringBuilder();

        // checks if the char parameter is null
        if (character == null){

            // loops tell it finds a non number skips comas
            while ((isCharInt(line.charAt(Index))||line.charAt(Index) == ',')&&Index != line.length()-1){

                // skips coma
               if (line.charAt(Index) != ','){
                   //append number to string builder
                   newString.append(line.charAt(Index));
               }

               // advance index
                Index++;
            }
        // used to get a string of a in a range based of a char position and a starting index
        } else {

            // checks if char at index is not goal
            while (line.charAt(Index) != character){
                // skips commas
                if (line.charAt(Index) != ','){
                    // appends value to string builder
                    newString.append(line.charAt(Index));
                }
                // advance index
                Index++;
            }

        }

        //return a generated string
        return  newString.toString();
    }

    // checks if a char is an int value 0-9
    private boolean isCharInt(char character){
        return character >= '0' && character <= '9';

    }

    // checks if a name is already part of the congress object if it is returns the index of that name
    // returns -1 if name is not found
    private int isNameNew(ArrayList<congressperson> congress, String name){

        // starting index
        int Index = 0;
        // loops through all elements in congress
        for (congressperson c: congress){
            // if name is found returns current index
            if (c.name.equals(name))
            {
                return  Index;

            } else {
                // otherwise advance index
                Index ++;

            }

        }
        // if name is not found return -1
        return -1;

    }

    // updates congress with pdf file information docID and filling year
    private void updateCongress(ArrayList<congressperson> congress, congressperson Temp){

        // gets index of last name if exists in object
        int Index = isNameNew(congress,Temp.name);

        // if name does not exist creates new congress entry
        if (Index == -1){

            congress.add(Temp);

        } else {

            // if it does add info to the filling list in congressperson
            congress.get(Index).year.add(Temp.year.getFirst());
            congress.get(Index).docID.add(Temp.docID.getFirst());

        }


    }

}

