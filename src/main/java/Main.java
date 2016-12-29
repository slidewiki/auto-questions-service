import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;

public class Main {

    public static void main(String[] args) {
//        try {
////            new NLPStuff().runNLP();
//            new NLPStuff().runFrequencyNLP();
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
        DBPediaSpotlightClient dbPediaSpotlightClient = new DBPediaSpotlightClient();
        dbPediaSpotlightClient.init();
        dbPediaSpotlightClient.getResponse("President Obama called Wednesday on Congress to extend a tax break\\n  for students included in last year's economic stimulus package, arguing\\n  that the policy provides more generous assistance.");
//        new JenaStuff().runJena();
//        new NIF().nifRun();
    }
}
