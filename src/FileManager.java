import java.io.*;
import java.net.URISyntaxException;
import java.time.Year;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


/*
       file manager handles organization of any files and abstracts download and parser objects

*/
public class FileManager {

    ArrayList<congressperson> congress = new ArrayList<>();
    Downloader downloader = new Downloader();
    DataParser dataParser = new DataParser();


// constructor does initial downloads only happen if file is not already present
    FileManager() throws IOException, URISyntaxException {

        // runs all the download commands for the pdf and zip files
        UpdateAllFiles();
        // returns a congressperson index from the congress array using their name
        //int Index = IndexOfCongressperson("Allen");
        // fills out the transaction array at the index of a congressperson
        //int Index = IndexOfCongressperson("Blumenauer");

        //dataParser.getCongresspeopleTransactions(Index,congress);
        //System.out.println(dataParser.ReturnPDFText("PDF/Lee/20016887.pdf"));
        //String text = dataParser.ReturnPDFText("PDF/Lee/20016887.pdf");

        int Index = IndexOfCongressperson("Lee");

        //dataParser.getCongresspeopleTransactions(Index,congress);

        //dataParser.getStockDataFromPDFText(text,congress,Index);

        //System.out.println(text);
        dataParser.getAllCongressTransactions(congress);
        System.out.println("done");

    }

    // returns the index position of a congressperson with a given lastname returns -1 if none is found
    public int IndexOfCongressperson(String Name){

        // initial index
        int index =0;
        // loops through all congressperson returns index if it finds matching name
        for (congressperson c: congress){
            if (c.name.equals(Name)){
                return index;
            }
            index++;
        }
        // returns -1 if name not found
        return -1;
    }

    // used to abstract download All zips function from downloader
    public void downloadAllZips() throws IOException, URISyntaxException {
        downloader.downloadAllZips();
    }
    // used to abstract download All PDF function from downloader
    public void downloadAllPDF() throws IOException, URISyntaxException {
        downloader.downloadAllPDF(congress);
    }

    // unzips all the download zip files with the name correct name
    public  void unzipAll() throws IOException {

        // there is one zip file for each year so this code gets the current year and the number of times
        // the code need to unzip a file
        int currentYear = Year.now().getValue();
        int NumberOfFiles = currentYear - 2014;

        // loops through all zip files and runs the unzip function on all of them
        // starts at 14 so names match the correct year
        for (int i = 14; i <= NumberOfFiles+14;i++){
            unZip("Zips/20"+i+"FD.zip");
        }

    }

    // unzips a single file and puts the contents in a shared unzipped directory
    // takes a path object representing the location of the file to unzip
    private void unZip (String Path) throws IOException {

        // creates the Destination directory to store unzipped files
        File DestinationDir = new File("Zips/unzipped");


        // used to store data from unzipped file before writing it to the directory
        // // makes things more efficient for large files
        byte[] buffer = new byte[1024];

        //used to reads the data from the zip file
        ZipInputStream zis = new ZipInputStream(new FileInputStream(Path));

        // used to read and write zip files
        ZipEntry zipEntry = zis.getNextEntry();


        // runs until no files are left in the zipped file
        while (zipEntry != null){
            // stores a directory for the files
            File newFile = new File(DestinationDir,zipEntry.getName());
            // confirms directory exists and tries to make it if not
            if (zipEntry.isDirectory()){
                if (!newFile.isDirectory()  && !newFile.mkdirs()){
                    // error message if code fails to make directory
                    throw  new IOException("Failed to create directory 1");
                }
            } else {
                // used to try and make second directory if failed throws error message
                File parent = newFile.getParentFile();
                if (!parent.isDirectory() && !parent.mkdirs()){
                    throw  new IOException("Failed to create directory 2");
                }

                // file output stream used to write the files to the new directory
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                //loops until all data is read from zipped file
                while ((len = zis.read(buffer)) > 0){
                    // writes the bytes from the buffer to the output file
                    fos.write(buffer,0,len);
                }
                // used to close file output stream
                fos.close();
            }
            // gets next file to unzip from the file if none left will be null
            zipEntry = zis.getNextEntry();

        }

        // resource management just closing unused Things
        zis.closeEntry();
        zis.close();
    }


    // Extracts the Names and year of all psd files so we can get stock data from them
    public void extractAllPdfFileNames() throws FileNotFoundException {

        //gets the current year and subtracts the farthest year we have data for
        // needed to know how many files to go through
        int currentYear = Year.now().getValue();
        int NumberOfFiles = currentYear - 2014;

        // loops through all pdf files
        for (int i =14;i < NumberOfFiles+14;i++){

            //uses the data parser to extract required info and store it in the relevant Congressperson
            dataParser.extractPdfFileName("20"+i+"FD.xml",congress);

        }

    }


    // updates all downloads to get any missing files and extracts information
    // names are self-explanatory
    public void UpdateAllFiles() throws IOException, URISyntaxException {



        downloadAllZips();

        unzipAll();

        extractAllPdfFileNames();

        downloadAllPDF();



    }



}
