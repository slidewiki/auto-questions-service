import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;

public class Main {

    public static void main(String[] args) {
        try {
            new NLPStuff().runNLP();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
//        new JenaStuff().runJena();
//        new NIF().nifRun();
    }
}
